package group13.channel.bestEffortBroadcast.events;

import group13.primitives.Event;

import java.util.Arrays;

public class BEBDeliver extends Event {
    public static final String EVENT_NAME = "bebDeliver";
    private String processId;
    private byte[] payload;


    public BEBDeliver (String processId, byte[] payload) {
        super(EVENT_NAME);
        this.processId = processId;
        this.payload = payload;
    }

    public String getProcessId() {
        return this.processId;
    }


    public byte[] getPayload() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {

        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        if (!(o instanceof BEBDeliver)) {
            return false;
        }

        BEBDeliver deliver = (BEBDeliver) o;

        return this.processId.equals(deliver.getProcessId()) &&
                Arrays.equals(this.payload, deliver.getPayload());
    }
}
