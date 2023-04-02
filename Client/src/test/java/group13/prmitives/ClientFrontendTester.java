package group13.prmitives;

import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.client.ClientFrontend;
import group13.primitives.Address;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

public class ClientFrontendTester extends ClientFrontend {
    public ClientFrontendTester(Address inAddress, List<Address> addresses, List<PublicKey> serversPKs, KeyPair keys) {

        mKey = keys.getPrivate();
        myPubKey = keys.getPublic();

        System.out.println("Client on port " + inAddress.toString());
        beb = new BEBroadcast(inAddress, myPubKey, mKey);
        for(int i = 0; i < addresses.size(); i++) {
            Address outAddress = addresses.get(i);
            PublicKey outPublicKey = serversPKs.get(i);

            beb.addServer(outAddress, outPublicKey);
        }

        beb.subscribeDelivery(this);
        beb.send_handshake();
    }
}
