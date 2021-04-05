package sym.ads.web.service;

import sym.ads.core.model.Ad;
import sym.ads.core.model.Site;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by bondarenko.vlad@gmail.com on 29.10.18.
 */

public class AuctionService {

    private final ConcurrentHashMap<Integer, ConcurrentSkipListSet<Ad>> map = new ConcurrentHashMap<>(256);
    private final Comparator<Ad> comparator;

    private static final AuctionService INSTANCE = new AuctionService();

    public static AuctionService getInstance() {
        return INSTANCE;
    }

    private AuctionService() {
        comparator = (o1, o2) -> {
/*
            if (o2.balance > o1.balance) {
                return 1;
            }

            if (o2.balance < o1.balance) {
                return -1;
            }

            return Long.compare(o2.mosaic, o1.mosaic);
*/
            return 0;
        };
    }

    public void update(Ad ad) {
        map.computeIfAbsent(ad.getCategory(), key -> new ConcurrentSkipListSet<>(comparator)).add(ad);
    }

    public Ad[] ads(Site site) {
        return null;
/*
        return new Ad[]{
                new Ad("1", OperationType.update.name(), "/click?advertiser1", "label1", category, 100),
                new Ad("2", OperationType.update.name(), "/click?advertiser2", "label2", category, 200),
                new Ad("3", OperationType.update.name(), "/click?advertiser3", "label3", category, 300),
                new Ad("4", OperationType.update.name(), "/click?advertiser4", "label4", category, 400)
        };
*/
    }
}
