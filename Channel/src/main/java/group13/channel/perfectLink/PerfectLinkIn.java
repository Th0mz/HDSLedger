package group13.channel.perfectLink;

import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.primitives.EventHandler;
import group13.primitives.EventListener;

public class PerfectLinkIn {

    private int inProcessId;
    private int currentSequenceNumber;
    private PerfectLink link;
    private EventHandler plEventHandler;

    public PerfectLinkIn(PerfectLink link, int inProcessId) {
        this.inProcessId = inProcessId;
        this.currentSequenceNumber = 0;
        this.link = link;
        this.plEventHandler = new EventHandler();
    }

    public void receive (byte[] packetData, int packetLength, int packetPort) {
        int messageType = this.getMessageType(packetData);
        int sequenceNumber = this.getSeqNum(packetData);
        int outProcessId = this.getProcessId(packetData);


        if (messageType == 0 ) {
            // DEBUG :
            System.out.println("message received [pid = " + this.inProcessId + "] : ");
            System.out.println(" - process id : " + this.inProcessId);
            System.out.println(" - sequence number : " + sequenceNumber);

            // deliver message
            if (this.currentSequenceNumber == sequenceNumber) {
                String payload = new String(packetData, PerfectLink.HEADER_SIZE, packetLength - PerfectLink.HEADER_SIZE);

                Pp2pDeliver deliver_event = new Pp2pDeliver(outProcessId, payload, packetPort);
                plEventHandler.trigger(deliver_event);

                // increase sequence number
                this.currentSequenceNumber += 1;
            }

            if (this.currentSequenceNumber >= sequenceNumber) {
                // send ack
                this.link.send_ack(sequenceNumber);
                System.out.println("Ack is sent");
            }
        } else if (messageType == 1) {
            // must notify the respective perfect link out that the message was
            // already received
            // DEBUG :
            System.out.println("ACK received [pid = " + this.inProcessId + "] :" );
            System.out.println(" - process id : " + this.inProcessId);
            System.out.println(" - sequence number : " + sequenceNumber);

            this.link.received_ack(sequenceNumber);
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

    public int getProcessId(byte[] packetData) {
        return (packetData[5] << 24) | (packetData[6] << 16) | (packetData[7] << 8) | packetData[8];
    }

    public int getMessageType(byte[] data) {

        if(data[0] == 0x00)
            return 0; // it's a 'send' message
        else if (data[0] == 0x01)
            return 1; // it's an 'ack' message

        return -1; //unknown type
    }
}
