package group13.channel.bestEffortBroadcast.events;

import group13.primitives.Address;
import group13.primitives.Event;

public class BEBDeliver extends Event {
    public static final String EVENT_NAME = "bebDeliver";
    private Address source;
    private String payload;

    public BEBDeliver (Address source, String payload) {
        super(EVENT_NAME);
        this.source = source;
        this.payload = payload;
    }

    public Address getSource() {
        return source;
    }

    public String getPayload() {
        return payload;
    }
}
