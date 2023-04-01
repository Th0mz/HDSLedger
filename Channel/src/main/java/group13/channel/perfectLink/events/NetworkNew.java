package group13.channel.perfectLink.events;

import group13.channel.perfectLink.PerfectLink;
import group13.primitives.Address;
import group13.primitives.Event;

import java.security.PublicKey;

public class NetworkNew extends Event {

    public static final String EVENT_NAME = "networkNew";

    private Address outAddress;
    private PublicKey outPublicKey;


    public NetworkNew (Address outAddress, PublicKey outPublicKey) {
        super(EVENT_NAME);
        this.outAddress = outAddress;
        this.outPublicKey = outPublicKey;
    }

    public PublicKey getOutPublicKey() {
        return outPublicKey;
    }

    public Address getOutAddress() {
        return outAddress;
    }
}
