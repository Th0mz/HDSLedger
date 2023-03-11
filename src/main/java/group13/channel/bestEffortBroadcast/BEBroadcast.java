package group13.channel.bestEffortBroadcast;

import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.bestEffortBroadcast.events.BEBSend;

import group13.channel.perfectLink.PerfectLinkIn;
import group13.channel.perfectLink.PerfectLinkOut;
import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.channel.perfectLink.events.Pp2pSend;
import group13.primitives.Address;
import group13.primitives.Event;
import group13.primitives.EventHandler;
import group13.primitives.EventListener;

import java.util.ArrayList;
import java.util.List;

public class BEBroadcast implements EventListener {

    // perfect link
    private PerfectLinkIn in_link;
    private List<PerfectLinkOut> out_links;

    private int processId;
    private Address address;
    private EventHandler bebEventHandler = new EventHandler();

    public BEBroadcast (int processId, Address address) {

        // create perfect link
        this.in_link = new PerfectLinkIn(address);
        this.in_link.subscribeDelivery(this);
        this.out_links = new ArrayList<>();

        this.address = address;
        this.processId = processId;
    }

    // handle received events
    public void update(Event event) {
        String eventName = event.getEventName();

        if (eventName == BEBSend.EVENT_NAME) {
            BEBSend typed_event = (BEBSend) event;
            String payload = typed_event.getPayload();

            Pp2pSend triggered_event = new Pp2pSend(payload);
            bebEventHandler.trigger(triggered_event);

        } else if (eventName == Pp2pDeliver.EVENT_NAME) {
            Pp2pDeliver typed_event = (Pp2pDeliver) event;
            Address address = typed_event.getSource();
            String payload = typed_event.getPayload();

            BEBDeliver triggered_event = new BEBDeliver(address, payload);
            bebEventHandler.trigger(triggered_event);
        }
    }

    public void addServers(List<Address> addresses) {
        for (Address destination : addresses) {
            PerfectLinkOut out_link = new PerfectLinkOut(this.processId, destination);

            this.out_links.add(out_link);
            this.subscribeSend(out_link);
        }
    }

    public Address getAddress() {
        return address;
    }

    public void removeServer(Address destination) {
        PerfectLinkOut out_link = new PerfectLinkOut(this.processId, destination);

        this.out_links.remove(out_link);
        this.unsubscribeSend(out_link);
    }

    public void subscribeDelivery(EventListener listener) {
        bebEventHandler.subscribe(BEBDeliver.EVENT_NAME, listener);
    }

    public void unsubscribeDelivery(EventListener listener) {
        bebEventHandler.unsubscribe(BEBDeliver.EVENT_NAME, listener);
    }

    public void close () {
        this.in_link.interrupt();
        this.in_link.close();
        for (PerfectLinkOut out_link : this.out_links) {
            out_link.close();
        }
    }

    public void subscribeSend(EventListener listener) {
        bebEventHandler.subscribe(Pp2pSend.EVENT_NAME, listener);
    }

    public void unsubscribeSend(EventListener listener) {
        bebEventHandler.unsubscribe(Pp2pSend.EVENT_NAME, listener);
    }


}
