package group13.blockchain.TES;

import java.io.Serializable;
import java.security.PublicKey;
import java.security.SignedObject;
import java.util.HashMap;

public class SnapshotAccount  implements Serializable{
    private float balance;
    private HashMap <PublicKey, SignedObject> signatures;
    private int version;

    public SnapshotAccount(HashMap<PublicKey, SignedObject> sign, Float balance, int version) {
        this.signatures = sign;
        this.balance = balance;
        this.version = version;
    }

    public HashMap<PublicKey, SignedObject> getSignatures() {
        return signatures;
    }

    public float getBalance() {
        return balance;
    }

    public int getVersion() {
        return version;
    }
    
}
