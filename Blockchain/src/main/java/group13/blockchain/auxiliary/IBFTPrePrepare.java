package group13.blockchain.auxiliary;

import java.security.PublicKey;

public class IBFTPrePrepare extends IBFTOperation {
    public static final String constType = "PREPREPARE";
    public IBFTPrePrepare(IBFTBlock blk, PublicKey pKey) {
        super(constType, blk, pKey);
    }
}
