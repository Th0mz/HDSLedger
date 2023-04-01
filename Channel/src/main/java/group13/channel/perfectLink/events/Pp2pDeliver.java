package group13.channel.perfectLink.events;

import group13.primitives.Event;

import java.security.PublicKey;

public class Pp2pDeliver extends Event {

    public static final String EVENT_NAME = "pp2pDeliver";
    private PublicKey processPK;
    private Object payload;


    public Pp2pDeliver(PublicKey processPK, Object payload) {
        super(EVENT_NAME);
        this.processPK = processPK;
        this.payload = payload;
    }

    public PublicKey getProcessPK() {
        return processPK;
    }

    public Object getPayload() {
        return payload;
    }
}
