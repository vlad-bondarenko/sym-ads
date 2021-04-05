package sym.ads.web.dao;

import sym.ads.core.AbstractRocksDBDao;
import one.nio.util.Utf8;

/**
 * Created by bondarenko.vlad@gmail.com on 24.10.18.
 */

public class SiteDao extends AbstractRocksDBDao<String, SiteDao.Site> {

    private static final SiteDao INSTANCE = new SiteDao();

    public static SiteDao getInstance() {
        return INSTANCE;
    }

    private SiteDao() {
        super(SiteDao.Site.class, Utf8::toBytes, Utf8::toString);
    }

    @Override
    protected byte[] getColumnFamilyName() {
        return Utf8.toBytes("site");
    }

    static class Site {
        final String name;
        final String url;
        final int category;
        final int linkSize;
        final String separator;

        public Site(String name, String url, int category, int linkSize, String separator) {
            this.name = name;
            this.url = url;
            this.category = category;
            this.linkSize = linkSize;
            this.separator = separator;
        }
    }
}
