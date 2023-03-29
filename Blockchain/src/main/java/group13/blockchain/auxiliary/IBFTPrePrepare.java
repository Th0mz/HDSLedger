package group13.blockchain.auxiliary;

import java.security.PublicKey;

public class IBFTPrePrepare extends IBFTOperation {
    public static final String constType = "PREPREPARE";
    protected IBFTBlock block;
    public IBFTPrePrepare(IBFTBlock blk, PublicKey pKey, String id, int instance) {
        super(constType, pKey, id, instance);
        block = blk;
    }

    public IBFTBlock getBlock() { return block; }
}
