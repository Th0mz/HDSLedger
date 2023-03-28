package group13.blockchain.auxiliary;

import java.security.PublicKey;

public class IBFTCommit extends IBFTOperation {
    public static final String constType = "PREPREPARE";
    public IBFTCommit(IBFTBlock blk, PublicKey pKey) {
        super(constType, blk, pKey);
    }
}
