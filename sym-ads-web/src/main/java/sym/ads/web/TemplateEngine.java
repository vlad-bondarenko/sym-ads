package sym.ads.web;

import sym.ads.core.AbstractTemplateEngine;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by vbondarenko on 05.06.2020.
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
                Paths.get(WEB_ROOT, "/admin/sites.html"),
                Paths.get(WEB_ROOT, "/admin/site.html"),
                Paths.get(WEB_ROOT, "/admin/ads.html"),
                Paths.get(WEB_ROOT, "/admin/ad.html"),

                Paths.get(WEB_ROOT, "/admin/list/site.html"),
                Paths.get(WEB_ROOT, "/admin/list/ad.html"),
                Paths.get(WEB_ROOT, "/admin/list/option.html"),

                Paths.get(WEB_ROOT, "/ads.js"),
                Paths.get(WEB_ROOT, "/list/textAd.html"),

                Paths.get(WEB_ROOT, "/admin/site-history.html"),
                Paths.get(WEB_ROOT, "/admin/list/site-history.html"),
                Paths.get(WEB_ROOT, "/admin/ad-history.html"),
                Paths.get(WEB_ROOT, "/admin/list/ad-history.html")
        };
    }
}
