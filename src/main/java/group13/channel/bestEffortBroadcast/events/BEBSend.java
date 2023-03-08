package group13.channel.bestEffortBroadcast.events;

import group13.channel.primitives.Address;
import group13.channel.primitives.Event;

public class BEBSend extends Event {
    public static final String EVENT_NAME = "bebSend";
    private String payload;

    public BEBSend(Address destination, String payload) {
        super(EVENT_NAME);
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}
