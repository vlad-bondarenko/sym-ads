package sym.ads.core.model;

import java.util.Map;

/**
 * Created by vbondarenko on 11.05.2020.
 */

public class EntityDto extends Entity<Map<String, Object>> {

    public EntityDto(int type, Map<String, Object> data) {
        super(type, data);
    }

    public EntityDto(String id, int type) {
        super(id, type);
    }
}
