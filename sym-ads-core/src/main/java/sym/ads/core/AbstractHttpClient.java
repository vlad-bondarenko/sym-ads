package sym.ads.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class AbstractHttpClient extends BaseClass {

    private final AbstractTemplateEngine templateEngine;

    protected AbstractHttpClient(AbstractTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String doPost(final String url, final int idx, final HashMap<String, byte[]> model) throws Exception {
        try (BufferedReader reader = doRequestReader(url, "POST", idx, model)) {
            if (reader == null) {
                return null;
            }

            return reader.lines().collect(Collectors.joining());
        }
    }

    public BufferedReader doRequestReader(final String url, final String method, final int idx, final HashMap<String, byte[]> model) throws Exception {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(60_000);
            connection.setReadTimeout(60_000);

            connection.setRequestProperty("Accept-Encoding", "application/json");

            if (idx > -1) {
                connection.setRequestMethod(method);
                connection.setDoOutput(true);
                templateEngine.render(idx, connection.getOutputStream(), model);
            }

            int code;
            if ((code = connection.getResponseCode()) == HttpURLConnection.HTTP_OK) {
                return new BufferedReader(new InputStreamReader(connection.getInputStream(), UTF_8));
            } else {
                try (BufferedReader buffer = new BufferedReader(new InputStreamReader(connection.getInputStream(), UTF_8))) {
                    log.error("{}, code = {}: {}", url, code, buffer.lines().collect(Collectors.joining()));

                    throw new Exception("Http error code = " + code);
                }
            }
        } catch (IOException e) {
            InputStream errorInputStream = null;
            if (connection != null) {
                errorInputStream = connection.getErrorStream();
            }

            if (errorInputStream != null) {
                try (BufferedReader buffer = new BufferedReader(new InputStreamReader(errorInputStream))) {
                    log.error("IO ERROR: {}", buffer.lines().collect(Collectors.joining()));
                } catch (Exception e1) {
                    log.error(e1.toString(), e1);
                }
            }

            throw e;
        }
    }
}
