package group13.blockchain.TES;

import group13.blockchain.commands.BlockchainCommand;

import java.io.Serializable;
import java.security.PublicKey;

public class ClientResponse implements Serializable {

    private PublicKey issuer;
    private int sequenceNumber;
    private String commandType;

    private boolean applied;
    private Object response;

    public ClientResponse(BlockchainCommand command, boolean applied) {
        this.sequenceNumber = command.getSequenceNumber();
        this.issuer = command.getPublicKey();
        this.commandType = command.getType();
        this.response = null;
        this.applied = applied;
    }
    public ClientResponse(BlockchainCommand command, Object response, boolean applied) {
        this.sequenceNumber = command.getSequenceNumber();
        this.issuer = command.getPublicKey();
        this.commandType = command.getType();
        this.response = response;
        this.applied = applied;
    }

    public PublicKey getIssuer() {
        return issuer;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getCommandType() {
        return commandType;
    }

    public Object getResponse() {
        return response;
    }

    public boolean wasApplied() {
        return applied;
    }

    @Override
    public String toString() {
        String result = "[" + this.commandType + "] SN : " + this.sequenceNumber + "    Applied : " + this.applied;
        if (this.response != null) {
            result += "    Result : " + this.response;
        }

        return result;
    }
}
