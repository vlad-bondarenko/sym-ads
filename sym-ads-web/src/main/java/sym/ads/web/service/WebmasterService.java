package sym.ads.web.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.nem.symbol.sdk.model.blockchain.BlockInfo;
import sym.ads.core.*;
import sym.ads.core.model.Ad;
import sym.ads.core.model.Click;
import sym.ads.core.model.EntitySite;
import sym.ads.core.model.Site;
import sym.ads.web.TemplateEngine;
import sym.ads.web.dao.ClickDao;
import one.nio.http.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

import static sym.ads.core.Constants.*;
import static sym.ads.core.Utils.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Created by vbondarenko on 05.06.2020.
 */

public final class WebmasterService extends BaseClass implements Consumer<BlockInfo> {

    private final SymConnector symConnector = SymConnector.getInstance();
    private final TemplateEngine templateEngine = TemplateEngine.getInstance();
    private final ConcurrentHashMap<Integer, String> categories = StateService.getInstance().getCategories();
    private final ConcurrentSkipListMap<String, Site> sites = StateService.getInstance().getSites();
    private final ConcurrentHashMap<Integer, ConcurrentMap<String, Ad>> auction = StateService.getInstance().getAuction();
    private final ConcurrentNavigableMap<String, Ad> ads = StateService.getInstance().getAds();
    private final ConcurrentHashMap<String, Object> configs = StateService.getInstance().getConfigs();
    private final ConcurrentHashMap<String, String> siteHosts = StateService.getInstance().getSiteHosts();

    private final BiPredicate<String, ByteArrayOutputStream> SITE_LIST_BI_PREDICATE;
    private final BiPredicate<String, ByteArrayOutputStream> CATEGORY_OPTION_LIST_BI_PREDICATE;
    private final Function<Site, BiPredicate<String, ByteArrayOutputStream>> AUCTION_AD_LIST_FUNCTION;
    private final Function<String, BiPredicate<String, ByteArrayOutputStream>> CLICK_LIST_FUNCTION;

    private final ClickDao clickDao = StateService.getInstance().getClickDao();

    private final Cache<String, Boolean> clickCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build();

