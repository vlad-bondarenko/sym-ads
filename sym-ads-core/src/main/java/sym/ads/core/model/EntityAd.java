package sym.ads.core.model;

import static sym.ads.core.Constants.TYPE_AD;

/**
 * Created by vbondarenko on 03.01.2020.
 */

public class EntityAd extends Entity<Ad> {

    public EntityAd(Ad data) {
        super(TYPE_AD, data);
    }

    public EntityAd(String id) {
        super(id, TYPE_AD);
    }
}
