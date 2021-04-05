package sym.ads.core;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static sym.ads.core.Utils.EMPTY;
import static one.nio.util.Utf8.toBytes;
import static org.apache.commons.lang3.StringUtils.isBlank;

public abstract class AbstractHttpHandler extends BaseClass implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        try {
            handle0(exchange);
        } catch (Exception e) {
            log.error(e.toString(), e);

            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            e.printStackTrace(writer);
            writer.close();

            try {
                sendResponse(500, exchange, toBytes(stringWriter.toString().replaceAll("\t", "    ")));
            } catch (Exception e1) {
                log.error(e1.toString(), e1);
            }
        }
    }

    protected abstract void handle0(HttpExchange exchange) throws Exception;

    public static void sendResponse(int code, HttpExchange httpExchange, byte[] bytes) throws IOException {
        httpExchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream outputStream = httpExchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    public static void sendResponseOk(HttpExchange httpExchange, byte[] bytes) throws IOException {
        sendResponse(200, httpExchange, bytes);
    }

    public static void sendResponseNotFound(HttpExchange httpExchange) throws IOException {
        sendResponse(404, httpExchange, EMPTY);
    }

    public static void sendRedirect(HttpExchange httpExchange, String url) throws IOException {
        httpExchange.getResponseHeaders().add("Location", url);
        httpExchange.sendResponseHeaders(302, -1);
    }

    public static void sendRedirect(HttpExchange httpExchange, String url, Map<String, String> headers) throws IOException {
        httpExchange.getResponseHeaders().add("Location", url);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpExchange.getResponseHeaders().add(entry.getKey(), entry.getValue());
        }
        httpExchange.sendResponseHeaders(302, -1);
    }

    public static void handleDefault(String webRoot, HttpExchange httpExchange) throws IOException {
        java.nio.file.Path path = Paths.get(webRoot, httpExchange.getRequestURI().getPath());
        if (Files.exists(path) && !Files.isDirectory(path)) {
            sendResponseOk(httpExchange, Files.readAllBytes(path));

            return;
        }

        sendResponseNotFound(httpExchange);
    }

    public static String readBody(HttpExchange httpExchange) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody()))) {
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            return content.toString();
            //    }
        }
    }

    public static HashMap<String, String> getParams(String body) {
        if (isBlank(body)) {
            return null;
        }

        String[] nameValuePairs = body.split("&");
        if (nameValuePairs.length == 0) {
            return null;
        }

        HashMap<String, String> map = new HashMap<>();

        String[] values;
        for (String nameValuePair : nameValuePairs) {
            values = nameValuePair.split("=");
            map.put(values[0], URLDecoder.decode(values[1], StandardCharsets.UTF_8));
        }

        return map;
    }
}
