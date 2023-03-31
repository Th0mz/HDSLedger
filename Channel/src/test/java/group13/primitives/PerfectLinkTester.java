package group13.primitives;

import group13.channel.perfectLink.PerfectLink;

public class PerfectLinkTester extends PerfectLink {

    // network problems flags
    private boolean inProblems;

    public PerfectLinkTester(Address inAddress, Address outAddress, boolean inProblems) {
        super(inAddress, outAddress);
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
