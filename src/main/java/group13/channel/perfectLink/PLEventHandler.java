package group13.channel.perfectLink;

import group13.channel.primitives.Event;
import group13.channel.primitives.EventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class PLEventHandler {

    private HashMap<String, List<EventListener>> listners = new HashMap<String, List<EventListener>>();
    public void subscribe(String eventType, EventListener listener) {
        if (! this.listners.containsKey(eventType)) {
            this.listners.put(eventType, new ArrayList());
        }

        this.listners.get(eventType).add(listener);
    }

    public void unsubscribe(String eventType, EventListener listener) {
        if (! this.listners.containsKey(eventType)) {
            return;
        }

        this.listners.get(eventType).remove(listener);
    }

    public void trigger(Event event) {

        String eventName = event.getEventName();
        List<EventListener> listeners = this.listners.get(eventName);

        for (EventListener listner : listeners) {
            listner.update(event);
        }
    }
}
