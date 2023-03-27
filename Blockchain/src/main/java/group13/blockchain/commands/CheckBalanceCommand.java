package group13.blockchain.commands;

import java.security.PublicKey;

public class CheckBalanceCommand extends BlockchainCommand {

    private boolean isConsistent;

    public CheckBalanceCommand(PublicKey publicKey) {
        super("CHECKBALANCE", publicKey);
        isConsistent = true;
    }

    public boolean getIsConsistent() { return isConsistent; }
}
