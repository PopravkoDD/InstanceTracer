package dd.pp.interparaiment.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EventManager {
    private final Map<Class<?>, List<IListener<?>>> listenersMap;

    public EventManager() {
        this.listenersMap = new TreeMap<>(new IdentityComparator<>());
    }

    public <TEventData> void subscribe(Class<TEventData> event, final IListener<TEventData> listener) {
        this.getListeners(event).add(listener);
    }

    public <TEventData> void notify(TEventData event) {
        this.getListeners(event.getClass()).forEach(listener -> {
            ((IListener<TEventData>) listener).accept(event);
        });
    }

    private <TEventData> List<IListener<?>> getListeners(Class<TEventData> eventType) {
        return this.listenersMap.computeIfAbsent(eventType, listeners -> new ArrayList<>());
    }

    private static class IdentityComparator<TItem> implements Comparator<TItem> {
        @Override
        public int compare(TItem o1, TItem o2) {
            return Integer.compare(System.identityHashCode(o1), System.identityHashCode(o2));
        }

    }
}
