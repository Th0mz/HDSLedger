package group13.primitives;

import group13.channel.perfectLink.Network;

import java.security.PrivateKey;
import java.security.PublicKey;

public class NetworkTester extends Network {

    private int inProcessId;
    private Address inAddress;
    public NetworkTester (Address inAddress) {
        super(inAddress);

        this.inAddress = inAddress;
        this.inProcessId = inProcessId;
    }

    @Override
    public PerfectLinkTester createAuthenticatedLink(Address outAddress, PublicKey inPublicKey, PrivateKey inPrivateKey, PublicKey outPublicKey) {
        PerfectLinkTester link = new PerfectLinkTester(this.inAddress, outAddress, inPublicKey, inPrivateKey, outPublicKey,false);
        this.links.put(outPublicKey, link);

        return link;
    }
}
