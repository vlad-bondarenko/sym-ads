package sym.ads.web.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.nem.symbol.sdk.model.blockchain.BlockInfo;
import io.nem.symbol.sdk.model.transaction.CosignatureSignedTransaction;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.rpc.RpcClient;
import sym.ads.core.BaseClass;
import sym.ads.core.IDs;
import sym.ads.core.SymConnector;
import sym.ads.core.Utils;
import sym.ads.core.model.*;
import sym.ads.core.rpc.PaymentService;
import sym.ads.core.rpc.model.Payment;
import sym.ads.core.rpc.model.Result;
import sym.ads.web.TemplateEngine;
import sym.ads.web.dao.PayoutDao;
import sym.ads.web.dao.SurfDao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static sym.ads.core.Constants.*;
import static sym.ads.core.Utils.*;

/**
 * Created by vbondarenko on 22.06.2020.
 */

public final class AdvertiserService extends BaseClass implements Consumer<BlockInfo> {

    private final SymConnector symConnector = SymConnector.getInstance();
    private final TemplateEngine templateEngine = TemplateEngine.getInstance();
    private final ConcurrentHashMap<Integer, String> categories = StateService.getInstance().getCategories();
    private final ConcurrentSkipListMap<String, Site> sites = StateService.getInstance().getSites();
    private final ConcurrentNavigableMap<String, Ad> ads = StateService.getInstance().getAds();
    private final ConcurrentHashMap<String, Object> configs = StateService.getInstance().getConfigs();

    private final BiPredicate<String, ByteArrayOutputStream> AD_LIST_BI_PREDICATE;
    private final Function<String, BiPredicate<String, ByteArrayOutputStream>> AD_LIST_FUNCTION;
    private final BiPredicate<String, ByteArrayOutputStream> CATEGORY_OPTION_LIST_BI_PREDICATE;

    private final SurfDao surfDao = StateService.getInstance().getSurfDao();
    private final PayoutDao payoutDao = StateService.getInstance().getPayoutDao();

    private final Cache<String, Boolean> adCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build();

