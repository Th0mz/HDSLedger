package group13.channel.bestEffortBroadcast.events;

import group13.primitives.Event;

public class BEBSend extends Event {
    public static final String EVENT_NAME = "bebSend";
    private byte[] payload;

    public BEBSend(byte[] payload) {
        super(EVENT_NAME);
        this.payload = payload;
    }

    public byte[] getPayload() {
        return payload;
    }
}
