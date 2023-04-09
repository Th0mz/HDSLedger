package group13.blockchain.commands;

import java.security.PublicKey;

public class CheckBalanceCommand extends BlockchainCommand {

    public static final String constType = "CHECK_BALANCE";
    private boolean isConsistent;
    private int latestViewSeen;

    public CheckBalanceCommand(int seqNum, PublicKey publicKey, int lastViewSeen, boolean consistencyLevel) {
        super(constType, publicKey, seqNum);
        latestViewSeen = lastViewSeen;
        isConsistent = consistencyLevel;
    }

    public int getLastViewSeen() { return latestViewSeen; }
    public boolean getIsConsistent() { return isConsistent; }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && (isConsistent == ((CheckBalanceCommand) o).getIsConsistent());
    }
}
