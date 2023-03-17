package group13.client;

import group13.primitives.*;
import group13.channel.bestEffortBroadcast.*;
import group13.channel.bestEffortBroadcast.events.*;
import group13.channel.perfectLink.PerfectLinkIn;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class ClientFrontend implements EventListener {

    private BEBroadcast beb;

    public ClientFrontend(Address inAddress, List<Address> addresses) {
        System.out.println("Client on port " + inAddress.toString());
        beb = new BEBroadcast(inAddress);
        for(int i = 0; i < addresses.size(); i++)
            beb.addServer(addresses.get(i));

        beb.send_handshake();
        beb.subscribeDelivery(this);
    }

    public void sendCommand(String message) {
        BEBSend send_event = new BEBSend(message.getBytes());
        beb.send(send_event);
    }

    /* when we get responses from servers */
    @Override
    public void update(Event event) {
        String eventType = event.getEventName();

        if (!eventType.equals(BEBDeliver.EVENT_NAME)) {
            System.out.println("Should only receive deliver events (??)");
        }
        BEBDeliver ev = (BEBDeliver) event;
        
        byte[] byte_stream = ev.getPayload();
        String payload = new String(byte_stream, StandardCharsets.UTF_8);
        
        System.out.println("CONSENSUS RESULT: ");
        System.out.println(payload);
    }
}
