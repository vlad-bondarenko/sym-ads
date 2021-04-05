package sym.ads.core;

import one.nio.serial.Json;
import one.nio.serial.JsonReader;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static one.nio.util.Utf8.toBytes;

/**
 * Created by bondarenko.vlad@gmail.com on 26.10.18.
 */

public abstract class AbstractRocksDBDao<K extends Comparable<K>, V> extends BaseClass {

    protected RocksDB db;

    protected ColumnFamilyHandle columnFamilyHandle;

    protected final Function<K, byte[]> toBytesFunction;
    protected final Function<byte[], K> fromBytesFunction;
    protected final Class<V> type;

    protected AbstractRocksDBDao(Class<V> type, Function<K, byte[]> toBytesFunction, Function<byte[], K> fromBytesFunction) {
        this.type = type;
        this.toBytesFunction = toBytesFunction;
        this.fromBytesFunction = fromBytesFunction;
    }

    public void init() throws RocksDBException {
        columnFamilyHandle = Database.getInstance().getColumnFamilyHandle(getColumnFamilyName());
        if (columnFamilyHandle == null) {
            throw new RocksDBException("Not found columnFamilyHandle");
        }

        db = Database.getInstance().getDb();
    }

    protected abstract byte[] getColumnFamilyName();

    public void put(K key, V value) throws Exception {
        db.put(columnFamilyHandle, toBytesFunction.apply(key), toBytes(Json.toJson(value)));
    }

    public V get(K key) throws Exception {
        byte[] bytes = db.get(columnFamilyHandle, toBytesFunction.apply(key));
        if (bytes == null) {
            return null;
        }

        return new JsonReader(bytes).readObject(type);
    }

    public void delete(K key) throws Exception {
        db.delete(columnFamilyHandle, toBytesFunction.apply(key));
    }

    public void forEach(BiConsumer<K, V> biConsumer) throws Exception {
        RocksIterator iter = db.newIterator(columnFamilyHandle);
        iter.seekToFirst();
        while (iter.isValid()) {
            biConsumer.accept(fromBytesFunction.apply(iter.key()), new JsonReader(iter.value()).readObject(type));

            iter.next();
        }
    }

    public void forEach(K keyMin, K keyMax, BiConsumer<K, V> biConsumer) throws Exception {
        try (RocksIterator iter = db.newIterator(columnFamilyHandle)) {
            if (keyMin == null) {
                iter.seekToFirst();
            } else {
                iter.seek(toBytesFunction.apply(keyMin));
            }
            while (iter.isValid()) {
                K key = fromBytesFunction.apply(iter.key());
                if (keyMax != null) {
                    if (key.compareTo(keyMax) >= 0)
                        break;
                }

                biConsumer.accept(key, new JsonReader(iter.value()).readObject(type));

                iter.next();
            }
        }
    }
}
