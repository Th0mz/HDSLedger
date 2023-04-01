package group13.primitives;

import java.io.Serializable;
import java.security.PublicKey;
import java.security.SignedObject;

public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 12346790L;

    public static enum messageTypes {
        SEND,
        ACK,
        HANDSHAKE
    };

    private messageTypes type;
    private SignedObject payload;
    private int sequenceNumber;
    private PublicKey senderPK;

    public NetworkMessage(PublicKey senderPK, int sequenceNumber, SignedObject payload, messageTypes type) {
        this.senderPK = senderPK;
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
        this.type = type;
    }

    public SignedObject getPayload() {
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

    public PublicKey getSenderPK () {
        return senderPK;
    }
}
