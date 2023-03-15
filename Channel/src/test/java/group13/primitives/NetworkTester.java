package group13.primitives;

import group13.channel.perfectLink.Network;
import group13.channel.perfectLink.PerfectLink;

public class NetworkTester extends Network {

    private int inProcessId;
    private Address inAddress;
    public NetworkTester (int inProcessId, Address inAddress) {
        super(inProcessId, inAddress);

        this.inAddress = inAddress;
        this.inProcessId = inProcessId;
    }

    public PerfectLinkTester createTestLink (int outProcessId, Address outAddress, boolean inProblems) {
        PerfectLinkTester link = new PerfectLinkTester(this.inProcessId, this.inAddress, outProcessId, outAddress, inProblems);
        this.links.put(outProcessId, link);

        return link;
    }
}
