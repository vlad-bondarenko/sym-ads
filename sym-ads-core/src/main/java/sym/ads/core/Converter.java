package sym.ads.core;

import sym.ads.core.model.Ad;
import sym.ads.core.model.EntityDto;
import sym.ads.core.model.Site;

import java.util.Map;

/**
 * Created by vbondarenko on 11.05.2020.
 */

public final class Converter {

    public static Site toSite(EntityDto entityDto) {
        if (entityDto.getId() == null || entityDto.getId().isBlank()) {
            Map<String, Object> data = entityDto.getData();

            return new Site(
                    (String) data.get("url"),
                    (String) data.get("desc"),
                    (String) data.get("name"),
                    (int) data.get("category"),
                    (int) data.get("linkSize"),
                    (String) data.get("separator"),
                    (int) data.get("length")
            );
        }

        return null;
    }

    public static Ad toAd(EntityDto entityDto) {
        if (entityDto.getId() == null || entityDto.getId().isBlank()) {
            Map<String, Object> data = entityDto.getData();

            Ad ad = new Ad(
                    (String) data.get("url"),
                    (String) data.get("desc"),
                    (String) data.get("name"),
                    (int) data.get("category"),
                    data.get("price") instanceof Long ? (long) data.get("price") : (int) data.get("price")
            );
            if (data.containsKey("country")) {
                ad.setCountry((String) data.get("country"));
            }
            if (data.containsKey("fromHour")) {
                ad.setFromHour((int) data.get("fromHour"));
            }
            if (data.containsKey("toHour")) {
                ad.setToHour((int) data.get("toHour"));
            }
            if (data.containsKey("fromWeek")) {
                ad.setFromWeek((int) data.get("fromWeek"));
            }
            if (data.containsKey("toWeek")) {
                ad.setToWeek((int) data.get("toWeek"));
            }
            if (data.containsKey("bl")) {
                ad.setBl(data.get("bl") instanceof Long ? (long) data.get("bl") : (int) data.get("bl"));
            }

            return ad;
        }

        return null;
    }

}
