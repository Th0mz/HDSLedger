package group13.channel.perfectLink;

import group13.primitives.Address;
import group13.primitives.EventListener;
import group13.primitives.NetworkMessage;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class PerfectLink {

    public static int PROCESS_ID_SIZE = 32;
    public static int SEQUENCE_NUMBER_SIZE = 4;
    public static int MESSAGE_TYPE_SIZE = 1;

    public static int HEADER_SIZE = PROCESS_ID_SIZE + SEQUENCE_NUMBER_SIZE + MESSAGE_TYPE_SIZE;
    public static int RETRANSMIT_DELTA = 300;


    private PerfectLinkIn inLink;
    private PerfectLinkOut outLink;

    private Address inAddress;
    private Address outAddress;

    private Timer timer;
    private TimerTask retransmitTask;

    protected TreeMap<Integer, byte[]> sentMessages;

    private int backoff = 0;



    public PerfectLink(Address inAddress, Address outAddress) {
        this.outAddress = outAddress;

        this.inLink = new PerfectLinkIn(this, inAddress);
        this.outLink = new PerfectLinkOut(this, inAddress, outAddress);

        this.sentMessages = new TreeMap<>();

        // retransmit system
        class RetransmitPacketsTask extends TimerTask {

            @Override
            public void run() {

                for (Map.Entry<Integer, byte[]> entry : sentMessages.entrySet()) {
                    byte[] payload = entry.getValue();
                    retransmit(payload);
                }

            }
        }

        this.timer = new Timer();
        this.retransmitTask = new RetransmitPacketsTask();
        this.timer.scheduleAtFixedRate(retransmitTask, RETRANSMIT_DELTA, RETRANSMIT_DELTA);
    }

    public void receive (NetworkMessage message) {
        this.inLink.receive(message);
    }

    public void send(Object payload) {
        this.outLink.send(payload);
    }

    public void send_ack(int sequenceNumber) {
        this.outLink.send_ack(sequenceNumber);
    }

    public void send_handshake () {
        this.outLink.send_handshake();
    }

    public void retransmit (byte[] payload) {
        this.outLink.retransmit(payload);
    }

    public void packet_send(int sequenceNumber, byte[] payload) {
        this.sentMessages.put(sequenceNumber, payload);
    }

    public void received_ack(int sequenceNumber) {
        this.sentMessages.remove(sequenceNumber);
    }
    public void subscribeDelivery(EventListener listener) {
        this.inLink.subscribeDelivery(listener);
    }

    public void unsubscribeDelivery(EventListener listener) {
        this.inLink.unsubscribeDelivery(listener);
    }

    public Address getOutAddress() { return outAddress; }

    public void close () {
        this.retransmitTask.cancel();
        this.timer.cancel();
        this.outLink.close();
    }
}
