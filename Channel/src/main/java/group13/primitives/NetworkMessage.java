package group13.primitives;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 12346790L;

    public static enum messageTypes {
        SEND,
        ACK,
        HANDSHAKE
    };

    private messageTypes type;
    private byte[] payload;
    private int sequenceNumber;
    private String senderId;

    public NetworkMessage(String senderId, int sequenceNumber, byte[] payload, messageTypes type) {
        this.senderId = senderId;
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
        this.type = type;
    }

    public byte[] getPayload() {
        return payload;
    }

    public boolean isAck () {
        return this.type == messageTypes.ACK;
    }

    public boolean isHandshake () {
        return this.type == messageTypes.HANDSHAKE;
    }

    public boolean isSend () {
        return this.type == messageTypes.SEND;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getSenderId() {
        return senderId;
    }
}
