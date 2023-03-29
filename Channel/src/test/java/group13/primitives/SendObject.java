package group13.primitives;

import java.io.Serializable;

public class SendObject implements Serializable {
    private String message;

    public SendObject (String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {

        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        if (!(o instanceof SendObject)) {
            return false;
        }

        SendObject sendObject = (SendObject) o;
        return sendObject.getMessage().equals(this.message);
    }
}
