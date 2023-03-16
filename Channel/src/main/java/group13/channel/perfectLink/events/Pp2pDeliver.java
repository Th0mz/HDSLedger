package group13.channel.perfectLink.events;

import group13.primitives.Event;

public class Pp2pDeliver extends Event {

    public static final String EVENT_NAME = "pp2pDeliver";
    private String processId;
    private byte[] payload;
    private int port;


    public Pp2pDeliver(String processId, byte[] payload, int port) {
        super(EVENT_NAME);
        this.processId = processId;
        this.payload = payload;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getProcessId() {
        return processId;
    }

    public byte[] getPayload() {
        return payload;
    }
}
