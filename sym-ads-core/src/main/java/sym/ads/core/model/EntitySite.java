package sym.ads.core.model;

import static sym.ads.core.Constants.TYPE_SITE;

/**
 * Created by vbondarenko on 03.01.2020.
 */

public final class EntitySite extends Entity<Site> {

    public EntitySite(Site data) {
        super(TYPE_SITE, data);
    }

    public EntitySite(String id) {
        super(id, TYPE_SITE);
    }

}
