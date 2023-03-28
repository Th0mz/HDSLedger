package group13.blockchain.auxiliary;

import java.security.PublicKey;

public class IBFTPrepare extends IBFTOperation {
    public static final String constType = "PREPARE";
    public IBFTPrepare(IBFTBlock blk, PublicKey pKey) {
        super(constType, blk, pKey);
    }
}
