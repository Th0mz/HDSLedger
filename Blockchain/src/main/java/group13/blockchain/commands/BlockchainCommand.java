package group13.blockchain.commands;

import java.io.Serializable;
import java.security.PublicKey;

public abstract class BlockchainCommand implements Serializable{
    protected String type;
    protected PublicKey pubKey;
    protected int sequenceNumber;

    public BlockchainCommand(String type, PublicKey publicKey, int seqNum){
        this.type = type;
        this.pubKey = publicKey;
        this.sequenceNumber = seqNum;
    }

    public PublicKey getPublicKey() { return pubKey; }
    public String getType() { return type; }
    public int getSequenceNumber() { return sequenceNumber; }

}
