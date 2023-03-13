package group13.channel.perfectLink.events;

import group13.primitives.Event;

public class Pp2pSend extends Event {
    public static final String EVENT_NAME = "pp2pSend";
    private String payload;

    public Pp2pSend(String payload) {
        super(EVENT_NAME);
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}
