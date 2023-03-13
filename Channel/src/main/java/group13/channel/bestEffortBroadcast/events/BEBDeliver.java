package group13.channel.bestEffortBroadcast.events;

import group13.primitives.Address;
import group13.primitives.Event;

public class BEBDeliver extends Event {
    public static final String EVENT_NAME = "bebDeliver";
    private int process_id;
    private String payload;

    public BEBDeliver (int process_id, String payload) {
        super(EVENT_NAME);
        this.process_id = process_id;
        this.payload = payload;
    }

    public int getProcessID() {
        return this.process_id;
    }

    public String getPayload() {
        return payload;
    }
}
