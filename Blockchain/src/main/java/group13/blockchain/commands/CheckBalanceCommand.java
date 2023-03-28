package group13.blockchain.commands;

import java.security.PublicKey;

public class CheckBalanceCommand extends BlockchainCommand {

    public static final String constType = "CHECKBALANCE";
    private boolean isConsistent;

    public CheckBalanceCommand(int seqNum, PublicKey publicKey) {
        super(constType, publicKey, seqNum);
        isConsistent = true;
    }

    public boolean getIsConsistent() { return isConsistent; }
}
