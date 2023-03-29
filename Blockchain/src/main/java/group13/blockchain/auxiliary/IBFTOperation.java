package group13.blockchain.auxiliary;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.UUID;

public abstract class IBFTOperation implements Serializable {
    protected String type;
    private String uniqueID;
    protected PublicKey pKey;
    protected int instance;

    public IBFTOperation(String type, PublicKey pKey, String id, int inst){
        this.type = type;
        this.pKey = pKey;
        this.uniqueID = id;
        this.instance = inst;
    }

    public String getType() { return type; }
    public PublicKey getPublicKey() { return pKey; }
    public String getId() { return uniqueID; }
    public int getInstance() { return instance; }
}
