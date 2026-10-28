package dd.pp.interparaiment.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
    private final Map<Class<?>, List<IListener<?>>> listenersMap;

    public EventManager() {
        this.listenersMap = new ConcurrentHashMap<>();
    }

    public <TEventData> void subscribe(Class<TEventData> event, final IListener<TEventData> listener) {
        this.getListeners(event).add(listener);
    }

    public <TEventData> void unsubscribe(Class<TEventData> event, final IListener<TEventData> listener) {
        this.getListeners(event).remove(listener);
    }

    public <TEventData> void notify(TEventData event) {
        this.getListeners(event.getClass()).forEach(listener -> {
            ((IListener<TEventData>) listener).accept(event);
        });
    }

    private <TEventData> List<IListener<?>> getListeners(Class<TEventData> eventType) {
        return this.listenersMap.computeIfAbsent(eventType, listeners -> new CopyOnWriteArrayList<>());
    }
}
