package group13.channel.bestEffortBroadcast;

import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.bestEffortBroadcast.events.BEBSend;

import group13.channel.perfectLink.Network;
import group13.channel.perfectLink.PerfectLink;
import group13.channel.perfectLink.PerfectLinkOut;
import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.primitives.Address;
import group13.primitives.Event;
import group13.primitives.EventHandler;
import group13.primitives.EventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BEBroadcast implements EventListener {

    // perfect link
    private List<PerfectLink> links;
    private Network network;

    private int inProcessId;
    private Address inAddress;
    private EventHandler bebEventHandler = new EventHandler();

    public BEBroadcast (int inProcessId, Address inAddress) {

        this.inAddress = inAddress;
        this.inProcessId = inProcessId;

        // create network (process that listens for packets)
        this.network = new Network(inProcessId, inAddress);
        this.links = new ArrayList<>();
    }

    // handle received events
    public void update(Event event) {
        String eventName = event.getEventName();
        System.out.println(eventName);
        if (eventName == Pp2pDeliver.EVENT_NAME) {
            Pp2pDeliver typed_event = (Pp2pDeliver) event;
            int process_id = typed_event.getProcessId();
            byte[] payload = typed_event.getPayload();

            BEBDeliver triggered_event = new BEBDeliver(process_id, payload, typed_event.getPort());
            bebEventHandler.trigger(triggered_event);
        }
    }

    public void addServer(int outProcessId, Address outAddress) {
        PerfectLink link = this.network.createLink(outProcessId, outAddress);
        link.subscribeDelivery(this);

        this.links.add(link);
    }

    public void send (BEBSend send_event) {
        byte[] payload = send_event.getPayload();

        for (PerfectLink link : links) {
            link.send(payload);
        }
    }

    /*
    public void unicast(BEBSend send_event, int outProcessId, Address destination) {
        byte[] payload = send_event.getPayload().getBytes();
        HashMap<Integer, PerfectLinkOut> links = this.links.getOutLinks();
        System.out.println("Unicast");
        PerfectLinkOut out_link;
        
        if (!links.containsKey(processId)){
            out_link = new PerfectLinkOut(processId, destination);
        } else {
            out_link = links.get(processId);
        }
            
        out_link.send(payload);
    }

     */

    public Address getInAddress() {
        return inAddress;
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
