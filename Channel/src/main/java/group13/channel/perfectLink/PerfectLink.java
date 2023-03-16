package group13.channel.perfectLink;

import group13.primitives.Address;
import group13.primitives.EventListener;

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
                    int sequenceNumber = entry.getKey();
                    byte[] payload = entry.getValue();

                    retransmit(payload, sequenceNumber);
                }

            }
        }

        this.timer = new Timer();
        this.retransmitTask = new RetransmitPacketsTask();
        this.timer.scheduleAtFixedRate(this.retransmitTask, this.RETRANSMIT_DELTA, this.RETRANSMIT_DELTA);
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
        this.sentMessages.remove(sequenceNumber);
    }
    public void subscribeDelivery(EventListener listener) {
        this.inLink.subscribeDelivery(listener);
    }

    public void unsubscribeDelivery(EventListener listener) {
        this.inLink.unsubscribeDelivery(listener);
    }

    public void close () {
        this.retransmitTask.cancel();
        this.timer.cancel();
        this.outLink.close();
    }
}
