package sym.ads.core;

import java.math.BigInteger;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

/**
 * Created by vbondarenko on 09.02.2020.
 */

public final class Constants {

    public static final int SYM_DIVISIBILITY = 6;
    public static final long SYM_AMOUNT_MULTIPLY = BigInteger.TEN.pow(SYM_DIVISIBILITY).longValue();

    public static final char NUMBER_SEPARATOR = '.';

    //    public static final long TIME_DIFF = TimeUnit.DAYS.toMillis(2);//todo set for PRODUCTION
    public static final long TIME_DIFF = TimeUnit.SECONDS.toMillis(2);

    public static final int TYPE_CONFIG = 1;
    public static final int TYPE_SITE = 2;
    public static final int TYPE_AD = 3;

    public static final String CONTENT_TYPE_HTML = "Content-Type: text/html; charset=utf-8";
    public static final String CONTENT_TYPE_JAVASCRIPT = "Content-Type: application/javascript; charset=utf-8";

    //Config ids
    public static final String CONFIG_OTHER = "other";
    public static final String CONFIG_CATEGORY = "category";

    //Config keys
    public static final String CONFIG_WEBMASTER_SITE_DEFAULT = "w.site.default";
    //    String CONFIG_WEBMASTER_AD_DEFAULT = "w.ad.default";
    public static final String CONFIG_SERVICE_URL = "s.url";
    public static final String CONFIG_SERVICE_PUBLIC_KEY = "s.public.key";
    public static final String CONFIG_SERVICE_MOSAIC_ID = "s.mosaic.id";

    public static final Set<String> CONFIG_KEY_SET = new ConcurrentSkipListSet<>() {{
        add(CONFIG_WEBMASTER_SITE_DEFAULT);

        add(CONFIG_SERVICE_URL);
        add(CONFIG_SERVICE_PUBLIC_KEY);
        add(CONFIG_SERVICE_MOSAIC_ID);
    }};

    public static final String WEBMASTER_SITE_DEFAULT = "https://bitbucket.org/saves/nem-ads";

/*
    public static final String HEADER_SERVICE_AD_ID = "a.id";
    public static final String HEADER_SERVER_ID = "server.id";
*/

    public static final String PREFERENCE_SERVER_ID = "serverId";
}
