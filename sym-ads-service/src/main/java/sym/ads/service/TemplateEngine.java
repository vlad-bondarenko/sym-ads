package sym.ads.service;

import sym.ads.core.AbstractTemplateEngine;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by vbondarenko on 08.07.2020.
 */

public final class TemplateEngine extends AbstractTemplateEngine {

    public static final String WEB_ROOT = "web";

    private static final TemplateEngine INSTANCE = new TemplateEngine();

    public static TemplateEngine getInstance() {
        return INSTANCE;
    }

    private TemplateEngine() {
    }

    @Override
    protected Path[] templatePaths() {
        return new Path[]{
                Paths.get(WEB_ROOT, "/admin/configs.html"),
                Paths.get(WEB_ROOT, "/admin/list/config.html"),
                Paths.get(WEB_ROOT, "/admin/list/category.html")
        };
    }
}
