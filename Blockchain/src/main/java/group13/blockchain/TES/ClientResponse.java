package group13.blockchain.TES;

import group13.blockchain.commands.BlockchainCommand;
import group13.blockchain.commands.CheckBalanceCommand;

import java.io.Serializable;
import java.security.PublicKey;

public class ClientResponse implements Serializable {

    private PublicKey issuer;
    private int sequenceNumber;
    private String commandType;

    private boolean applied;
    private Object response;

    private String typeRead = null;
    private Integer view = null;

    public ClientResponse(int sequenceNumber, PublicKey issuer, String commandType, Object response, boolean applied) {
        this.sequenceNumber = sequenceNumber;
        this.issuer = issuer;
        this.commandType = commandType;
        this.response = response;
        this.applied = applied;
    }

    public ClientResponse(int sequenceNumber, PublicKey issuer, String commandType, String typeRead, Object response, boolean applied) {
        this.sequenceNumber = sequenceNumber;
        this.issuer = issuer;
        this.commandType = commandType;
        this.typeRead = typeRead;
        this.response = response;
        this.applied = applied;
    }

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
        if (this.commandType.equals("CHECK_BALANCE")) {
            typeRead = ((CheckBalanceCommand) command).getIsConsistent() ? "s" : "w";
        }
        this.response = response;
        this.applied = applied;
    }

    public ClientResponse(BlockchainCommand command, Object response, boolean applied, int viewNr) {
        this.sequenceNumber = command.getSequenceNumber();
        this.issuer = command.getPublicKey();
        this.commandType = command.getType();
        if (this.commandType.equals("CHECK_BALANCE")) {
            typeRead = ((CheckBalanceCommand) command).getIsConsistent() ? "s" : "w";
        }
        this.response = response;
        this.applied = applied;
        this.view = viewNr;
    }

    public PublicKey getIssuer() {
        return issuer;
    }

    public Integer getViewSeen() {
        return view;
    }

    public String getTypeRead() {
        return typeRead;
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
    public boolean equals(Object o) {

        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        /* Check if o is an instance of ClientResponse or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof ClientResponse)) {
            return false;
        }

        ClientResponse c = (ClientResponse) o;

        if (this.typeRead != null && !this.typeRead.equals(c.typeRead)) {
            return false;
        }

        if (this.response == null) {
            return this.sequenceNumber == c.getSequenceNumber() && this.issuer.equals(c.getIssuer()) &&
                    this.commandType.equals(c.getCommandType()) && this.response == c.getResponse() &&
                    this.applied == c.wasApplied();
        }

        return this.sequenceNumber == c.getSequenceNumber() && this.issuer.equals(c.getIssuer()) &&
                this.commandType.equals(c.getCommandType()) && this.response.equals(c.getResponse()) &&
                this.applied == c.wasApplied();
    }

    @Override
    public String toString() {
        String result = "[" + this.commandType + "] SN : " + this.sequenceNumber + "    Applied : " + this.applied;
        if (this.response != null) {

            if(this.response instanceof SnapshotAccount) {
                SnapshotAccount aux = (SnapshotAccount)this.response;
                result += "    Result : " + aux.getBalance();
            } else {
                result += "    Result : " + this.response;
            }
        }

        return result;
    }
}
