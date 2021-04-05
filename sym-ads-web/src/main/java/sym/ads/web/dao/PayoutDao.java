package sym.ads.web.dao;

import sym.ads.core.AbstractRocksDBDao;
import sym.ads.core.model.Payout;
import one.nio.util.Utf8;

/**
 * Created by vbondarenko on 16.10.2020.
 */

public class PayoutDao extends AbstractRocksDBDao<String, Payout> {

    private static final PayoutDao INSTANCE = new PayoutDao();

    public static PayoutDao getInstance() {
        return INSTANCE;
    }

    private PayoutDao() {
        super(Payout.class, Utf8::toBytes, Utf8::toString);
    }

    @Override
    protected byte[] getColumnFamilyName() {
        return Utf8.toBytes("payout");
    }
}
