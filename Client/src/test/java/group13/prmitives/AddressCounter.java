package group13.prmitives;

public class AddressCounter {
    public int clientAddress;
    public int replicaAddress;

    public AddressCounter(int clientAddress, int replicaAddress) {
        this.clientAddress = clientAddress;
        this.replicaAddress = replicaAddress;
    }

    public void generateNewAddresses(int offset) {
        clientAddress = clientAddress + offset;
        replicaAddress = replicaAddress + offset;
    }

    public int getClientAddress() {
        return clientAddress;
    }

    public int getReplicaAddress() {
        return replicaAddress;
    }
}
