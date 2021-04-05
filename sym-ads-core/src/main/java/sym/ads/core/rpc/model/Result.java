package sym.ads.core.rpc.model;

import java.io.Serializable;

/**
 * Created by vbondarenko on 12.07.2020.
 */

public final class Result<T> implements Serializable {

    private final T data;
    private final int code;
    private final String error;

    public Result(T data, int code, String error) {
        this.data = data;
        this.code = code;
        this.error = error;
    }

    public T getData() {
        return data;
    }

    public int getCode() {
        return code;
    }

    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        return "Result{" +
                "data=" + data +
                ", code=" + code +
                ", error='" + error + '\'' +
                '}';
    }
}
