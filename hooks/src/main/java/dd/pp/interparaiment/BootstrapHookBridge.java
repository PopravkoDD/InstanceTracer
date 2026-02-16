package dd.pp.interparaiment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class BootstrapHookBridge {
    private final ConcurrentLinkedQueue<Object> eventQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Object, Object> trackedInstances = new ConcurrentHashMap<>();
    private Consumer<Integer> instanceCreatedNotificator;
    private ExecutorService notificationExecutor = Executors.newSingleThreadExecutor();


    private static BootstrapHookBridge instance;

    private BootstrapHookBridge() {
    }

    public static BootstrapHookBridge getInstance() {
        if (instance == null) {
            instance = new BootstrapHookBridge();
        }

        return instance;
    }

    public void add(final Object event) {
        if (event == null) {
            return;
        }

        eventQueue.add(event);
    }

    public Object poll() {
        return eventQueue.poll();
    }

    public ConcurrentHashMap<Object, Object> getTrackedInstances() {
        return trackedInstances;
    }

    public void setPeerNotificator(final Consumer<Integer> notificator) {
        this.instanceCreatedNotificator = notificator;
    }

    public void notifyInstanceCreated(final int instanceHash) {
        notificationExecutor.execute(() -> instanceCreatedNotificator.accept(instanceHash));
    }
}
