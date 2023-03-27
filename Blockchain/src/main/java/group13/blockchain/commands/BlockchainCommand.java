package group13.blockchain.commands;

import java.io.Serializable;
import java.security.PublicKey;

public abstract class BlockchainCommand implements Serializable{
    protected String type;
    protected PublicKey pubKey;

    public BlockchainCommand(String type, PublicKey publicKey){
        this.type = type;
        this.pubKey = publicKey;
    }

    public PublicKey getPublicKey() { return pubKey; }
    public String getType() { return type; }

}
