package group13.primitives;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AboveModuleListener implements EventListener {

    private HashMap<String, List<Event>> received_events;

    public AboveModuleListener() {
        this.received_events = new HashMap<>();
    }
    @Override
    public void update(Event event) {
        String eventType = event.getEventName();
        if (! this.received_events.containsKey(eventType)) {
            this.received_events.put(eventType, new ArrayList<>());
        }

        this.received_events.get(eventType).add(event);
    }

    public int get_all_events_num () {
        int result = 0;

        for (String eventType : this.received_events.keySet()) {
            result += this.received_events.get(eventType).size();
        }
        return result;
    }

    public List<Event> get_events(String eventType) {
        if (! received_events.containsKey(eventType)) {
            return new ArrayList<>();
        }

        return received_events.get(eventType);
    }

    public void clean_events () {
        this.received_events = new HashMap<>();
    }
}