    public WebmasterService() {
        SITE_LIST_BI_PREDICATE = (name, byteArrayOutputStream) -> {
            if (!"list.site".equals(name)) {
                return false;
            }

//                SITE_DAO.forEach((id, site) -> {
            HashMap<String, byte[]> model = new HashMap<>();
            String id;
            Site site;
            for (Map.Entry<String, Site> entry : sites.entrySet()) {
                id = entry.getKey();
                site = entry.getValue();
                model.put("id", toBytesOrDefault(id));
                model.put("name", toBytesOrDefault(site.getName()));
                model.put("desc", toBytesOrDefault(site.getDesc()));
                model.put("url", toBytesOrDefault(site.getUrl()));
                model.put("category", toBytesOrDefault(categories.get(site.getCategory())));
                model.put("linkSize", toBytesOrDefault(String.valueOf(site.getLinkSize())));
                model.put("separator", toBytesOrDefault(site.getSeparator()));
                model.put("length", toBytesOrDefault(String.valueOf(site.getLength())));

                try {
                    templateEngine.render(4, byteArrayOutputStream, model);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return true;
        };

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

        AUCTION_AD_LIST_FUNCTION = site -> (name, byteArrayOutputStream) -> {
            if (!"list.ad".equals(name)) {
                return false;
            }

//                Ad[] ads = AUCTION_SERVICE.ads(site);
            Set<Map.Entry<String, Ad>> ads = auction.get(site.getCategory()).entrySet();
            HashMap<String, byte[]> model = new HashMap<>(2);
            for (Map.Entry<String, Ad> entry : ads) {
                model.put("name", toBytesOrDefault(entry.getValue().getName()));
                model.put("url", toBytesOrDefault("/click?id=" + entry.getKey()));

                try {
                    templateEngine.render(8, byteArrayOutputStream, model);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return true;
        };

        CLICK_LIST_FUNCTION = id -> (name, byteArrayOutputStream) -> {
            if (clickDao == null || !"list.click".equals(name)) {
                return false;
            }

            try {
                HashMap<String, byte[]> model = new HashMap<>(4);
                long current = System.currentTimeMillis();
                clickDao.forEach((key, click) -> {
                    if (id.equals(click.getId())) {
                        long time = IDs.timestampOf(key);

                        if (current - time > TIME_DIFF) {
                            try {
                                log.debug("{}: History delete", key);

//                                    clickDao.delete(key);
                            } catch (Exception e) {
                                log.error(e.toString(), e);
                            }

//                            return;
                        }

                        model.put("date", toBytesOrDefault(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        model.put("host", toBytesOrDefault(click.getHost()));
                        model.put("referrer", toBytesOrDefault(click.getReferrer()));
                        model.put("destinationId", toBytesOrDefault(click.getDestinationId()));
                        model.put("country", toBytesOrDefault(click.getCountry()));

                        try {
                            templateEngine.render(10, byteArrayOutputStream, model);
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

    public Response adminSites() throws Exception {
        Response response = Response.ok(templateEngine.render(
                0,
                new ByteArrayOutputStream(),
                null,
                SITE_LIST_BI_PREDICATE
        ).toByteArray());
        response.addHeader(Constants.CONTENT_TYPE_HTML);

        return response;
    }

    public Response adminSite(String deleteId) throws Exception {
        if (isNotBlank(deleteId)) {
            try {
                log.info("{}: Delete site", deleteId);

                symConnector.save(new EntitySite(deleteId));
            } catch (Exception e) {
                log.error(e.toString(), e);
            }

            return Response.redirect("/admin/sites.html");
        }

        Response response = Response.ok(templateEngine.render(
                1,
                new ByteArrayOutputStream(),
                null,
                CATEGORY_OPTION_LIST_BI_PREDICATE
        ).toByteArray());
        response.addHeader(CONTENT_TYPE_HTML);

        return response;
    }

    public Response adminSiteForm(String body) throws Exception {
        Map<String, List<String>> params = Utils.parseParameters(body);

        String id = firstParam(params, "id");
        String name = firstParam(params, "name");
        String url = firstParam(params, "url");
        String desc = firstParam(params, "desc");
        String separator = firstParam(params, "separator");

        int category = parseInt(firstParam(params, "category"));
        int linkSize = parseInt(firstParam(params, "linkSize"));
        int length = parseInt(firstParam(params, "length"));

        if (isNotBlank(name)
                && isNotBlank(url)
                && isNotBlank(desc)
                && category > 0
                && linkSize > 0
                && isNotBlank(separator)
                && length > 0) {
            Site site = id == null ? null : sites.get(id);
            if (site == null
                    || (!site.getUrl().equals(url)
                    || !site.getDesc().equals(desc)
                    || category != site.getCategory()
                    || linkSize != site.getLinkSize()
                    || !site.getSeparator().equals(separator)
                    || length != site.getLength()
            )) {
                site = new Site(url, desc, name, category, linkSize, separator, length);
            } else {
                site = null;
            }

            if (site != null) {
                try {
                    //                            SITE_DAO.put(name, new SiteDao.Site(name, url, category, linkSize, separator));
                    symConnector.save(new EntitySite(site));
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }
            }
        }

        return Response.redirect("/admin/sites.html");
    }

    public Response adminSiteHistory(String id) throws IOException {
        log.info("{}: History site", id);

        Site site = sites.get(id);
        if (site == null) {
            return Response.ok(EMPTY);
        }

        HashMap<String, byte[]> model = new HashMap<>(4);
        model.put("name", toBytesOrDefault(site.getName()));
        model.put("desc", toBytesOrDefault(site.getDesc()));
        model.put("url", toBytesOrDefault(site.getUrl()));
        model.put("category", toBytesOrDefault(categories.get(site.getCategory())));

        return Response.ok(templateEngine.render(
                9,
                new ByteArrayOutputStream(),
                model,
                CLICK_LIST_FUNCTION.apply(id)).toByteArray()
        );
    }

    public Response click(String id, String referer, String host) throws Exception {
        if (isBlank(id) || isBlank(referer) || isBlank(host)) {
            return Response.redirect((String) configs.getOrDefault(CONFIG_WEBMASTER_SITE_DEFAULT, WEBMASTER_SITE_DEFAULT));
        }

        try {
            Ad ad;
            String url;
            if ((ad = ads.get(id)) != null && isNotBlank(url = ad.getUrl())) {
                //           String ip = InetAddress.getByName(headers.getFirst("Host").split(":")[0]).getHostAddress();

                AtomicLong clickIdReference = new AtomicLong();
                clickCache.get(id, s -> {
                    try {
                        clickIdReference.set(IDs.newId());
                        clickDao.put(clickIdReference.get(), new Click(siteHosts.get(new URL(referer).getHost()), referer, host, "country", id));//todo country
                    } catch (Exception e) {
                        log.error(e.toString(), e);
                    }

                    return Boolean.TRUE;
                });

                return url.contains("?") ? Response.redirect(url + "&clickId=" + clickIdReference.get()) : Response.redirect(url + "?clickId=" + clickIdReference.get());
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }

        return Response.redirect((String) configs.getOrDefault(CONFIG_WEBMASTER_SITE_DEFAULT, WEBMASTER_SITE_DEFAULT));
    }

    public Response ads(String id) throws Exception {
        Site site = sites.get(id);
        if (site == null) {
            return Response.ok(EMPTY);
        }

        Response response = Response.ok(templateEngine.render(
                7,
                new ByteArrayOutputStream(),
                null,
                AUCTION_AD_LIST_FUNCTION.apply(site)).toByteArray()
        );

        response.addHeader(CONTENT_TYPE_JAVASCRIPT);
        response.addHeader("Cache-Control: no-cache, no-store, must-revalidate");
        response.addHeader("Pragma: no-cache");
        response.addHeader("Expires: 0");

        return response;
    }

    @Override
    public void accept(BlockInfo blockInfo) {
        log.debug("New block");


    }
}
