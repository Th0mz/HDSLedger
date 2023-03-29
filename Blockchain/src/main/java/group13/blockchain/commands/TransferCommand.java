package group13.blockchain.commands;

import java.security.PublicKey;

public class TransferCommand extends BlockchainCommand {

    public static final String constType = "TRANSFER";
    private PublicKey _destPubKey;
    private float _amount;

    public TransferCommand(int seqNum, PublicKey publicKey, PublicKey destPublicKey, int amount) {
        super(constType, publicKey, seqNum);
        _destPubKey = destPublicKey;
        _amount = amount;
    }

    public PublicKey getDestPublicKey() { return _destPubKey; }
    public float getAmount() { return _amount; }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && (_amount == ((TransferCommand) o).getAmount()) 
                && _destPubKey.equals(((TransferCommand) o).getDestPublicKey());
    }
}
