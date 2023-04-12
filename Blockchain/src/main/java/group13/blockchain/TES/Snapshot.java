package group13.blockchain.TES;

import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class Snapshot {


    private int snapshotVersion = -1;
    private PublicKey mPublicKey;
    private PrivateKey mPrivateKey;
    private int mySnapshotVersion = -1;

    private HashMap<Integer, HashMap<PublicKey, HashMap<PublicKey, SignedObject >>> outsideSnapshots;
    private HashMap<PublicKey, SignedObject> mySnapshot;

    private int sameSnapSignatures = 0;
    private int f;

    private ReentrantLock snapLock = new ReentrantLock();

    public Snapshot(PublicKey myPubKey, PrivateKey myPrivKey, int faulty){
        mPublicKey = myPubKey;
        mPrivateKey = myPrivKey; 
        outsideSnapshots = new HashMap<>();
        mySnapshot = new HashMap<>();
        f = faulty;
    }


    public SnapshotAccount getBalance(PublicKey client){

        HashMap <PublicKey, SignedObject> signatures = new HashMap<>();
        HashMap<PublicKey, HashMap<PublicKey,SignedObject>> nodes =  outsideSnapshots.get(snapshotVersion);
        System.out.println("GET BALANCE map size:" +nodes.size());
        Float balance = null;
        for (PublicKey node : nodes.keySet()){
            SignedObject aSigned = nodes.get(node).get(client);
            if (balance == null){
                try {
                    balance = (float)aSigned.getObject();
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
            }
            signatures.put(node, aSigned);
        }
        return new SnapshotAccount(signatures, balance, snapshotVersion);
    }
    
    public void takeSnapShot(ArrayList<Account> accounts, int version) {
        mySnapshotVersion = version;
        //snapLock.lock();
        sameSnapSignatures = 1;
       //snapLock.unlock();
        for (Account acc: accounts) {
            mySnapshot.put(acc.getKey(), signBalance(acc.getBalance(), mPrivateKey));
        } 

        //check snapshots received for current version if not add own to others for weak read purposes
        if(outsideSnapshots.get(version) != null) {
           // System.out.println("TAKE SNAP MAP EXISTS");
            for (PublicKey node : outsideSnapshots.get(version).keySet()) {
                
                updateSameSnap(version, node);
            }
        } else {
           // System.out.println("TAKE SNAP CREATE THE MAP");
            HashMap<PublicKey, HashMap<PublicKey, SignedObject>> aux = new HashMap<>();
            aux.put(mPublicKey, mySnapshot);
            outsideSnapshots.put(version, aux);
           // System.out.println("takecreate size:" + outsideSnapshots.get(version));
        }

    }

    public HashMap<PublicKey, SignedObject> getSnapshot(){
        return mySnapshot;
    }

    public void recieveSnapShot(HashMap<PublicKey, SignedObject> snap, PublicKey sender, int version){
        //no need for old version
        if (version < mySnapshotVersion) return;

        //shouldnt happen but extra protection against sending already sent snapshots
        if (outsideSnapshots.get(version) !=null && outsideSnapshots.get(version).get(sender) != null) return;

        //first to present signatures for this version of snapshot
        if (outsideSnapshots.get(version) == null ) {
          //  System.out.println("RECEIVE SNAP CREATE THE MAP");
            HashMap<PublicKey, HashMap<PublicKey, SignedObject>> aux = new HashMap<>();
            aux.put(sender, snap);
            outsideSnapshots.put(version, aux);
        }

        //version exists but not for this node
        if(outsideSnapshots.get(version) !=null && outsideSnapshots.get(version).get(sender) == null) {
          //  System.out.println("RECEIVE SNAP EXISTS MAP");
            outsideSnapshots.get(version).put(sender, snap);
        } 

        //if on curr version update counter of validSignatures for curr view
        if (version == mySnapshotVersion) {
           // System.out.println("RECEIVE SNAP UPDATE");
            updateSameSnap(version, sender);
            //System.out.println("receiveexistsupdate size:" + outsideSnapshots.get(version));
            //System.out.println(sender.equals(mPublicKey));
            //System.out.println("receiveexistsupdate ==");

        }

    }


    public Boolean isValidSnapshot(){
        return snapshotVersion >= 0;
    }

    public int getValidVersion(){
        return snapshotVersion;
    }

    private void updateSameSnap(int version, PublicKey node){
        Boolean valid = true;

        HashMap<PublicKey, SignedObject> signedValues = outsideSnapshots.get(version).get(node);
        
        //size difference hence snapshot doesnt match
        if(mySnapshot.size() !=  signedValues.size()) {
            //remove to not cause conflict in weak read
            outsideSnapshots.get(version).remove(node);
            return;
        }
        
        for (PublicKey client : signedValues.keySet() ) {
            SignedObject myView = mySnapshot.get(client);
            try {
                if ( myView == null || !( (float)signedValues.get(client).getObject()  == (float)myView.getObject()) ) {
                    //if some value does match snapshot considered notsame and doesnt count toward "quorum"
                    valid = false;
                    //System.out.println("SNAPSHOT NOT VERIFIED;");

                    break;
                }
            } catch (ClassNotFoundException | IOException e) {  
                e.printStackTrace();
            }
        }

        if(valid) {
            //System.out.println("SNAPSHOT VERIFIED;");
            sameSnapSignatures++;
            //System.out.println(sameSnapSignatures);
            //got enough signatures to prove vailidity of weak read  and cleanup old valid snaps
            if(sameSnapSignatures >= (2*f + 1)) {
                //System.out.println("SNAPSHOT VALID;");
                snapshotVersion =version;
                Iterator<Map.Entry<Integer, HashMap<PublicKey, HashMap<PublicKey, SignedObject>>>> iterator = outsideSnapshots.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Integer, HashMap<PublicKey, HashMap<PublicKey, SignedObject>>> entry = iterator.next();
                    if (entry.getKey() < snapshotVersion) {
                        iterator.remove();
                    } else {
                        break;
                    }
                }
            }
        } else {
            //remove to not cause conflict in weak read
            outsideSnapshots.get(version).remove(node);
        }
    }

    public static SignedObject signBalance(float value, PrivateKey privateKey) {
        SignedObject signedMessage = null;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            SignedObject signedObject = new SignedObject(value, privateKey, signature);
            return signedObject;
        } catch (SignatureException |NoSuchAlgorithmException | InvalidKeyException | IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return signedMessage;
    }

}
