package sym.ads.web.dao;

import sym.ads.core.AbstractRocksDBDao;
import sym.ads.core.Utils;
import sym.ads.core.model.Click;
import one.nio.util.Utf8;

/**
 * Created by bondarenko.vlad@gmail.com on 29.10.18.
 */

public class ClickDao extends AbstractRocksDBDao<Long, Click> {

    private static final ClickDao INSTANCE = new ClickDao();

    public static ClickDao getInstance() {
        return INSTANCE;
    }

    private ClickDao() {
        super(Click.class, Utils::toBytes, Utils::toLong);
    }

    @Override
    protected byte[] getColumnFamilyName() {
        return Utf8.toBytes("click");
    }
}
