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

    @Override
    public boolean equals(Object o) {
        //instead of instanceof => to ensure subclass != baseclass
        if (!(o == null || o.getClass() != getClass()))
            return false;
        BlockchainCommand c = (BlockchainCommand) o;

        if(!this.type.equals(c.getType()))
            return false;
        
        if(!this.pubKey.equals(c.getPublicKey()))
            return false;
        
        if(this.sequenceNumber != c.getSequenceNumber())
            return false;

        return true;
    }

}
