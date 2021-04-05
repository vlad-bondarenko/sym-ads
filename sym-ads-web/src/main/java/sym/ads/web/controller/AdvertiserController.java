package sym.ads.web.controller;

import sym.ads.core.BaseClass;
import sym.ads.web.service.AdvertiserService;
import sym.ads.web.service.StateService;
import one.nio.http.*;
import one.nio.util.Utf8;

import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_POST;

/**
 * Created by vbondarenko on 10.01.2021.
 */

public final class AdvertiserController extends BaseClass {

    private final AdvertiserService service = StateService.getInstance().getAdvertiserService();

    @Path("/admin/ads.html")
    @RequestMethod(METHOD_GET)
    public void adminAds(HttpSession session) throws Exception {
        session.sendResponse(service.adminAds());
    }

    @Path("/admin/ad.html")
    @RequestMethod(METHOD_GET)
    public void adminAd(
            @Param("delete") String deleteId,
            @Param("history") String historyId,
            HttpSession session
    ) throws Exception {
        session.sendResponse(service.adminAd(deleteId, historyId));
    }

    @Path("/admin/ad.html")
    @RequestMethod(METHOD_POST)
    public void adminAdForm(Request request, HttpSession session) throws Exception {
        String body = Utf8.toString(request.getBody());

        log.debug("POST: {}, {}, {}", request.getPath(), body, request.getHeaders());

        session.sendResponse(service.adminAdForm(body));
    }

    @Path("/admin/ad-history.html")
    @RequestMethod(METHOD_GET)
    public void adminAdHistory(
            @Param(value = "id", required = true) String id,
            HttpSession session) throws Exception {
        session.sendResponse(service.adminAdHistory(id));
    }

    @Path("/ad")
    @RequestMethod(METHOD_GET)
    public void ad(
            @Param("id") String id,

            @Header(value = "Referer", required = true) String referer,
            @Header(value = "Host", required = true) String host,
            HttpSession session
    ) throws Exception {
        session.sendResponse(service.ad(id, referer, host));
    }

    @Path("/payment")
    @RequestMethod(METHOD_POST)
    public void payment(Request request, HttpSession session) throws Exception {
        String body = Utf8.toString(request.getBody());

        log.debug("POST: {}, {}, {}", request.getPath(), body, request.getHeaders());

        session.sendResponse(service.payment());
    }
}
