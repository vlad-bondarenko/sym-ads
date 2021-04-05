package sym.ads.web.service;

import io.nem.symbol.sdk.model.blockchain.BlockInfo;
import sym.ads.core.BaseClass;
import sym.ads.core.SymConnector;
import sym.ads.web.TemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static sym.ads.core.Utils.toBytesOrDefault;

/**
 * Created by vbondarenko on 05.06.2020.
 */

public class AdminService extends BaseClass implements Consumer<BlockInfo> {

    private final SymConnector symConnector = SymConnector.getInstance();
    private final TemplateEngine templateEngine = TemplateEngine.getInstance();
    private final ConcurrentHashMap<Integer, String> categories = StateService.getInstance().getCategories();

    public final BiPredicate<String, ByteArrayOutputStream> CATEGORY_OPTION_LIST_BI_PREDICATE;

    public AdminService() {
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

    }

    @Override
    public void accept(BlockInfo blockInfo) {

    }
}
