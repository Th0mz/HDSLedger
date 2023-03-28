package group13.blockchain.commands;

import java.security.PublicKey;

public class TransferCommand extends BlockchainCommand {

    public static final String constType = "TRANSFER";
    private PublicKey _destPubKey;
    private int _amount;

    public TransferCommand(int seqNum, PublicKey publicKey, PublicKey destPublicKey, int amount) {
        super(constType, publicKey, seqNum);
        _destPubKey = destPublicKey;
        _amount = amount;
    }

    public PublicKey getDestPublicKey() { return _destPubKey; }
}
