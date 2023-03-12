package group13.channel.perfectLink.events;

import group13.primitives.Address;
import group13.primitives.Event;

public class Pp2pDeliver extends Event {

    public static final String EVENT_NAME = "pp2pDeliver";
    private int process_id;
    private String payload;


    public Pp2pDeliver(int process_id, String payload) {
        super(EVENT_NAME);
        this.process_id = process_id;
        this.payload = payload;
    }

    public int getProcessId() {
        return process_id;
    }

    public String getPayload() {
        return payload;
    }
}
