package dd.pp.interparaiment;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BootstrapHookBridge {
    private final ConcurrentLinkedQueue<Object> eventQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Object, Object> scannedInstances = new ConcurrentHashMap<>();


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

    public ConcurrentHashMap<Object, Object> getScannedInstances() {
        return scannedInstances;
    }
}
