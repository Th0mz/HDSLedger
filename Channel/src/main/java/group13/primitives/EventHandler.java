package group13.primitives;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EventHandler {
    private HashMap<String, List<EventListener>> listeners = new HashMap<>();

    public void subscribe(String eventType, EventListener listener) {
        if (! this.listeners.containsKey(eventType)) {
            this.listeners.put(eventType, new ArrayList());
        }

        this.listeners.get(eventType).add(listener);
    }

    public void unsubscribe(String eventType, EventListener listener) {
        if (! this.listeners.containsKey(eventType)) {
            return;
        }

        this.listeners.get(eventType).remove(listener);
    }

    public void trigger(Event event) {

        String eventType = event.getEventName();
        if (! this.listeners.containsKey(eventType)) {
            return;
        }

        String eventName = event.getEventName();
        List<EventListener> listeners = this.listeners.get(eventName);

        for (EventListener listener : listeners) {
            listener.update(event);
        }
    }
}
