package group13.prmitives;

import group13.blockchain.TES.ClientResponse;
import group13.blockchain.commands.BlockchainCommand;
import group13.blockchain.commands.CheckBalanceCommand;
import group13.blockchain.commands.RegisterCommand;
import group13.blockchain.commands.TransferCommand;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.client.ClientFrontend;
import group13.primitives.Address;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ClientFrontendTester extends ClientFrontend {

    private boolean useDifferentKeys = false;
    private HashMap<PublicKey, String> keysTest = new HashMap<>();
    private HashMap<Integer, ClientResponse> expectedResponses = new HashMap<>();

    private Object nullObject = null;

    public ClientFrontendTester(Address inAddress, List<Address> addresses, List<PublicKey> serversPKs, KeyPair keys, int nrFaulty, boolean diffKeys) {

        super();
        testing = true;
        responses = new HashMap<>();

        useDifferentKeys = diffKeys;

        mKey = keys.getPrivate();
        myPubKey = keys.getPublic();
        faulty = nrFaulty;

        System.out.println("Client on port " + inAddress.toString());
        beb = new BEBroadcast(inAddress, myPubKey, mKey);
        if(!diffKeys){

            for(int i = 0; i < addresses.size(); i++) {
                Address outAddress = addresses.get(i);
                PublicKey outPublicKey = serversPKs.get(i);
                keysTest.put(outPublicKey, outAddress.getProcessId().substring(0, 5));
    
                beb.addServer(outAddress, outPublicKey);
            }
        }

        beb.subscribeDelivery(this);
        beb.send_handshake();
    }

    public void registerLogger(boolean applied){
        int sequenceNumber = super.register();
        ClientResponse expectedResponse = new ClientResponse(sequenceNumber, myPubKey, RegisterCommand.constType, nullObject, applied);
        expectedResponses.put(sequenceNumber, expectedResponse);
    }

    public void transferLogger(PublicKey pKeyDest, int amount, boolean applied){
        int sequenceNumber = super.transfer(pKeyDest, amount);
        ClientResponse expectedResponse = new ClientResponse(sequenceNumber, myPubKey, TransferCommand.constType, nullObject, applied);
        expectedResponses.put(sequenceNumber, expectedResponse);
    }

    public void checkBalanceLogger(String readType, float balance, boolean applied){
        int sequenceNumber = super.checkBalance(readType);
        ClientResponse expectedResponse = new ClientResponse(sequenceNumber, myPubKey, CheckBalanceCommand.constType, readType, balance, applied);
        expectedResponses.put(sequenceNumber, expectedResponse);
    }

    public void setSequenceNumber(int newSequenceNumber) {
        mySeqNum = newSequenceNumber;
    }

    public void addCommandLock(int sequenceNumber) {
        commandLock.put(sequenceNumber, new ReentrantLock());

    }

    public void fakeCommandSend(int sequenceNumber) {
        addCommandLock(sequenceNumber);
        setSequenceNumber(sequenceNumber + 1);
    }

    public HashMap<Integer, ClientResponse> getExpectedResponses() {
        return expectedResponses;
    }

    public HashMap<Integer, ArrayList<ClientResponse>> getDeliveredResponses() {
        return responses;
    }

    public void setKeys() {
        super.keys = keysTest;
    }


    public void changeKey(PublicKey ky) {
        HashMap<PublicKey, String> aux = new HashMap<>();
        int count = 0;
        int size = keysTest.keySet().size();
        for (PublicKey k : keysTest.keySet()){
            count++;
            if (count == size){
                aux.put(ky, "TEST");
            } else{
                aux.put(k, keysTest.get(k));
            }
        } 

        keysTest = aux;
        setKeys();
    }
}
