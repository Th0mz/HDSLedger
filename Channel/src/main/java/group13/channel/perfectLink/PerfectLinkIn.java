package group13.channel.perfectLink;

import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.primitives.Address;
import group13.primitives.EventHandler;
import group13.primitives.EventListener;
import group13.primitives.NetworkMessage;

import java.util.Base64;
import java.util.TreeMap;

public class PerfectLinkIn {

    private static int WINDOW_SIZE = 5;

    private int receiveBase;
    private TreeMap<Integer, NetworkMessage> notDeliveredPackets;

    private Address inAddress;
    private PerfectLink link;
    private EventHandler plEventHandler;

    public PerfectLinkIn(PerfectLink link, Address inAddress) {
        this.receiveBase = 0;
        this.link = link;
        this.inAddress = inAddress;
        this.plEventHandler = new EventHandler();
        this.notDeliveredPackets = new TreeMap<>();
    }

    public void receive (NetworkMessage message) {
        int sequenceNumber = message.getSequenceNumber();
        String outProcessId = message.getSenderId();

        if ( message.isSend() ) {

            // deliver message
            synchronized (this) {

                // DEBUG :
                /*
                System.out.println("[" + inAddress.getProcessId() + "] Received send message from " + outProcessId + " with sn = " + sequenceNumber);
                System.out.println(" > receive base = " + this.receiveBase);
                System.out.println(" > window = " + this.notDeliveredPackets);
                /**/

                // base sequence number received, must deliver all possible packets
                if (this.receiveBase == sequenceNumber) {

                    // DEBUG :
                    /*
                    System.out.println("received base sequence number");
                    /**/

                    this.notDeliveredPackets.put(sequenceNumber, message);
                    for (int toDeliverSN = this.receiveBase; toDeliverSN < this.receiveBase + WINDOW_SIZE; toDeliverSN++) {

                        if (this.notDeliveredPackets.containsKey(toDeliverSN)) {
                            NetworkMessage toDeliverMessage = this.notDeliveredPackets.remove(toDeliverSN);
                            Object payload = toDeliverMessage.getPayload();

                            Pp2pDeliver deliver_event = new Pp2pDeliver(outProcessId, payload);
                            plEventHandler.trigger(deliver_event);

                            this.receiveBase += 1;
                        } else {
                            // no more in sequence packets to deliver
                            break;
                        }
                    }

                    // send ack
                    this.link.send_ack(sequenceNumber);


                    // DEBUG :
                    /*
                    System.out.println(" > window after = " + this.notDeliveredPackets);
                    /**/

                // ack all packets already received
                } else if (this.receiveBase > sequenceNumber) {

                    // DEBUG :
                    /*
                    System.out.println("received a packet that is already delivered and out of the window");
                    /**/

                    // send ack
                    this.link.send_ack(sequenceNumber);

                // process out of order packets, that are inside the window
                } else if (this.receiveBase + WINDOW_SIZE > sequenceNumber) {

                    // DEBUG :
                    /*
                    System.out.println("packet out of order but in the window range");
                    /**/

                    // if not already in the not delivered list add it
                    if (!this.notDeliveredPackets.containsKey(sequenceNumber)) {
                        notDeliveredPackets.put(sequenceNumber, message);
                    }

                    // DEBUG :
                    /*
                    System.out.println(" > window after = " + this.notDeliveredPackets);
                    /**/

                    // send ack
                    this.link.send_ack(sequenceNumber);
                }
                // all other packets ignore
            }

        } else if ( message.isAck() ) {

            // DEBUG :
            /*
            System.out.println("[" + inAddress.getProcessId() + "] Received ACK message from " + outProcessId + " with sn = " + sequenceNumber);
            /**/

            // must notify the respective perfect link out that the
            // message was already received
            this.link.received_ack(sequenceNumber);
        } else if ( message.isHandshake() ) {

            synchronized (this) {

                // DEBUG :
                /*
                System.out.println("[" + inAddress.getProcessId() + "] Received handshake message from " + outProcessId + " with sn = " + sequenceNumber);
                /**/

                if (this.receiveBase == sequenceNumber) {
                    this.receiveBase += 1;
                }

                if (this.receiveBase >= sequenceNumber) {
                    // send ack
                    this.link.send_ack(sequenceNumber);
                }
            }
        }
    }

    public void subscribeDelivery(EventListener listener) {
        this.plEventHandler.subscribe(Pp2pDeliver.EVENT_NAME, listener);
    }

    public void unsubscribeDelivery(EventListener listener) {
        this.plEventHandler.unsubscribe(Pp2pDeliver.EVENT_NAME, listener);
    }
}
