package group13.channel.bestEffortBroadcast;

import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.bestEffortBroadcast.events.BEBSend;

import group13.channel.perfectLink.PerfectLink;
import group13.channel.perfectLink.PerfectLinkOut;
import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.primitives.Address;
import group13.primitives.Event;
import group13.primitives.EventHandler;
import group13.primitives.EventListener;

import java.util.HashMap;

public class BEBroadcast implements EventListener {

    // perfect link
    private PerfectLink link;

    private int processId;
    private Address address;
    private EventHandler bebEventHandler = new EventHandler();

    public BEBroadcast (int processId, Address address) {

        // create perfect link
        this.link = new PerfectLink(processId, address);
        this.link.subscribeDelivery(this);

        this.address = address;
        this.processId = processId;
    }

    // handle received events
    public void update(Event event) {
        String eventName = event.getEventName();

        if (eventName == Pp2pDeliver.EVENT_NAME) {
            Pp2pDeliver typed_event = (Pp2pDeliver) event;
            int process_id = typed_event.getProcessId();
            String payload = typed_event.getPayload();

            BEBDeliver triggered_event = new BEBDeliver(process_id, payload);
            bebEventHandler.trigger(triggered_event);
        }
    }

    public void addServer(int processId, Address destination) {
        PerfectLinkOut out_link = this.link.createLink(processId, destination);
    }

    public void removeServer(Address destination) {
        PerfectLinkOut out_link = this.link.removeLink(destination);
    }

    public void send (BEBSend send_event) {
        byte[] payload = send_event.getPayload().getBytes();
        HashMap<Integer, PerfectLinkOut> links = this.link.getOutLinks();

        for (int processId : links.keySet()) {
            PerfectLinkOut link = links.get(processId);
            link.send(payload);
        }
    }

    public Address getAddress() {
        return address;
    }

    public void subscribeDelivery(EventListener listener) {
        bebEventHandler.subscribe(BEBDeliver.EVENT_NAME, listener);
    }

    public void unsubscribeDelivery(EventListener listener) {
        bebEventHandler.unsubscribe(BEBDeliver.EVENT_NAME, listener);
    }

    public void close () {
        this.link.close();
    }
}
