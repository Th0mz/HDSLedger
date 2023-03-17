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

    @Override
    public PerfectLinkTester createLink (Address outAddress) {
        PerfectLinkTester link = new PerfectLinkTester(this.inAddress, outAddress, false);

        String outProcessId = outAddress.getProcessId();
        this.links.put(outProcessId, link);

        return link;
    }
}