    public AdvertiserService() {
        CATEGORY_OPTION_LIST_BI_PREDICATE = (name, byteArrayOutputStream) -> {
            if (!"list.category".equals(name)) {
                return false;
            }

            HashMap<String, byte[]> model = new HashMap<>(2);
            Integer id;
            String category;
            for (Map.Entry<Integer, String> entry : categories.entrySet()) {
                id = entry.getKey();
                category = entry.getValue();
                model.put("name", toBytesOrDefault(category));
                model.put("value", toBytesOrDefault(String.valueOf(id)));

                try {
                    templateEngine.render(6, byteArrayOutputStream, model);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return true;
        };

        AD_LIST_BI_PREDICATE = (name, byteArrayOutputStream) -> {
            if (!"list.ad".equals(name)) {
                return false;
            }

//                SITE_DAO.forEach((id, site) -> {
            HashMap<String, byte[]> model = new HashMap<>();
            String id;
            Ad ad;
            for (Map.Entry<String, Ad> entry : ads.entrySet()) {
                id = entry.getKey();
                ad = entry.getValue();
                model.put("id", toBytesOrDefault(id));
                model.put("name", toBytesOrDefault(ad.getName()));
                model.put("desc", toBytesOrDefault(ad.getDesc()));
                model.put("url", toBytesOrDefault(ad.getUrl()));
                model.put("category", toBytesOrDefault(categories.get(ad.getCategory())));
                model.put("price", toBytesOrDefault(Utils.toAmount(ad.getPrice(), StateService.getInstance().getDivisibility(), NUMBER_SEPARATOR)));

                model.put("country", toBytesOrDefault(ad.getCountry()));
                model.put("fromHour", toBytesOrDefault(String.valueOf(ad.getFromHour())));
                model.put("toHour", toBytesOrDefault(String.valueOf(ad.getToHour())));
                model.put("fromWeek", toBytesOrDefault(String.valueOf(ad.getFromWeek())));
                model.put("toWeek", toBytesOrDefault(String.valueOf(ad.getToWeek())));
                model.put("bl", toBytesOrDefault(Utils.toAmount(ad.getBl(), SYM_DIVISIBILITY, NUMBER_SEPARATOR)));

                try {
                    templateEngine.render(5, byteArrayOutputStream, model);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return true;
        };

        AD_LIST_FUNCTION = id -> (name, byteArrayOutputStream) -> {
            if (surfDao == null || !"list.click".equals(name)) {
                return false;
            }

            try {
                HashMap<String, byte[]> model = new HashMap<>(4);
                long current = System.currentTimeMillis();
                surfDao.forEach((key, surf) -> {
                    if (id.equals(surf.getId())) {
                        long time = IDs.timestampOf(key);

                        if (current - time > TIME_DIFF) {
                            try {
                                log.debug("{}: History delete", key);

//                                    surfDao.delete(key);
                            } catch (Exception e) {
                                log.error(e.toString(), e);
                            }

//                            return;
                        }

                        model.put("date", toBytesOrDefault(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        model.put("host", toBytesOrDefault(surf.getHost()));
                        model.put("referrer", toBytesOrDefault(surf.getReferrer()));
                        model.put("destinationId", toBytesOrDefault(surf.getDestinationId()));
                        model.put("country", toBytesOrDefault(surf.getCountry()));

                        try {
                            templateEngine.render(12, byteArrayOutputStream, model);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (Exception e) {
                log.error(e.toString(), e);
            }

            return true;
        };
    }

    public Response adminAds() throws Exception {
        Response response = Response.ok(templateEngine.render(
                2,
                new ByteArrayOutputStream(),
                null,
                AD_LIST_BI_PREDICATE
        ).toByteArray());
        response.addHeader(CONTENT_TYPE_HTML);

        return response;
    }

    public Response adminAd(String deleteId, String historyId) throws IOException {
        if (isNotBlank(deleteId)) {
            try {
                log.info("{}: Delete ad", deleteId);

                symConnector.save(new EntityAd(deleteId));
            } catch (Exception e) {
                log.error(e.toString(), e);
            }

            return Response.redirect("/admin/ads.html");
        }

        //todo impl
        if (isNotBlank(historyId)) {
            try {
                log.info("{}: History ad", historyId);
            } catch (Exception e) {
                log.error(e.toString(), e);
            }

            return Response.redirect("/admin/ad/history.html");
        }

        Response response = Response.ok(templateEngine.render(
                3,
                new ByteArrayOutputStream(),
                null,
                CATEGORY_OPTION_LIST_BI_PREDICATE).toByteArray());
        response.addHeader(CONTENT_TYPE_HTML);

        return response;
    }

    public Response adminAdForm(String body) throws Exception {
        Map<String, List<String>> params = Utils.parseParameters(body);

        String id = firstParam(params, "id");
        String name = firstParam(params, "name");
        String url = firstParam(params, "url");
        String desc = firstParam(params, "desc");
        String country = firstParam(params, "country");

        int category = parseInt(firstParam(params, "category"));
        long price = (long) (parseDouble(firstParam(params, "price")) * StateService.getInstance().getAmountMultiply());
        int fromHour = parseInt(firstParam(params, "fromHour"));
        int toHour = parseInt(firstParam(params, "toHour"));
        int fromWeek = parseInt(firstParam(params, "fromWeek"));
        int toWeek = parseInt(firstParam(params, "toWeek"));
        long bl = parseLong(firstParam(params, "bl")) * SYM_AMOUNT_MULTIPLY;

        if (isNotBlank(name)
                && isNotBlank(url)
                && isNotBlank(desc)
                && category > 0
                && price > 0
                && isNotBlank(country)) {
            Ad ad = id == null ? null : ads.get(id);
            if (ad == null
                    || (!ad.getUrl().equals(url)
                    || !ad.getDesc().equals(desc)
                    || category != ad.getCategory()
                    || price != ad.getPrice()
                    || !ad.getCountry().equals(country)
                    || fromHour != ad.getFromHour()
                    || toHour != ad.getToHour()
                    || fromWeek != ad.getFromWeek()
                    || toWeek != ad.getToWeek()
                    || bl != ad.getBl()
            )) {
                ad = new Ad(url, desc, name, category, price);
                ad.setCountry(country);
                ad.setFromHour(fromHour);
                ad.setToHour(toHour);
                ad.setFromWeek(fromWeek);
                ad.setToWeek(toWeek);
                ad.setBl(bl);
            } else {
                ad = null;
            }

            if (ad != null) {
                //                            SITE_DAO.put(name, new SiteDao.Site(name, url, category, linkSize, separator));
                symConnector.save(new EntityAd(ad));
            }
        }

        return Response.redirect("/admin/ads.html");
    }

    public Response adminAdHistory(String id) throws Exception {
        Ad ad = ads.get(id);
        if (ad == null) {
            return Response.ok(EMPTY);
        }

        log.info("{}: History ad", id);

        HashMap<String, byte[]> model = new HashMap<>(11);
        model.put("name", toBytesOrDefault(ad.getName()));
        model.put("desc", toBytesOrDefault(ad.getDesc()));
        model.put("url", toBytesOrDefault(ad.getUrl()));
        model.put("category", toBytesOrDefault(categories.get(ad.getCategory())));
        model.put("price", toBytesOrDefault(Utils.toAmount(ad.getPrice(), StateService.getInstance().getDivisibility(), NUMBER_SEPARATOR)));
        model.put("country", toBytesOrDefault(ad.getCountry()));
        model.put("fromHour", toBytesOrDefault(String.valueOf(ad.getFromHour())));
        model.put("toHour", toBytesOrDefault(String.valueOf(ad.getToHour())));
        model.put("fromWeek", toBytesOrDefault(String.valueOf(ad.getFromWeek())));
        model.put("toWeek", toBytesOrDefault(String.valueOf(ad.getToWeek())));
        model.put("bl", toBytesOrDefault(Utils.toAmount(ad.getBl(), SYM_DIVISIBILITY, NUMBER_SEPARATOR)));
        return Response.ok(templateEngine.render(
                11,
                new ByteArrayOutputStream(),
                model,
                AD_LIST_FUNCTION.apply(id)).toByteArray()
        );
    }

    public Response ad(String id, String referer, String host) throws Exception {
        String query;
        if (isBlank(id)
                || isBlank(referer)
                || isBlank(host)
                || !ads.containsKey(id)
                || isBlank(query = new URL(referer).getQuery())) {
            return Response.ok(EMPTY);
        }

        String[] values = query.split("=", 2);
        if (values.length != 2 || !IDs.validateTime(IDs.timestampOf(Utils.parseLong(values[1])))) {
            return Response.ok(EMPTY);
        }

        try {
            //           String ip = InetAddress.getByName(headers.getFirst("Host").split(":")[0]).getHostAddress();
            adCache.get(id, s -> {
                try {
                    surfDao.put(IDs.newId(), new Surf(id, referer, host, "country", values[1]));//todo country
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }

                return Boolean.TRUE;
            });
        } catch (Exception e) {
            log.error(e.toString(), e);
        }

        return Response.ok(EMPTY);
    }

    @Override
    public void accept(BlockInfo blockInfo) {
        log.debug("New block");

        payment();
    }

    @SuppressWarnings("unchecked")
    public Response payment() {
        if (StateService.getInstance().getServiceUrl() == null) {
            return new Response(Response.INTERNAL_ERROR);
        }

        try (RpcClient client = new RpcClient(new ConnectionString(StateService.getInstance().getServiceUrl()))) {
            PaymentService paymentService = (PaymentService) Proxy.newProxyInstance(PaymentService.class.getClassLoader(), new Class[]{PaymentService.class}, client);

            long current = System.currentTimeMillis();

            surfDao.forEach((key, surf) -> {
                final Ad ad = ads.get(surf.getId());

                if (ad == null) {
                    return;
                }

                long time = IDs.timestampOf(key);

                if (current - time > TIME_DIFF) {
                    try {
                        log.debug("{}: Payment", key);

                        final Payout payout = payoutDao.get(surf.getId());
                        if (payout == null) {
//                            Result<String> paymentResult = paymentService.payment(GSON.toJson(new Payment(StateService.getInstance().getServerId(), surf.getId(), symConnector.rootPublicAccount.getAddress().plain(), BigInteger.valueOf(ad.getPrice()), symConnector.account.getPublicKey())).getBytes(StandardCharsets.UTF_8));
                            Result<String> paymentResult = GSON.fromJson(new String(paymentService.payment(GSON.toJson(new Payment(StateService.getInstance().getServerId(), surf.getId(), "TD24RP-F7DZSD-CGW5KA-IN3TUN-7QP5WR-JTYULE-XHA", BigInteger.valueOf(ad.getPrice()), symConnector.account.getPublicKey())).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8), Result.class);

                            log.debug("paymentResult: {}", paymentResult);

                            if (paymentResult.getCode() != 0) {
                                log.error("paymentResult: {}", paymentResult);
                            } else {
                                String hash = paymentResult.getData();

                                CosignatureSignedTransaction cosignatureSignedTransaction = symConnector.account.signCosignatureTransaction(hash);

                                String json = GSON.toJson(cosignatureSignedTransaction);

                                log.debug("CosignatureSignedTransaction: {}", json);

                                byte[] cosignatureSignedTransactionBytes = json.getBytes(StandardCharsets.UTF_8);

                                Result<String> confirmResult = GSON.fromJson(new String(paymentService.confirm(hash, cosignatureSignedTransactionBytes), StandardCharsets.UTF_8), Result.class);

                                log.debug("confirmResult: {}", paymentResult);

                                if (confirmResult.getCode() != 0) {
                                    log.error("confirmResult: {}", paymentResult);
                                } else {
                                    payoutDao.put(surf.getId(), new Payout(hash));
                                }
                            }
                        } else {
                            log.debug("Already payment: {}", payout);
                        }
                    } catch (Exception e) {
                        log.error(e.toString(), e);
                    }
                }
            });
        } catch (Exception e) {
            log.error(e.toString(), e);
        }

        return Response.ok(Response.OK);
    }
}
