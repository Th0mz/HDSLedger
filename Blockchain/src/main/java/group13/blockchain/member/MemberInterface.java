package group13.blockchain.member;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.bestEffortBroadcast.events.BEBSend;
import group13.channel.perfectLink.PerfectLinkOut;
import group13.primitives.Address;
import group13.primitives.Event;
import group13.primitives.EventListener;

public class MemberInterface implements EventListener {

    int _port;
    private BEBroadcast beb;

    public MemberInterface(int pid, int port) {
        _port = port;
        beb = new BEBroadcast(pid, new Address(port)); //TODO: PORT??
        beb.subscribeDelivery(this);
        System.out.println("Started process " + _port );
    }

    @Override
    public void update(Event event) {
        String eventType = event.getEventName();
        if (!eventType.equals(BEBDeliver.EVENT_NAME)) {
            System.out.println("Should only receive deliver events (??)");
        }
        BEBDeliver ev = (BEBDeliver) event;
        String payload = ev.getPayload();
        System.out.println(payload);
        
        String response = "MESSAGE RECEIVED";
        BEBSend send_event = new BEBSend(response);
        beb.unicast(send_event, 9999, new Address(9876));
    }



}
