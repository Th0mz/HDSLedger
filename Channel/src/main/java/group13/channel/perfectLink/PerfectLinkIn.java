package group13.channel.perfectLink;

import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.primitives.*;

import java.io.IOException;
import java.security.*;
import java.util.TreeMap;

public class PerfectLinkIn {

    private static int WINDOW_SIZE = 5;

    private int receiveBase;
    private TreeMap<Integer, Object> notDeliveredObjects;

    private Address inAddress;
    private PerfectLink link;
    private EventHandler plEventHandler;

    private PublicKey outPublicKey;

    public PerfectLinkIn(PerfectLink link, Address inAddress, PublicKey outPublicKey) {
        this.receiveBase = 0;
        this.link = link;
        this.inAddress = inAddress;
        this.plEventHandler = new EventHandler();
        this.notDeliveredObjects = new TreeMap<>();

        this.outPublicKey = outPublicKey;
    }

    public void receive (NetworkMessage message) {
        int sequenceNumber = message.getSequenceNumber();

        // Check if public keys match
        if (!message.getSenderPK().equals(this.outPublicKey)) {
            System.err.println("Error : Sender public key doesn't match this link endpoint public key");
            return;
        }

        // Check signature
        SignedObject signedObject = message.getPayload();
        FreshObject freshObject = null;
        try {
            if (!(signedObject.getObject() instanceof FreshObject)) {
                System.err.println("Error : Signed object is not a FreshObject");
                return;
            }

            freshObject = (FreshObject) signedObject.getObject();
            // signature check
            if (!signedObject.verify(this.outPublicKey, Signature.getInstance("SHA256withRSA")))
                return;

        } catch (ClassNotFoundException | IOException | InvalidKeyException |
                 SignatureException | NoSuchAlgorithmException  e) {
            e.printStackTrace();
        }

        // Check freshness
        if (sequenceNumber != freshObject.getSequenceNumber()) {
            System.err.println("Error : Received object is not fresh");
            return;
        }

        Object objectToDeliver = freshObject.getObject();

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

                    this.notDeliveredObjects.put(sequenceNumber, objectToDeliver);
                    for (int toDeliverSN = this.receiveBase; toDeliverSN < this.receiveBase + WINDOW_SIZE; toDeliverSN++) {

                        if (this.notDeliveredObjects.containsKey(toDeliverSN)) {
                            Object object = this.notDeliveredObjects.remove(toDeliverSN);

                            Pp2pDeliver deliver_event = new Pp2pDeliver(this.outPublicKey, object);
                            plEventHandler.trigger(deliver_event, EventHandler.Mode.ASYNC);

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
                    if (!this.notDeliveredObjects.containsKey(sequenceNumber)) {
                        notDeliveredObjects.put(sequenceNumber, objectToDeliver);
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
                System.out.println("[" + inAddress.getProcessId() + "] Received handshake message from " + outPublicKey + " with sn = " + sequenceNumber);
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
