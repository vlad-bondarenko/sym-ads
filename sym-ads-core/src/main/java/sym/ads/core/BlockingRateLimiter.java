package sym.ads.core;

import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.slf4j.LoggerFactory.getLogger;

public class BlockingRateLimiter {
    private static final Logger logger = getLogger(BlockingRateLimiter.class);
    private static final int RING_SIZE = 60;
    private static final int SAFE_OFFSET = 20;
    private static final long TIMEOUT_MS = 1_000;
    private static final int DELAY = (SAFE_OFFSET / 2) * 1_000;
    private static final ConcurrentHashMap<BlockingRateLimiter, Object> MAP = new ConcurrentHashMap<>();
    private static final Object DUMMY = new Object();

    private static Thread thread;

    private final Semaphore[] ring = new Semaphore[RING_SIZE];
    private int maxRatePerSecond;
    private int delay;

    BlockingRateLimiter(int maxRatePerSecond) {
        this(maxRatePerSecond, 0);
    }

    BlockingRateLimiter(int maxRatePerSecond, int delay) {
        this(maxRatePerSecond, delay, false);
    }

    BlockingRateLimiter(int maxRatePerSecond, int delay, boolean fair) {
        this.maxRatePerSecond = maxRatePerSecond;
        this.delay = delay * 1_000;

        for (int i = 0; i < ring.length; i++) {
            ring[i] = new Semaphore(maxRatePerSecond, fair);
        }

        MAP.put(this, DUMMY);

        init();
    }

    public static void terminate() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    private synchronized void init() {
        if (thread == null) {
            thread = new Thread("BlockingRateLimiter-Cleaner") {
                @Override
                public void run() {
                    do {
                        try {
                            MAP.keySet().forEach(BlockingRateLimiter::clean);
                        } catch (Exception e) {
                            logger.error(e.toString(), e);
                        }

                        try {
                            sleep(DELAY);
                        } catch (InterruptedException e) {
                            break;
                        }
                    } while (true);

                    logger.info("Terminate");
                }
            };

            thread.start();
        }
    }

    void acquireAccess() {
        while (!acquire()) {
            if (delay > 0) {
                LockSupport.parkNanos(delay);
//                sleep(delay);
            }
        }
    }

    private void clean() {
        int activeCell = getActiveRingCell();
        int left = (activeCell - SAFE_OFFSET + RING_SIZE) % RING_SIZE;
        int right = (activeCell + SAFE_OFFSET + 1) % RING_SIZE;

        for (int cell = right; cell != left; cell = nextCellInRing(cell)) {
            reset(cell);
        }
    }

    private boolean acquire() {
        try {
            return tryToAcquire(getActiveRingCell());
        } catch (InterruptedException e) {
            return false;
        }
    }

    private boolean tryToAcquire(int cell) throws InterruptedException {
        Semaphore semaphore = ring[cell];

        return semaphore.tryAcquire(TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private int getActiveRingCell() {
        return (int) ((System.currentTimeMillis() / 1_000) % RING_SIZE);
    }

    private int nextCellInRing(int cell) {
        return (cell + 1) % RING_SIZE;
    }

    private void reset(int cell) {
        Semaphore semaphore = ring[cell];
        semaphore.drainPermits();
        semaphore.release(maxRatePerSecond);
    }
}
