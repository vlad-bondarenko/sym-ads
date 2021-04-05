package sym.ads.web.service;

import io.nem.symbol.sdk.model.blockchain.BlockInfo;
import io.nem.symbol.sdk.model.mosaic.MosaicId;
import io.nem.symbol.sdk.model.mosaic.MosaicInfo;
import io.nem.symbol.sdk.model.transaction.Transaction;
import io.nem.symbol.sdk.model.transaction.TransactionType;
import sym.ads.core.*;
import sym.ads.core.model.Ad;
import sym.ads.core.model.EntityDto;
import sym.ads.core.model.Site;
import sym.ads.web.SymAdsServer;
import sym.ads.web.dao.ClickDao;
import sym.ads.web.dao.PayoutDao;
import sym.ads.web.dao.SurfDao;

import java.math.BigInteger;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import static sym.ads.core.Constants.*;

/**
 * Created by vbondarenko on 13.06.2020.
 */

public final class StateService extends BaseClass implements Consumer<BlockInfo> {

    public static final AtomicBoolean IS_RUNNING = new AtomicBoolean(true);

    private static final StateService INSTANCE = new StateService();

    private final SymConnector symConnector = SymConnector.getInstance();

    private final ConcurrentHashMap<Integer, String> categories = new ConcurrentHashMap<>();
    private final ConcurrentSkipListMap<String, Site> sites = new ConcurrentSkipListMap<>();
    private final ConcurrentSkipListMap<String, Ad> ads = new ConcurrentSkipListMap<>();
    private final HashSet<String> removedIds = new HashSet<>();
    private final ConcurrentHashMap<Integer, ConcurrentMap<String, Ad>> auction = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Object> configs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> siteHosts = new ConcurrentHashMap<>();

    private final SurfDao surfDao;
    private final ClickDao clickDao;
    private final PayoutDao payoutDao;

    private final boolean isWebmasterEnabled;
    private final boolean isAdvertiserEnabled;

    private final String serverId;

    private WebmasterService webmasterService;
    private AdvertiserService advertiserService;

    private String lastTxId;
    //    private Lock mosaicLock;
    private MosaicId mosaicId;
    private int divisibility = 6;
    private long amountMultiply = 1_000_000;
    private String serviceUrl = "http://localhost:7777";

    private StateService() {
        try {
            Preferences preferences = Preferences.userNodeForPackage(SymAdsServer.class);
            String v;
            if ((v = preferences.get(PREFERENCE_SERVER_ID, null)) == null) {
                serverId = UUID.randomUUID().toString();

                preferences.put(PREFERENCE_SERVER_ID, serverId);
            } else {
                serverId = v;
            }

            Role role = Settings.getInstance().role();
            if (role == Role.dual) {
                isWebmasterEnabled = true;
                isAdvertiserEnabled = true;

                clickDao = ClickDao.getInstance();
                surfDao = SurfDao.getInstance();
                payoutDao = PayoutDao.getInstance();
                Database.getInstance().init(clickDao, surfDao, payoutDao);
            } else if (role == Role.webmaster) {
                isWebmasterEnabled = true;
                isAdvertiserEnabled = false;

                clickDao = ClickDao.getInstance();
                surfDao = null;
                payoutDao = null;
                Database.getInstance().init(clickDao);
            } else if (role == Role.advertiser) {
                isWebmasterEnabled = false;
                isAdvertiserEnabled = true;

                clickDao = null;
                surfDao = SurfDao.getInstance();
                payoutDao = PayoutDao.getInstance();
                Database.getInstance().init(surfDao, payoutDao);
            } else {
                throw new UnsupportedOperationException("Unsupported '" + role + "' role");
            }

            log.info("Role use '{}'", role);

            ShutdownHookHandler.getInstance().addShutdownHook(() -> {
                log.info("Terminate Database");

                Database.getInstance().terminate();
            });
        } catch (Exception e) {
            log.error(e.toString(), e);

            throw new RuntimeException(e);
        }
    }

    public void init() {
           /*
        TreeMap<AccountMetaDataPair, Ad> map = new TreeMap<>((o1, o2) -> {
            if (o2.balance > o1.balance) {
                return 1;
            }

            if (o2.balance < o1.balance) {
                return -1;
            }

            return Long.compare(o2.mosaic, o1.mosaic);
        });
*/
        try {
            symConnector.setTransactionConsumer(this::handle);

            //bootstrap
            accept(null);

            Consumer<BlockInfo> blockInfoConsumer = this;
            if (isWebmasterEnabled) {
                webmasterService = new WebmasterService();
                blockInfoConsumer = blockInfoConsumer.andThen(webmasterService);
            }
            if (isAdvertiserEnabled) {
                advertiserService = new AdvertiserService();
                blockInfoConsumer = blockInfoConsumer.andThen(advertiserService);
            }
            symConnector.setNewBlockConsumer(blockInfoConsumer);
//            nemConnector.setNewBlockConsumer(this::handleNewBlock);
        } catch (Exception e) {
            log.error(e.toString(), e);

            throw new RuntimeException(e);
        }
    }

