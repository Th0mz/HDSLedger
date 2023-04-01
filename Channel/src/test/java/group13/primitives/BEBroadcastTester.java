package group13.primitives;

import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.channel.perfectLink.PerfectLink;

import java.security.PrivateKey;
import java.security.PublicKey;

public class BEBroadcastTester extends BEBroadcast {
    public BEBroadcastTester (Address inAddress, PublicKey inPublicKey, PrivateKey inPrivateKey) {
        super(inAddress, inPublicKey, inPrivateKey);

        super.network.close();
        super.network = new NetworkTester(inAddress);
    }

    public void setInProblems (Address outAddress, boolean inProblems) {
        for (PerfectLink link : super.links) {
            Address linkAddress = link.getOutAddress();
            if (linkAddress.equals(outAddress)) {
                PerfectLinkTester test_link = (PerfectLinkTester) link;
                test_link.setInProblems(inProblems);
            }
        }
    }

}
