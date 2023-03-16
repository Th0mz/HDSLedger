package group13.channel.bestEffortBroadcast.events;

import group13.primitives.Event;

public class BEBDeliver extends Event {
    public static final String EVENT_NAME = "bebDeliver";
    private int process_id;
    private byte[] payload;
    private int port;

    public BEBDeliver (int process_id, byte[] payload, int port) {
        super(EVENT_NAME);
        this.process_id = process_id;
        this.payload = payload;
        this.port = port;
    }

    public int getProcessID() {
        return this.process_id;
    }

    public int getPort() {
        return this.port;
    }

    public byte[] getPayload() {
        return payload;
    }
}
