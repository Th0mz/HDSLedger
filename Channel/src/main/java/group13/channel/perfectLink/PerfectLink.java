package group13.channel.perfectLink;

import group13.primitives.Address;
import group13.primitives.EventListener;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class PerfectLink {

    public static int HEADER_SIZE = 9;
    public static int RETRANSMIT_DELTA = 300;

    private PerfectLinkIn inLink;
    private PerfectLinkOut outLink;
    private int outProcessId;
    private Address outAddress;

    protected TreeMap<Integer, byte[]> sentMessages;



    public PerfectLink(int inPoressId, Address inAddress, int outProcessId, Address outAddress) {
        this.outProcessId = outProcessId;
        this.outAddress = outAddress;

        this.inLink = new PerfectLinkIn(this, inPoressId);
        this.outLink = new PerfectLinkOut(this, inPoressId, outAddress);

        this.sentMessages = new TreeMap<>();

        // retransmit system
        class RetransmitPacketsTask extends TimerTask {

            @Override
            public void run() {
                // DEBUG :
                System.out.println("[pid = " + inPoressId + "] retransmitting : " + sentMessages);
                for (Map.Entry<Integer, byte[]> entry : sentMessages.entrySet()) {
                    int sequenceNumber = entry.getKey();
                    byte[] payload = entry.getValue();

                    retransmit(payload, sequenceNumber);
                }

            }
        }

        Timer time = new Timer();
        RetransmitPacketsTask task = new RetransmitPacketsTask();
        time.scheduleAtFixedRate(task, this.RETRANSMIT_DELTA, this.RETRANSMIT_DELTA);
    }

    public void receive (byte[] packetData, int packetLength, int packetPort) {
        this.inLink.receive(packetData, packetLength, packetPort);
    }

    public void send(byte[] payload) {
        this.outLink.send(payload);
    }

    public void send_ack(int sequenceNumber) {
        this.outLink.send_ack(sequenceNumber);
    }

    public void retransmit (byte[] payload, int sequenceNumber) {
        this.outLink.retransmit(payload, sequenceNumber);
    }

    public void packet_send(int sequenceNumber, byte[] payload) {
        this.sentMessages.put(sequenceNumber, payload);
    }

    public void received_ack(int sequenceNumber) {
        if (this.sentMessages.containsKey(sequenceNumber)) {
            this.sentMessages.remove(sequenceNumber);
        }
    }
    public void subscribeDelivery(EventListener listener) {
        this.inLink.subscribeDelivery(listener);
    }

    public void unsubscribeDelivery(EventListener listener) {
        this.inLink.unsubscribeDelivery(listener);
    }
    public void close () {
        this.outLink.close();
    }
}
