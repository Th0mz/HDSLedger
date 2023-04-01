package group13.primitives;

import java.io.Serializable;

public class FreshObject implements Serializable {

    private static final long serialVersionUID = 92138129L;

    private int sequenceNumber;
    private Object object;

    public FreshObject (int sequenceNumber, Object object) {
        this.sequenceNumber = sequenceNumber;
        this.object = object;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public Object getObject() {
        return object;
    }
}
