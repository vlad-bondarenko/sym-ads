package sym.ads.core;

import org.rocksdb.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by bondarenko.vlad@gmail.com on 25.10.18.
 */

public class Database extends BaseClass {

    private RocksDB db;
    private List<ColumnFamilyHandle> columnFamilyHandleList;
    private Runnable destroyTask;

    private static Database ourInstance = new Database();

    public static Database getInstance() {
        return ourInstance;
    }

    private Database() {
        RocksDB.loadLibrary();
    }

    public void init(AbstractRocksDBDao<?, ?>... daos) throws RocksDBException {
        byte[][] columnFamilyNames = new byte[daos.length][];
        for (int i = 0; i < daos.length; i++) {
            AbstractRocksDBDao<?, ?> dao = daos[i];
            columnFamilyNames[i] = dao.getColumnFamilyName();
        }

        init(columnFamilyNames);

        for (AbstractRocksDBDao<?, ?> dao : daos) {
            dao.init();
        }
    }

    public void init(byte[]... columnFamilyNames) {
        try {
            if (db != null) {
                throw new IllegalStateException("Already init");
            }

            final ColumnFamilyOptions cfOpts = new ColumnFamilyOptions().optimizeUniversalStyleCompaction();

            // list of column family descriptors, first entry must always be default column family
            final List<ColumnFamilyDescriptor> cfDescriptors = new ArrayList<>(columnFamilyNames.length + 1);
            cfDescriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOpts));
            for (byte[] bytes : columnFamilyNames) {
                cfDescriptors.add(new ColumnFamilyDescriptor(bytes, cfOpts));
            }

            // a list which will hold the handles for the column families once the db is opened
            columnFamilyHandleList = new ArrayList<>();

            final DBOptions options = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true);
            db = RocksDB.open(options, "data", cfDescriptors, columnFamilyHandleList);

            destroyTask = () -> {
                for (final ColumnFamilyHandle columnFamilyHandle : columnFamilyHandleList) {
                    try {
                        columnFamilyHandle.close();
                    } catch (Exception e) {
                        log.error(e.toString(), e);
                    }
                }

                try {
                    db.close();
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }

                try {
                    options.close();
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }

                try {
                    cfOpts.close();
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ColumnFamilyHandle getColumnFamilyHandle(byte[] columnFamilyName) throws RocksDBException {
        for (ColumnFamilyHandle handle : columnFamilyHandleList) {
            if (Arrays.equals(columnFamilyName, handle.getName())) {
                return handle;
            }
        }

        return null;
    }

    public RocksDB getDb() {
        return db;
    }

    public void terminate() {
        destroyTask.run();
    }
}
