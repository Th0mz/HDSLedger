package group13.channel.perfectLink.events;

import group13.channel.primitives.Address;
import group13.channel.primitives.Event;

public class Pp2pDeliver extends Event {

    public static final String EVENT_NAME = "pp2pDeliver";
    private Address source;
    private String payload;


    public Pp2pDeliver(Address source, String payload) {
        super(EVENT_NAME);
        this.source = source;
        this.payload = payload;
    }

    public Address getSource() {
        return source;
    }

    public String getPayload() {
        return payload;
    }
}
