package group13.primitives;

import group13.channel.perfectLink.PerfectLink;

import java.security.PrivateKey;
import java.security.PublicKey;

public class PerfectLinkTester extends PerfectLink {

    // network problems flags
    private boolean inProblems;

    public PerfectLinkTester(Address inAddress, Address outAddress, PublicKey inPublicKey, PrivateKey inPrivateKey, PublicKey outPublicKey, boolean inProblems) {
        super(inAddress, outAddress, inPublicKey, inPrivateKey, outPublicKey);
        this.inProblems = inProblems;
    }

    @Override
    public void receive (NetworkMessage message) {
        if (!inProblems) {
            super.receive(message);
        }
    }

    public void setInProblems(boolean inProblems) {
        this.inProblems = inProblems;
    }

    public int getRetransmitQueueSize () {
        return super.retransmitTasks.size();
    }
}
