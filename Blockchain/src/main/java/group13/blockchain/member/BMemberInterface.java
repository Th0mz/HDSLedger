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

    int _port;
    private BEBroadcast beb;
    private BMember _server;

    public BMemberInterface(int pid, int port, BMember server) {
        _port = port;
        _server = server;
        beb = new BEBroadcast(pid, new Address(port));
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
        _server.tryConsensus(payload);
    }

    public void ackClient(Integer instance, String msg, int pid, int port) { 
        String response = instance + msg;
        BEBSend send_event = new BEBSend(response);
        //TODO:
        // beb.unicast(send_event, 9999, new Address(port));
    }



}
