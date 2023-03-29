package group13.blockchain.auxiliary;

import java.security.PublicKey;

public class IBFTCommit extends IBFTOperation {
    public static final String constType = "COMMIT";
    public IBFTCommit(PublicKey pKey, String id, int instance) {
        super(constType, pKey, id, instance);
    }
}
