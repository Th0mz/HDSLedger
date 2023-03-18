package group13.blockchain.member;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import group13.blockchain.consensus.IBFT;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.bestEffortBroadcast.events.BEBSend;
import group13.channel.perfectLink.PerfectLinkOut;
import group13.primitives.Address;
import group13.primitives.Event;
import group13.primitives.EventListener;

public class BMemberInterface implements EventListener {

    private BEBroadcast beb;
    private BMember _server;

    public BMemberInterface(Address memberAddress, BMember server) {
        _server = server;
        beb = new BEBroadcast(memberAddress);
        beb.subscribeDelivery(this);

        //System.out.println("Started process " +  memberAddress.getPort() );
    }

    @Override
    public void update(Event event) {
        String eventType = event.getEventName();
        if (!eventType.equals(BEBDeliver.EVENT_NAME)) {
            System.out.println("Should only receive deliver events (??)");
        }


        BEBDeliver ev = (BEBDeliver) event;
        String clientId = ev.getProcessId();

        byte[] payload = ev.getPayload();
        //String payloadString = new String(payload);
        _server.tryConsensus(payload, clientId);
    }

    public void ackClient(Integer instance, String msg, String clientId) {
        String response = instance + msg;
        BEBSend send_event = new BEBSend(response.getBytes());

        beb.unicast(clientId, send_event);
    }
}
