package dd.pp.interparaiment.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
    private final Map<Class<?>, CopyOnWriteArrayList<IListener<?>>> listenersMap = new ConcurrentHashMap<>();

    public <TEventData> Subscription subscribe(Class<TEventData> event, IListener<TEventData> listener) {
        CopyOnWriteArrayList<IListener<?>> listeners = listenersMap.computeIfAbsent(
                event, ignored -> new CopyOnWriteArrayList<>());
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @SuppressWarnings("unchecked")
    public <TEventData> void notify(TEventData event) {
        CopyOnWriteArrayList<IListener<?>> listeners = listenersMap.get(event.getClass());
        if (listeners != null) {
            for (IListener<?> listener : listeners) {
                ((IListener<TEventData>) listener).accept(event);
            }
        }
    }

    @FunctionalInterface
    public interface Subscription extends AutoCloseable {
        @Override
        void close();
    }
}
