package group13.channel.perfectLink;

import group13.primitives.Address;
import group13.primitives.EventListener;
import group13.primitives.NetworkMessage;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PerfectLink {

    public static int RETRANSMIT_DELTA = 200;

    public static int RETRANSMIT_RATE = 3;



    private PerfectLinkIn inLink;
    private PerfectLinkOut outLink;

    private Address inAddress;
    private Address outAddress;

    protected ConcurrentHashMap<Integer, RetransmitTask> retransmitTasks;
    protected Random randomGenerator = new Random();
    private Timer timer;


    public PerfectLink(Address inAddress, Address outAddress, PublicKey inPublicKey, PrivateKey inPrivateKey, PublicKey outPublicKey) {
        this.inAddress = inAddress;
        this.outAddress = outAddress;

        this.inLink = new PerfectLinkIn(this, inAddress, outPublicKey);
        this.outLink = new PerfectLinkOut(this, inAddress, outAddress, inPublicKey, inPrivateKey);

        this.retransmitTasks = new ConcurrentHashMap<>();
        this.timer = new Timer();

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
        RetransmitTask task = new RetransmitTask(sequenceNumber, payload);
        this.retransmitTasks.put(sequenceNumber, task);
    }

    public void received_ack(int sequenceNumber) {
        RetransmitTask task =  this.retransmitTasks.remove(sequenceNumber);

        if (task != null) {
            task.terminate();
        }
    }
    public void subscribeDelivery(EventListener listener) {
        this.inLink.subscribeDelivery(listener);
    }

    public void unsubscribeDelivery(EventListener listener) {
        this.inLink.unsubscribeDelivery(listener);
    }

    public Address getOutAddress() { return outAddress; }

    public void close () {

        for (int sequenceNumber : this.retransmitTasks.keySet()) {
            RetransmitTask task = this.retransmitTasks.get(sequenceNumber);
            task.terminate();
        }
        this.outLink.close();
    }


    // retransmit system
    private class RetransmitTask extends TimerTask {

        private int sequenceNumber;
        private boolean received = false;
        private byte[] payload;
        private int tries = 0;

        public RetransmitTask(int sequenceNumber, byte[] payload) {
            this.sequenceNumber = sequenceNumber;
            this.payload = payload;

            // schedule task
            timer.schedule(this, RETRANSMIT_DELTA);
        }

        public RetransmitTask(int sequenceNumber, byte[] payload, int tries) {
            this.sequenceNumber = sequenceNumber;
            this.payload = payload;
            this.tries = tries;

            // schedule task
            int delay = RETRANSMIT_DELTA + randomGenerator.nextInt(1 << (this.tries + RETRANSMIT_RATE));
            timer.schedule(this, delay);
        }

        @Override
        public void run() {

            if (!this.received) {
                retransmit(this.payload);
                this.tries++;

                // reschedule task
                RetransmitTask newTask = new RetransmitTask(this.sequenceNumber, this.payload, this.tries);
                retransmitTasks.put(sequenceNumber, newTask);

                this.cancel();
            }
        }

        public void terminate () {
            this.received = true;
            this.cancel();
        }
    }
}
