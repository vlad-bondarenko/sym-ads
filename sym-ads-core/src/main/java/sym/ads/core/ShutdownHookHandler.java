package sym.ads.core;

import java.util.ArrayList;

/**
 * bondarenko.vlad@gmail.com
 * Created by vbondarenko on 01.10.17.
 */
public final class ShutdownHookHandler extends BaseClass {

    private static final ArrayList<Runnable> SHUTDOWN_HOOKS = new ArrayList<>();
    private static final ShutdownHookHandler ourInstance = new ShutdownHookHandler();

    private ShutdownHookHandler() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Exec shutdown hooks...");

            for (Runnable shutdownHook : SHUTDOWN_HOOKS) {
                try {
                    shutdownHook.run();
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }
            }
        }));
    }

    public static ShutdownHookHandler getInstance() {
        return ourInstance;
    }

    public void addShutdownHook(Runnable task) {
        addShutdownHook(task, false);
    }

    public synchronized void addShutdownHook(Runnable task, boolean isHead) {
        if (isHead) {
            SHUTDOWN_HOOKS.add(0, task);
        } else {
            SHUTDOWN_HOOKS.add(task);
        }
    }

    public synchronized void removeShutdownHook(Runnable task) {
        SHUTDOWN_HOOKS.remove(task);
    }
}
