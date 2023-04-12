package group13.prmitives;

import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.client.ClientFrontend;
import group13.primitives.Address;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;

public class ClientFrontendTester extends ClientFrontend {

    private boolean useDifferentKeys = false;
    HashMap<PublicKey, String> keysTest = new HashMap<>();

    public ClientFrontendTester(Address inAddress, List<Address> addresses, List<PublicKey> serversPKs, KeyPair keys, int nrFaulty, boolean diffKeys) {

        super();

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
