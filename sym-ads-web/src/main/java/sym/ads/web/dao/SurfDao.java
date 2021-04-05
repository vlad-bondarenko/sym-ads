package sym.ads.web.dao;

import sym.ads.core.AbstractRocksDBDao;
import sym.ads.core.Utils;
import sym.ads.core.model.Surf;
import one.nio.util.Utf8;

/**
 * Created by vbondarenko on 02.06.2020.
 */

public final class SurfDao extends AbstractRocksDBDao<Long, Surf> {

    private static final SurfDao INSTANCE = new SurfDao();

    public static SurfDao getInstance() {
        return INSTANCE;
    }

    private SurfDao() {
        super(Surf.class, Utils::toBytes, Utils::toLong);
    }

    @Override
    protected byte[] getColumnFamilyName() {
        return Utf8.toBytes("surf");
    }

}
