package sym.ads.core;

import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicLong;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * Created by bondarenko.vlad@gmail.com on 29.10.18.
 */

public final class IDs {

    private static final AtomicLong LAST_TIMESTAMP = new AtomicLong(0L);
    static long serverId;
    private static Logger logger = getLogger(IDs.class);

    //    private static final long key = 0x7f593ace3f01b7d8L;
    private static long lastTime;

    static {
        String serverIdStr = System.getProperty("serverId", "1");
        if (serverIdStr == null || serverIdStr.isEmpty()) {
            throw new IllegalArgumentException("Not specified 'serverId'");
        }

        serverId = Long.parseLong(serverIdStr);

        if (serverId < 1 || serverId > 127) {
            throw new IllegalArgumentException("Invalid 'serverId'");
        }

        logger.info("Server id = {}", serverId);
    }

    public static long timestampOf(long id) {
        return id >>> 20;
    }

    public static long timestampOfBatch(long id) {
        return id >>> 7;
    }

    public static long startOf(long ts) {
//        batchId &= ~(1 << 20);

        return ts << 20;
    }

    public static long startOfBatch(long ts) {
        return ts << 7;
    }

    public static long endOf(long ts) {
//        batchId |= (1 << (counterBitSize + serverBitSize));

        ts = startOf(ts);
        ts |= (1 << 20);

        return ts;
    }

    public static long endOfBatch(long ts) {
        ts = startOfBatch(ts);
        ts |= (1 << 7);

        return ts;
    }

    //44 - timestamp, 13 - counter, 7 - serverId
    public static long newId() {
        while (true) {
            long now = System.currentTimeMillis() << 13;
            long last = LAST_TIMESTAMP.get();
            if (now > last) {
                if (LAST_TIMESTAMP.compareAndSet(last, now))
                    return (now << 7) | serverId & 0x000000000000007fL;
            } else {
                long lastMillis = last >>> 13;
                if (now >>> 13 < last >>> 13)
                    return (LAST_TIMESTAMP.incrementAndGet() << 7) | serverId & 0x000000000000007fL;

                long candidate = last + 1;
                if (candidate >>> 13 == lastMillis && LAST_TIMESTAMP.compareAndSet(last, candidate))
                    return (candidate << 7) | serverId & 0x000000000000007fL;
            }
        }
    }

    public static long newBatchId() {
        return newBatchId(System.currentTimeMillis());
    }

    public static long newBatchId(long millis) {
        return (millis << 7) | serverId & 0x000000000000007fL;
    }

    public static String external(long id) {
        return Long.toString(Long.reverseBytes(id), 36);
    }

    public static String external(long id, long key) {
        return Long.toString(id ^ key, 36);
    }

    public static long internal(String id) {
        try {
            return Long.reverseBytes(Long.parseLong(id, 36));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static long internal(String id, long key) {
        try {
            return Long.parseLong(id, 36) ^ key;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean validateTime(long time) {
        return time > 1420070400000L && time < 2997993600000L;//2015-2075 years
    }

    public static synchronized int newTransactionId() {
        long time = System.currentTimeMillis() / 100;
        while (lastTime >= time) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }

            time = System.currentTimeMillis() / 100;
        }

        lastTime = time;

        if (time > Integer.MAX_VALUE) {
            time = time - (Integer.MAX_VALUE * (time / Integer.MAX_VALUE));

            if (time == 0) {
                lastTime++;

                return (int) (time + 1);
            }
        }

        return (int) time;
    }
}
