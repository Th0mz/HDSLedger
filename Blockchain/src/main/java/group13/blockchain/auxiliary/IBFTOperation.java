package group13.blockchain.auxiliary;

import java.io.Serializable;
import java.security.PublicKey;

public abstract class IBFTOperation implements Serializable {
    protected String type;
    protected IBFTBlock block;
    protected PublicKey pKey;

    public IBFTOperation(String type, IBFTBlock blk, PublicKey pKey){
        this.type = type;
        this.block = blk;
        this.pKey = pKey;
    }

    public String getType() { return type; }
    public IBFTBlock getBlock() { return block; }
    public PublicKey getPublicKey() { return pKey; }
}
