package group13.blockchain.commands;

import java.security.PublicKey;

public class RegisterCommand extends BlockchainCommand {
    public static final String constType = "REGISTER";
    public RegisterCommand(int seqNum, PublicKey publicKey) {
        super(constType, publicKey, seqNum);
    }
}
