package group13.blockchain.auxiliary;

import java.io.Serializable;
import java.security.PublicKey;
import java.security.SignedObject;
import java.util.HashMap;

import group13.blockchain.TES.AmountSigned;

public class SnapOperation implements Serializable{


        protected PublicKey pKey;
        protected int version;
        protected HashMap<PublicKey, SignedObject > signedSnap;
    
        public SnapOperation(PublicKey pKey, HashMap<PublicKey, SignedObject > signedSnap, int version){
            this.pKey = pKey;
            this.signedSnap = signedSnap;
            this.version = version;
        }
    
        public HashMap<PublicKey, SignedObject > GetSignedSnap() { return signedSnap; }
        public PublicKey getPublicKey() { return pKey; }
        public int getVersion() { return version; }
    
}