    public static StateService getInstance() {
        return INSTANCE;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public String getServerId() {
        return serverId;
    }

    public boolean isWebmasterEnabled() {
        return isWebmasterEnabled;
    }

    public boolean isAdvertiserEnabled() {
        return isAdvertiserEnabled;
    }

    public WebmasterService getWebmasterHandler() {
        return webmasterService;
    }

    public AdvertiserService getAdvertiserService() {
        return advertiserService;
    }

    public SurfDao getSurfDao() {
        return surfDao;
    }

    public PayoutDao getPayoutDao() {
        return payoutDao;
    }

    public ClickDao getClickDao() {
        return clickDao;
    }

    public ConcurrentHashMap<Integer, String> getCategories() {
        return categories;
    }

    public ConcurrentSkipListMap<String, Site> getSites() {
        return sites;
    }

    public ConcurrentSkipListMap<String, Ad> getAds() {
        return ads;
    }

    public ConcurrentHashMap<Integer, ConcurrentMap<String, Ad>> getAuction() {
        return auction;
    }

    public ConcurrentHashMap<String, Object> getConfigs() {
        return configs;
    }

    public ConcurrentHashMap<String, String> getSiteHosts() {
        return siteHosts;
    }

    public MosaicId getMosaicId() {
        return mosaicId;
    }

    public int getDivisibility() {
        return divisibility;
    }

    public long getAmountMultiply() {
        return amountMultiply;
    }

    @Override
    public void accept(BlockInfo blockInfo) {
        log.debug("New block");

        handleTransactions();
    }

    private void handleTransactions() {
        try {
            lastTxId = symConnector.load(symConnector.rootPublicAccount, lastTxId, 10);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    private void handle(Transaction transaction) {
        if (transaction.isConfirmed() && transaction.getType() == TransactionType.TRANSFER) {
            symConnector.handleTransactionMessage(transaction, this::handle);
        }
    }

    private void handle(Transaction transaction, EntityDto entityDto) {
        symConnector.handleTransactionMessageType(
                transaction,
                entityDto,
                this::handleAd,
                this::handleSite,
                this::handleConfig
        );
    }

    private void handleAd(String id, Ad ad) {
        try {
            if (ad == null) {
                removeAd(id);
            } else {
                if (removedIds.isEmpty()) {
                    ads.putIfAbsent(id, ad);

                    if (isWebmasterEnabled) {
                        auction.computeIfAbsent(ad.getCategory(), category -> new ConcurrentSkipListMap<>()).
                                computeIfAbsent(id, key -> ad);
                    }
                } else {
                    if (!removedIds.contains(id)) {
                        ads.putIfAbsent(id, ad);

                        if (isWebmasterEnabled) {
                            auction.computeIfAbsent(ad.getCategory(), category -> new ConcurrentSkipListMap<>()).
                                    computeIfAbsent(id, key -> ad);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    private void removeAd(String id) {
        try {
            if (removedIds.isEmpty()) {
                ads.remove(id);

                if (isWebmasterEnabled) {
                    for (Map<String, Ad> adMap : auction.values()) {
                        adMap.keySet().removeIf(id::equals);
                    }
                }
            } else {
                removedIds.add(id);
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    private void handleSite(String id, Site site) {
        try {
            if (site == null) {
                removeSite(id);
            } else {
                if (removedIds.isEmpty()) {
                    sites.putIfAbsent(id, site);
                    try {
                        siteHosts.putIfAbsent(new URL(site.getUrl()).getHost(), id);
                    } catch (Exception e) {
                        log.error(e.toString(), e);
                    }
                } else {
                    if (!removedIds.contains(id)) {
                        sites.putIfAbsent(id, site);
                        try {
                            siteHosts.putIfAbsent(new URL(site.getUrl()).getHost(), id);
                        } catch (Exception e) {
                            log.error(e.toString(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    private void removeSite(String id) {
        try {
            if (removedIds.isEmpty()) {
                sites.remove(id);
                siteHosts.values().removeIf(id::equals);
            } else {
                removedIds.add(id);
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    private void handleConfig(String id, Map<String, Object> config) {
        symConnector.handleConfig(id, config, categories, configs);

        if (config.containsKey(CONFIG_SERVICE_MOSAIC_ID)) {
            String mosaicId = (String) config.get(CONFIG_SERVICE_MOSAIC_ID);
            if (this.mosaicId == null || !this.mosaicId.getIdAsHex().equals(mosaicId)) {
/*
            if (mosaicLock == null) {
                mosaicLock = new ReentrantLock();
            }
            mosaicLock.lock();

            try {
*/
                this.mosaicId = new MosaicId(mosaicId);

                MosaicInfo mosaicInfo = symConnector.mosaicInfo(this.mosaicId);
                divisibility = mosaicInfo.getDivisibility();
                amountMultiply = BigInteger.TEN.pow(divisibility).longValue();
/*
            } finally {
                mosaicLock.unlock();
            }
*/
            }
        }

        if (config.containsKey(CONFIG_SERVICE_URL)) {
            String serviceUrl = (String) config.get(CONFIG_SERVICE_URL);
            if (this.serviceUrl == null || !this.serviceUrl.equals(serviceUrl)) {
                this.serviceUrl = serviceUrl;
            }
        }
    }

}
