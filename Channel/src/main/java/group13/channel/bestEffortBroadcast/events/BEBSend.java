package group13.channel.bestEffortBroadcast.events;

import group13.primitives.Event;

public class BEBSend extends Event {
    public static final String EVENT_NAME = "bebSend";
    private Object payload;

    public BEBSend(Object payload) {
        super(EVENT_NAME);
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }
}
