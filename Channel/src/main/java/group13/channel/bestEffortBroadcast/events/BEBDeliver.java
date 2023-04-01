package group13.channel.bestEffortBroadcast.events;

import group13.primitives.Event;

import java.security.PublicKey;
import java.util.Arrays;

public class BEBDeliver extends Event {
    public static final String EVENT_NAME = "bebDeliver";
    private PublicKey processPK;
    private Object payload;


    public BEBDeliver (PublicKey processPK, Object payload) {
        super(EVENT_NAME);
        this.processPK = processPK;
        this.payload = payload;
    }

    public PublicKey getProcessPK() {
        return processPK;
    }


    public Object getPayload() {
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

        return this.processPK.equals(deliver.getProcessPK()) &&
                this.payload.equals(deliver.getPayload());
    }
}
