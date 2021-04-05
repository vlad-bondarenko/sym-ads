package sym.ads.web;

import sym.ads.core.ShutdownHookHandler;
import sym.ads.web.controller.AdvertiserController;
import sym.ads.web.controller.WebmasterController;
import sym.ads.web.service.StateService;
import one.nio.config.ConfigParser;
import one.nio.http.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static sym.ads.core.Utils.read;
import static sym.ads.web.TemplateEngine.WEB_ROOT;
import static sym.ads.web.service.StateService.IS_RUNNING;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Response.BAD_REQUEST;
import static one.nio.http.Response.INTERNAL_ERROR;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by vbondarenko on 13.06.2020.
 */

public class SymAdsServer extends HttpServer {

    private static final Logger log = getLogger(SymAdsServer.class);

    public SymAdsServer(HttpServerConfig config, Object... controllers) throws IOException {
        super(config, controllers);

        ShutdownHookHandler.getInstance().addShutdownHook(() -> {
            IS_RUNNING.set(false);

            log.info("Terminate HTTP-server");

            stop();
        }, true);
    }

    @Path({"/admin/", "/admin"})
    @RequestMethod(METHOD_GET)
    public void adminRoot(HttpSession session) throws IOException {
        session.sendResponse(Response.redirect("/admin/index.html"));
    }

    @Override
    public void handleRequest(Request request, HttpSession session) throws IOException {
        try {
            super.handleRequest(request, session);
        } catch (Exception e) {
            if (e instanceof HttpException) {
                log.error("{}: {}", e.toString(), request);

                session.sendError(BAD_REQUEST, BAD_REQUEST);

                return;
            }

            log.error(e.toString(), e);

            session.sendError(INTERNAL_ERROR, INTERNAL_ERROR);
        }
    }

    @Override
    public void handleDefault(Request request, HttpSession session) throws IOException {
        java.nio.file.Path path = Paths.get(WEB_ROOT, request.getPath());
        if (Files.exists(path) && !Files.isDirectory(path)) {
            session.sendResponse(Response.ok(Files.readAllBytes(path)));

            return;
        }

        session.sendError(BAD_REQUEST, BAD_REQUEST);
    }

    public static void main(String[] args) {
        try {
            final HttpServerConfig config = ConfigParser.parse(read(SymAdsServer.class.getResource("/http.yml")), HttpServerConfig.class);

            StateService.getInstance().init();

            final List<Object> controllers = new ArrayList<>();
            if (StateService.getInstance().isWebmasterEnabled()) {
                controllers.add(new WebmasterController());
            }
            if (StateService.getInstance().isAdvertiserEnabled()) {
                controllers.add(new AdvertiserController());
            }

            final SymAdsServer server = new SymAdsServer(config, controllers.toArray());

            log.info("Start HTTP-server, {}:{}", config.acceptors[0].address, config.acceptors[0].port);

            server.start();
        } catch (Throwable e) {
            log.error(e.toString(), e);

            e.printStackTrace();

            System.exit(-1);
        }
    }
}
