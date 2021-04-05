package sym.ads.core.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by bondarenko.vlad@gmail.com on 30.10.18.
 */

public abstract class Entity<T> implements Serializable {

    private String id;
    private final int type;
    private T data;

    public Entity(int type, T data) {
        id = "";
        this.type = type;
        this.data = data;
    }

    public Entity(String id, int type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public T getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> entity = (Entity<?>) o;
        return type == entity.type &&
                Objects.equals(data, entity.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, data);
    }
}
