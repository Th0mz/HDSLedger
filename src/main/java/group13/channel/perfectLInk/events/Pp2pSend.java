package group13.channel.perfectLInk.events;

import group13.channel.primitives.Address;
import group13.channel.primitives.Event;

public class Pp2pSend extends Event {
    public static final String EVENT_NAME = "pp2pSend";
    private Address destination;
    private String payload;

    public Pp2pSend(Address destination, String payload) {
        super(EVENT_NAME);
        this.destination = destination;
        this.payload = payload;
    }

    public Address getDestination() {
        return destination;
    }

    public String getPayload() {
        return payload;
    }
}
