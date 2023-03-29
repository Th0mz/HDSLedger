package group13.blockchain.auxiliary;

import java.security.PublicKey;

public class IBFTPrepare extends IBFTOperation {
    public static final String constType = "PREPARE";
    public IBFTPrepare(PublicKey pKey, String id, int instance) {
        super(constType, pKey, id, instance);
    }
}
