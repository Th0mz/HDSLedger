package group13.channel.perfectLink.events;

import group13.primitives.Event;

public class Pp2pDeliver extends Event {

    public static final String EVENT_NAME = "pp2pDeliver";
    private String processId;
    private Object payload;


    public Pp2pDeliver(String processId, Object payload) {
        super(EVENT_NAME);
        this.processId = processId;
        this.payload = payload;
    }

    public String getProcessId() {
        return processId;
    }

    public Object getPayload() {
        return payload;
    }
}
