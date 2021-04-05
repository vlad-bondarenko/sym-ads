package sym.ads.service;

import one.nio.config.ConfigParser;
import one.nio.http.*;
import one.nio.rpc.RpcServer;
import one.nio.server.ServerConfig;
import one.nio.util.Utf8;
import org.slf4j.Logger;
import sym.ads.core.ShutdownHookHandler;
import sym.ads.core.rpc.PaymentService;
import sym.ads.service.rpc.PaymentServiceImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_POST;
import static one.nio.http.Response.BAD_REQUEST;
import static one.nio.http.Response.INTERNAL_ERROR;
import static org.slf4j.LoggerFactory.getLogger;
import static sym.ads.core.Utils.read;
import static sym.ads.service.TemplateEngine.WEB_ROOT;

/**
 * Created by vbondarenko on 16.05.2020.
 */

public class ServiceServer extends HttpServer {

    private static final Logger log = getLogger(ServiceServer.class);

    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(true);

    private final ConfigHandler configHandler;
//    private final PaymentHandler paymentHandler;

    public ServiceServer(HttpServerConfig config) throws Exception {
        super(config);

        ShutdownHookHandler.getInstance().addShutdownHook(() -> {
            IS_RUNNING.set(false);

            log.info("Terminate HTTP-server");

            stop();
        }, true);

        StateService.getInstance().init();

        configHandler = StateService.getInstance().getConfigHandler();
//        paymentHandler = StateService.getInstance().getPaymentHandler();
    }

/*
    @Path("/payment")
    @RequestMethod(METHOD_POST)
    public void payment(
            @Param(value = "serverId", required = true) String serverId,
            @Param(value = "adId", required = true) String adId,
            @Param(value = "recipient", required = true) String recipient,
            @Param(value = "amount", required = true) long amount,
            @Param(value = "publicKey", required = true) String publicKey,

            HttpSession session,
            Request request) throws Exception {
        log.debug("POST: {}, {}, {}", request.getPath(), Utf8.toString(request.getBody()), request.getHeaders());

        session.sendResponse(paymentHandler.payment(serverId, adId, recipient, amount, publicKey));
    }
*/

    @Path({"/admin/", "/admin"})
    @RequestMethod(METHOD_GET)
    public void adminRoot(HttpSession session) throws Exception {
        session.sendResponse(Response.redirect("/admin/index.html"));
    }

    @Path("/admin/configs.html")
    public void adminConfigs(HttpSession session) throws Exception {
        session.sendResponse(configHandler.adminConfigs());
    }

    @Path("/admin/config.html")
    @RequestMethod(METHOD_POST)
    public void adminConfig(HttpSession session, Request request) throws Exception {
        String body = Utf8.toString(request.getBody());

        log.debug("POST: {}, {}, {}", request.getPath(), body, request.getHeaders());

        session.sendResponse(configHandler.adminConfig(body));
    }

    @Override
    public void handleRequest(Request request, HttpSession session) throws IOException {
        try {
            super.handleRequest(request, session);
        } catch (Exception e) {
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
            HttpServerConfig httpServerConfig = ConfigParser.parse(read(ServiceServer.class.getResource("/http.yml")), HttpServerConfig.class);

            ServiceServer serviceServer = new ServiceServer(httpServerConfig);

            log.info("Start HTTP-serviceServer, {}:{}", httpServerConfig.acceptors[0].address, httpServerConfig.acceptors[0].port);

            serviceServer.start();
        } catch (Exception e) {
            log.error(e.toString(), e);

            System.exit(-1);
        }
    }
}
