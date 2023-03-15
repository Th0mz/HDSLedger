package group13.primitives;

import group13.channel.perfectLink.PerfectLink;

public class PerfectLinkTester extends PerfectLink {

    // network problems flags
    private boolean inProblems;

    public PerfectLinkTester(int inProcessId, Address inAddress, int outProcessId, Address outAddress, boolean inProblems) {
        super(inProcessId, inAddress, outProcessId, outAddress);
        this.inProblems = inProblems;
    }

    @Override
    public void receive (byte[] packetData, int packetLength, int packetPort) {
        if (!inProblems) {
            super.receive(packetData, packetLength, packetPort);
        }
    }

    public void setInProblems(boolean inProblems) {
        this.inProblems = inProblems;
    }

    public int getRetransmitQueueSize () {
        return super.sentMessages.size();
    }
}
