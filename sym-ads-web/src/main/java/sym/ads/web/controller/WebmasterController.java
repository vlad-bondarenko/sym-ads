package sym.ads.web.controller;

import sym.ads.core.BaseClass;
import sym.ads.web.service.StateService;
import sym.ads.web.service.WebmasterService;
import one.nio.http.*;
import one.nio.util.Utf8;

import java.io.IOException;

import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_POST;

/**
 * Created by vbondarenko on 10.01.2021.
 */

public final class WebmasterController extends BaseClass {

    private final WebmasterService service = StateService.getInstance().getWebmasterHandler();

    @Path("/admin/sites.html")
    @RequestMethod(METHOD_GET)
    public void adminSites(HttpSession session) throws Exception {
        session.sendResponse(service.adminSites());
    }

    @Path("/admin/site.html")
    @RequestMethod(METHOD_GET)
    public void adminSite(
            @Param("delete") String deleteId,
            HttpSession session
    ) throws Exception {
        session.sendResponse(service.adminSite(deleteId));
    }

    @Path("/admin/site.html")
    @RequestMethod(METHOD_POST)
    public void adminSiteForm(Request request, HttpSession session) throws Exception {
        String body = Utf8.toString(request.getBody());

        log.debug("POST: {}, {}, {}", request.getPath(), body, request.getHeaders());

        session.sendResponse(service.adminSiteForm(body));
    }

    @Path("/admin/site-history.html")
    @RequestMethod(METHOD_GET)
    public void adminSiteHistory(
            @Param(value = "id", required = true) String id,
            HttpSession session) throws IOException {
        session.sendResponse(service.adminSiteHistory(id));
    }

    @Path("/click")
    @RequestMethod(METHOD_GET)
    public void click(
            @Param(value = "id", required = true) String id,

            @Header(value = "Referer", required = true) String referer,
            @Header(value = "Host", required = true) String host,
            HttpSession session
    ) throws Exception {
        session.sendResponse(service.click(id, referer, host));
    }

    @Path("/ads")
    @RequestMethod(METHOD_GET)
    public void ads(
            @Param(value = "id", required = true) String id,
            HttpSession session
    ) throws Exception {
        session.sendResponse(service.ads(id));
    }

}

