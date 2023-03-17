package group13.channel.perfectLink;

import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.primitives.Address;
import group13.primitives.EventHandler;
import group13.primitives.EventListener;

import java.util.Base64;

public class PerfectLinkIn {

    private Address inAddress;
    private int currentSequenceNumber;
    private PerfectLink link;
    private EventHandler plEventHandler;

    public PerfectLinkIn(PerfectLink link, Address inAddress) {
        this.currentSequenceNumber = 0;
        this.link = link;
        this.inAddress = inAddress;
        this.plEventHandler = new EventHandler();
    }

    public void receive (byte[] packetData, int packetLength, int packetPort) {
        int messageType = this.getMessageType(packetData);
        int sequenceNumber = this.getSeqNum(packetData);
        String outProcessId = this.getProcessId(packetData);


        if ( messageType == 0 ) {

            // deliver message
            synchronized (this) {
                if (this.currentSequenceNumber == sequenceNumber) {
                    byte[] payload = new byte[packetLength - PerfectLink.HEADER_SIZE];
                    System.arraycopy(packetData, PerfectLink.HEADER_SIZE, payload, 0, packetLength - PerfectLink.HEADER_SIZE);

                    Pp2pDeliver deliver_event = new Pp2pDeliver(outProcessId, payload, packetPort);
                    plEventHandler.trigger(deliver_event);

                    // increase sequence number
                    this.currentSequenceNumber += 1;
                }

                if (this.currentSequenceNumber >= sequenceNumber) {
                    // send ack
                    this.link.send_ack(sequenceNumber);
                }
            }

        } else if (messageType == 1) {
            // must notify the respective perfect link out that the
            // message was already received
            this.link.received_ack(sequenceNumber);
        } else if (messageType == 2) {

            if (this.currentSequenceNumber == sequenceNumber) {
                this.currentSequenceNumber += 1;
            }

            if (this.currentSequenceNumber >= sequenceNumber) {
                // send ack
                this.link.send_ack(sequenceNumber);
            }
        }
    }

    public void subscribeDelivery(EventListener listener) {
        this.plEventHandler.subscribe(Pp2pDeliver.EVENT_NAME, listener);
    }

    public void unsubscribeDelivery(EventListener listener) {
        this.plEventHandler.unsubscribe(Pp2pDeliver.EVENT_NAME, listener);
    }

    // message parse
    public int getSeqNum(byte[] ackData) {
        return (ackData[1] << 24) | (ackData[2] << 16) | (ackData[3] << 8) | ackData[4];
    }

    public String getProcessId(byte[] packetData) {
        int processIdStart = PerfectLink.MESSAGE_TYPE_SIZE + PerfectLink.SEQUENCE_NUMBER_SIZE;
        byte[] processId = new byte[32];

        for (int i = processIdStart; i < processIdStart + PerfectLink.PROCESS_ID_SIZE; i++) {
            processId[i - processIdStart] = packetData[i];
        }

        return Base64.getEncoder().encodeToString(processId);
    }

    public int getMessageType(byte[] data) {

        if(data[0] == 0x00)
            return 0; // it's a 'send' message
        else if (data[0] == 0x01)
            return 1; // it's an 'ack' message
        else if (data[0] == 0x02)
            return 2;

        return -1; //unknown type
    }
}
