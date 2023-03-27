package group13.blockchain.commands;

import java.security.PublicKey;

public class RegisterCommand extends BlockchainCommand {
    public RegisterCommand(PublicKey publicKey) {
        super("REGISTER", publicKey);
    }
}
