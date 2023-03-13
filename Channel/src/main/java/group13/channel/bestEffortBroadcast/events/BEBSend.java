package group13.channel.bestEffortBroadcast.events;

import group13.primitives.Event;

public class BEBSend extends Event {
    public static final String EVENT_NAME = "bebSend";
    private String payload;

    public BEBSend(String payload) {
        super(EVENT_NAME);
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}
