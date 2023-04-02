package group13.channel.bestEffortBroadcast;

import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.bestEffortBroadcast.events.BEBSend;

import group13.channel.perfectLink.Network;
import group13.channel.perfectLink.PerfectLink;
import group13.channel.perfectLink.events.NetworkNew;
import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.primitives.Address;
import group13.primitives.Event;
import group13.primitives.EventHandler;
import group13.primitives.EventListener;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class BEBroadcast implements EventListener {

    // perfect link
    protected List<PerfectLink> links;
    protected Network network;
    private Address inAddress;

    private PublicKey inPublicKey;
    private PrivateKey inPrivateKey;

    private EventHandler bebEventHandler = new EventHandler();

    public BEBroadcast (Address inAddress, PublicKey inPublicKey, PrivateKey inPrivateKey) {

        this.inAddress = inAddress;

        // create network (process that listens for packets)
        this.network = new Network(inAddress);
        this.links = new ArrayList<>();

        this.inPublicKey = inPublicKey;
        this.inPrivateKey = inPrivateKey;

        this.network.subscribeNew(this);
    }

    // handle received events
    public void update(Event event) {
        String eventName = event.getEventName();
        if (eventName == Pp2pDeliver.EVENT_NAME) {
            Pp2pDeliver typed_event = (Pp2pDeliver) event;
            PublicKey processPK = typed_event.getProcessPK();
            Object payload = typed_event.getPayload();

            BEBDeliver triggered_event = new BEBDeliver(processPK, payload);
            bebEventHandler.trigger(triggered_event, EventHandler.Mode.ASYNC);
        } else if (eventName == NetworkNew.EVENT_NAME) {
            NetworkNew typed_event = (NetworkNew) event;
            Address outAddress = typed_event.getOutAddress();
            PublicKey outPublicKey = typed_event.getOutPublicKey();

            addServer(outAddress, outPublicKey);
        }
    }

    public void addServer(Address outAddress, PublicKey outPublicKey) {
        PerfectLink link = this.network.createAuthenticatedLink(outAddress, this.inPublicKey, this.inPrivateKey, outPublicKey);
        link.subscribeDelivery(this);

        this.links.add(link);
    }

    public void send (BEBSend send_event) {
        Object payload = send_event.getPayload();

        for (PerfectLink link : links) {
            link.send(payload);
        }
    }

    public void send_handshake () {
        for (PerfectLink link : links) {
            link.send_handshake();
        }
    }

    public void unicast(PublicKey outProcessPK, BEBSend send_event) {
        Object payload = send_event.getPayload();
        PerfectLink link = this.network.getLink(outProcessPK);

        if (link != null) {
            link.send(payload);
        }
    }

    public Address getInAddress() {
        return inAddress;
    }

    public List<Address> getAllAddresses () {

        List<Address> addresses = new ArrayList<>();
        for (PerfectLink link : this.links) {
            Address outAddress = link.getOutAddress();
            addresses.add(outAddress);
        }

        return addresses;
    }

    public void subscribeDelivery(EventListener listener) {
        bebEventHandler.subscribe(BEBDeliver.EVENT_NAME, listener);
    }

    public void unsubscribeDelivery(EventListener listener) {
        bebEventHandler.unsubscribe(BEBDeliver.EVENT_NAME, listener);
    }

    public void close () {
        this.network.close();
    }
}
