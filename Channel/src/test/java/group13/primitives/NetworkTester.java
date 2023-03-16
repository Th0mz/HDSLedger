package group13.primitives;

import group13.channel.perfectLink.Network;
import group13.channel.perfectLink.PerfectLink;

import java.util.Base64;

public class NetworkTester extends Network {

    private int inProcessId;
    private Address inAddress;
    public NetworkTester (Address inAddress) {
        super(inAddress);

        this.inAddress = inAddress;
        this.inProcessId = inProcessId;
    }

    public PerfectLinkTester createTestLink (Address outAddress, boolean inProblems) {
        PerfectLinkTester link = new PerfectLinkTester(this.inAddress, outAddress, inProblems);

        String outProcessId = outAddress.getProcessId();
        this.links.put(outProcessId, link);

        return link;
    }
}
