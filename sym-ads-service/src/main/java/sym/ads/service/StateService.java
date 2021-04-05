package sym.ads.service;

import io.nem.symbol.sdk.model.blockchain.BlockInfo;
import io.nem.symbol.sdk.model.mosaic.MosaicId;
import io.nem.symbol.sdk.model.mosaic.MosaicInfo;
import io.nem.symbol.sdk.model.transaction.Transaction;
import sym.ads.core.BaseClass;
import sym.ads.core.Database;
import sym.ads.core.SymConnector;
import sym.ads.core.ShutdownHookHandler;
import sym.ads.core.model.Ad;
import sym.ads.core.model.EntityDto;
import sym.ads.core.model.Site;
import sym.ads.core.rpc.PaymentService;
import sym.ads.service.rpc.PaymentServiceImpl;
import one.nio.rpc.RpcServer;
import one.nio.server.ServerConfig;

import java.math.BigInteger;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import static sym.ads.core.Constants.*;

/**
 * Created by vbondarenko on 08.07.2020.
 */

public final class StateService extends BaseClass implements Consumer<BlockInfo> {

    private static final StateService INSTANCE = new StateService();

    private final SymConnector symConnector = SymConnector.getInstance();

    private final ConcurrentHashMap<Integer, String> categories = new ConcurrentHashMap<>();
    private final ConcurrentSkipListMap<String, Site> sites = new ConcurrentSkipListMap<>();
    private final ConcurrentSkipListMap<String, Ad> ads = new ConcurrentSkipListMap<>();
    private final HashSet<String> removedIds = new HashSet<>();

    private final ConcurrentHashMap<String, Object> configs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> siteHosts = new ConcurrentHashMap<>();

/*
    private final SurfDao surfDao;
    private final ClickDao clickDao;
*/

    private final String serverId;

    private ConfigHandler configHandler;
//    private PaymentHandler paymentHandler;

    private String lastTxId;
    //    private Lock mosaicLock;
    private MosaicId mosaicId;
    private int divisibility;
    private long amountMultiply;
    private String serviceUrl;

    private StateService() {
        try {
            Preferences preferences = Preferences.userNodeForPackage(ServiceServer.class);
            String v;
            if ((v = preferences.get(PREFERENCE_SERVER_ID, null)) == null) {
                serverId = UUID.randomUUID().toString();

                preferences.put(PREFERENCE_SERVER_ID, serverId);
            } else {
                serverId = v;
            }

/*
            clickDao = ClickDao.getInstance();
            surfDao = SurfDao.getInstance();
            Database.getInstance().init(clickDao, surfDao);
*/

            ServerConfig config = ServerConfig.from("0.0.0.0:" + 7777);
            RpcServer<PaymentService> server = new RpcServer<>(config, new PaymentServiceImpl(Collections.unmodifiableMap(configs)));

            log.info("Start RPC-server, {}:{}", config.acceptors[0].address, config.acceptors[0].port);

            server.start();

            ShutdownHookHandler.getInstance().addShutdownHook(() -> {
                log.info("Terminate RPC-server");

                server.stop();

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
            configHandler = new ConfigHandler();
//            paymentHandler = new PaymentHandler(Collections.unmodifiableMap(configs));

            blockInfoConsumer = blockInfoConsumer.andThen(configHandler);

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

    public ConfigHandler getConfigHandler() {
        return configHandler;
    }

/*
    public PaymentHandler getPaymentHandler() {
        return paymentHandler;
    }
*/

    /*
    public SurfDao getSurfDao() {
        return surfDao;
    }

    public ClickDao getClickDao() {
        return clickDao;
    }
*/

    public ConcurrentHashMap<Integer, String> getCategories() {
        return categories;
    }

    public ConcurrentSkipListMap<String, Site> getSites() {
        return sites;
    }

    public ConcurrentSkipListMap<String, Ad> getAds() {
        return ads;
    }

    public ConcurrentHashMap<String, Object> getConfigs() {
        return configs;
    }

    public ConcurrentHashMap<String, String> getSiteHosts() {
        return siteHosts;
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
        symConnector.handleTransactionMessage(transaction, this::handle);
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
                } else {
                    if (!removedIds.contains(id)) {
                        ads.putIfAbsent(id, ad);
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
