package group13.client;

import group13.primitives.*;
import group13.channel.bestEffortBroadcast.*;
import group13.channel.bestEffortBroadcast.events.*;

import java.util.List;

public class ClientFrontend implements EventListener {

    private BEBroadcast beb;
    //private EventHandler amEventHandler = new EventHandler();

    public ClientFrontend(int pid, int port, List<Address> addresses, List<Integer> pids) {
        beb = new BEBroadcast(pid, new Address(port));
        for(int i = 0; i < addresses.size(); i++)
            beb.addServer(pids.get(i), addresses.get(i));
        beb.subscribeDelivery(this);
        //amEventHandler.subscribe(BEBSend.EVENT_NAME, sender);
    }

    public void sendCommand(String message) {
        BEBSend send_event = new BEBSend(message);
        //amEventHandler.trigger(send_event);
        beb.update(send_event);
    }

    /* when we get responses from servers ?? */
    @Override
    public void update(Event event) {
        String eventType = event.getEventName();
        if (!eventType.equals(BEBDeliver.EVENT_NAME)) {
            System.out.println("Should only receive deliver events (??)");
        }
        BEBDeliver ev = (BEBDeliver) event;
        String payload = ev.getPayload();
    }
    
}
