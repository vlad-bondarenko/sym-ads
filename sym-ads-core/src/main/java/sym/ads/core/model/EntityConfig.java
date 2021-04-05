package sym.ads.core.model;

import java.util.Map;

import static sym.ads.core.Constants.TYPE_CONFIG;

/**
 * Created by vbondarenko on 10.05.2020.
 */

public class EntityConfig extends Entity<Map<String, ?>> {

    public EntityConfig(String id, Map<String, ?> data) {
        super(TYPE_CONFIG, data);

        setId(id);
    }

    public EntityConfig(Map<String, ?> data) {
        super(TYPE_CONFIG, data);
    }

    public EntityConfig(String id) {
        super(id, TYPE_CONFIG);
    }
}
