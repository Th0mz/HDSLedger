package group13.primitives;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EventHandler {

    public enum Mode {
        ASYNC,
        SYNC
    }
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


    public void trigger(Event event, Mode triggerMode) {

        String eventType = event.getEventName();
        if (! this.listeners.containsKey(eventType)) {
            return;
        }

        String eventName = event.getEventName();
        List<EventListener> listeners = this.listeners.get(eventName);

        for (EventListener listener : listeners) {
            if (Mode.ASYNC.equals(triggerMode)) {
                Thread thread = new Thread() {
                    @Override
                    public void run () {
                        listener.update(event);
                    }
                };
                thread.start();
            } else if (Mode.SYNC.equals(triggerMode)) {
                listener.update(event);
            } else {
                System.err.println("Error : Unknown trigger mode");
            }

        }
    }
}
