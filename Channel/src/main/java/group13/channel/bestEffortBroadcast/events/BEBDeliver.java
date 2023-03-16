package group13.channel.bestEffortBroadcast.events;

import group13.primitives.Event;

public class BEBDeliver extends Event {
    public static final String EVENT_NAME = "bebDeliver";
    private String processId;
    private byte[] payload;
    private int port;

    public BEBDeliver (String processId, byte[] payload, int port) {
        super(EVENT_NAME);
        this.processId = processId;
        this.payload = payload;
        this.port = port;
    }

    public String getProcessId() {
        return this.processId;
    }

    public int getPort() {
        return this.port;
    }

    public byte[] getPayload() {
        return payload;
    }
}
