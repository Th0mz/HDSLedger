package group13.blockchain.commands;

import java.security.PublicKey;

public class TransferCommand extends BlockchainCommand {

    private PublicKey _destPubKey;
    private int _amount;

    public TransferCommand(PublicKey publicKey, PublicKey destPublicKey, int amount) {
        super("TRANSFER", publicKey);
        _destPubKey = destPublicKey;
        _amount = amount;
    }

    public PublicKey getDestPublicKey() { return _destPubKey; }
}
