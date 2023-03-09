package group13.channel.bestEffortBroadcast;

import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.bestEffortBroadcast.events.BEBSend;

import group13.channel.perfectLink.PerfectLinkIn;
import group13.channel.perfectLink.PerfectLinkOut;
import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.channel.perfectLink.events.Pp2pSend;
import group13.channel.primitives.Address;
import group13.channel.primitives.Event;
import group13.channel.primitives.EventListener;

public class BEBroadcast implements EventListener {

    private BEBEventHandler eventHandler = new BEBEventHandler();

    public BEBroadcast (int processId, Address address) {
        // create perfect link
        PerfectLinkIn in_link = new PerfectLinkIn(processId, address);
        in_link.subscribeDelivery(this);
    }

    // handle received events
    public void update(Event event) {
        String eventName = event.getEventName();

        if (eventName == BEBSend.EVENT_NAME) {
            BEBSend typed_event = (BEBSend) event;
            String payload = typed_event.getPayload();

            Pp2pSend triggered_event = new Pp2pSend(payload);
            eventHandler.trigger(triggered_event);

        } else if (eventName == Pp2pDeliver.EVENT_NAME) {
            Pp2pDeliver typed_event = (Pp2pDeliver) event;
            Address address = typed_event.getSource();
            String payload = typed_event.getPayload();

            BEBDeliver triggered_event = new BEBDeliver(address, payload);
            eventHandler.trigger(triggered_event);
        }
    }

    public void addServer(Address destination) {
        PerfectLinkOut out_link = new PerfectLinkOut(destination);
        this.subscribeSend(out_link);
    }

    public void removeServer(Address destination) {
        PerfectLinkOut out_link = new PerfectLinkOut(destination);
        this.unsubscribeSend(out_link);
    }

    public void subscribeDelivery(EventListener listener) {
        eventHandler.subscribe(BEBDeliver.EVENT_NAME, listener);
    }

    public void unsubscribeDelivery(EventListener listener) {
        eventHandler.unsubscribe(BEBDeliver.EVENT_NAME, listener);
    }

    public void subscribeSend(EventListener listener) {
        eventHandler.subscribe(Pp2pSend.EVENT_NAME, listener);
    }

    public void unsubscribeSend(EventListener listener) {
        eventHandler.unsubscribe(Pp2pSend.EVENT_NAME, listener);
    }
}
