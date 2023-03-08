package group13.channel.bestEffortBroadcast;

import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.bestEffortBroadcast.events.BEBSend;
import group13.channel.perfectLInk.PerfectLink;
import group13.channel.perfectLInk.events.Pp2pDeliver;
import group13.channel.perfectLInk.events.Pp2pSend;
import group13.channel.primitives.Address;
import group13.channel.primitives.Event;
import group13.channel.primitives.EventListener;

import java.util.ArrayList;
import java.util.List;

public class BEBroadcast implements EventListener {

    private BEBEventHandler eventHandler = new BEBEventHandler();
    private List<Address> servers = new ArrayList<>();

    public BEBroadcast (int port, int processId) {
        // create perfect link
        // TODO : what to do if the send method raises error?
        //   - what must happen for the send method to raise an error?
        PerfectLink link;
        try {
            link = new PerfectLink(port, processId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        link.subscribeDelivery(this);
        this.subscribeSend(link);
    }

    // handle received events
    public void update(Event event) {
        String eventName = event.getEventName();
        if (eventName == BEBSend.EVENT_NAME) {
            BEBSend typed_event = (BEBSend) event;
            String payload = typed_event.getPayload();

            for (Address server : this.servers) {
                Pp2pSend triggered_event = new Pp2pSend(server, payload);
                eventHandler.trigger(triggered_event);
            }

        } else if (eventName == Pp2pDeliver.EVENT_NAME) {
            Pp2pDeliver typed_event = (Pp2pDeliver) event;
            Address address = typed_event.getSource();
            String payload = typed_event.getPayload();

            BEBDeliver triggered_event = new BEBDeliver(address, payload);
            eventHandler.trigger(triggered_event);
        }
    }

    public void addServer(Address server) {
        this.servers.add(server);
    }

    public boolean removeServer(Address server) {
        return this.servers.remove(server);
    }

    public void subscribeDelivery(EventListener listener) {
        eventHandler.subscribe(BEBDeliver.EVENT_NAME, listener);
    }

    public void subscribeSend(EventListener listener) {
        eventHandler.subscribe(Pp2pSend.EVENT_NAME, listener);

    }
}
