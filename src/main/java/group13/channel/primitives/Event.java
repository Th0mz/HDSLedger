package group13.channel.primitives;

public class Event {

    private String eventName;

    public Event (String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}