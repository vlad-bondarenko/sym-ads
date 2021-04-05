package sym.ads.service;

import io.nem.symbol.sdk.model.blockchain.BlockInfo;
import sym.ads.core.BaseClass;
import sym.ads.core.Constants;
import sym.ads.core.SymConnector;
import sym.ads.core.Utils;
import sym.ads.core.model.EntityConfig;
import one.nio.http.Response;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static sym.ads.core.Constants.CONTENT_TYPE_HTML;
import static sym.ads.core.Utils.firstParam;
import static one.nio.util.Utf8.toBytes;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Created by vbondarenko on 08.07.2020.
 */

public final class ConfigHandler extends BaseClass implements Consumer<BlockInfo> {

    private final SymConnector symConnector = SymConnector.getInstance();
    private final TemplateEngine templateEngine = TemplateEngine.getInstance();

    private final ConcurrentHashMap<Integer, String> categories = StateService.getInstance().getCategories();
    private final ConcurrentHashMap<String, Object> configs = StateService.getInstance().getConfigs();

    private final BiPredicate<String, ByteArrayOutputStream> CATEGORY_LIST_BI_PREDICATE;
    private final BiPredicate<String, ByteArrayOutputStream> CONFIG_LIST_BI_PREDICATE;

    public ConfigHandler() {
        CONFIG_LIST_BI_PREDICATE = (name, byteArrayOutputStream) -> {
            if (!name.equals("list.config")) {
                return false;
            }

            configs.forEach((key, value) -> {
                HashMap<String, byte[]> model = new HashMap<>();
                model.put("id", toBytes("other"));
                model.put("name", toBytes(key));
                model.put("value", toBytes(String.valueOf(value)));

                try {
                    templateEngine.render(1, byteArrayOutputStream, model);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            return true;
        };

        CATEGORY_LIST_BI_PREDICATE = (name, byteArrayOutputStream) -> {
            if (!name.equals("list.category")) {
                return false;
            }

            categories.forEach((key, value) -> {
                HashMap<String, byte[]> model = new HashMap<>();
                model.put("id", toBytes(Constants.CONFIG_CATEGORY));
                model.put("name", toBytes(String.valueOf(key)));
                model.put("value", toBytes(String.valueOf(value)));

                try {
                    templateEngine.render(2, byteArrayOutputStream, model);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            return true;
        };
    }

    public Response adminConfigs() throws Exception {
        Response response = Response.ok(templateEngine.render(
                0,
                new ByteArrayOutputStream(),
                null,
                CONFIG_LIST_BI_PREDICATE.or(CATEGORY_LIST_BI_PREDICATE)
        ).toByteArray());
        response.addHeader(CONTENT_TYPE_HTML);

        return response;
    }

    public Response adminConfig(String body) throws Exception {
        String id = firstParam(Utils.parseParameters(body), "id");
        String values = firstParam(Utils.parseParameters(body), "values");

        if (isNotBlank(values)) {
            Map<String, Object> map = new LinkedHashMap<>();

            values.lines().forEach(s -> {
                String[] v = s.split("=", 2);
                if (v.length == 2) {
                    if (NumberUtils.isParsable(v[1])) {
                        if (v[1].contains(".")) {
                            map.putIfAbsent(v[0], Double.valueOf(v[1]));
                        } else {
                            map.putIfAbsent(v[0], Long.valueOf(v[1]));
                        }
                    } else {
                        map.putIfAbsent(v[0], v[1]);
                    }
                }
            });

            if (map.size() > 0) {
                try {
                    if (id != null && id.length() > 0) {
                        if (!Constants.CONFIG_CATEGORY.equals(id) && !Constants.CONFIG_OTHER.equals(id)) {
                            id = "";
                        }
                    }

                    symConnector.save(new EntityConfig(id, map));
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }
            }
        }

        return Response.redirect("/admin/configs.html");
    }

    @Override
    public void accept(BlockInfo blockInfo) {
        log.debug("New block");
    }
}
