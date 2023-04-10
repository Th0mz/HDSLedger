package group13.client;

import group13.blockchain.TES.ClientResponse;
import group13.blockchain.TES.SnapshotAccount;
import group13.blockchain.auxiliary.SnapOperation;
import group13.primitives.Address;
import group13.primitives.*;
import group13.blockchain.commands.BlockchainCommand;
import group13.blockchain.commands.CheckBalanceCommand;
import group13.blockchain.commands.RegisterCommand;
import group13.blockchain.commands.TransferCommand;
import group13.channel.bestEffortBroadcast.*;
import group13.channel.bestEffortBroadcast.events.*;
import group13.primitives.EventListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.UIDefaults.ProxyLazyValue;

public class ClientFrontend implements EventListener {

    protected BEBroadcast beb;
    protected PrivateKey mKey;
    protected PublicKey myPubKey;
    protected int mySeqNum = 0;
    protected int latestViewSeen = -1;

    private int lastResponseDelivered = 0;
    private HashSet<Integer> responsesDelivered = new HashSet<>();
    private HashMap<Integer, ReentrantLock> commandLock = new HashMap<>();
    private HashMap<Integer, HashMap<String, Integer>> responsesReceived = new HashMap<>();

    protected int faulty;
    private ReentrantLock seqNumLock = new ReentrantLock();
    private ReentrantLock lockView = new ReentrantLock();

    private HashMap<PublicKey, String> keys = new HashMap();

    public ClientFrontend() {}

    public ClientFrontend(Address inAddress, List<Address> addresses, String pubKeyFile, int faulty) {
        String keys_folder;
        this.faulty = faulty;

        try {
            keys_folder = new File("./src/main/java/group13/client/keys").getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mKey = getPrivateKey(keys_folder + "/private-key-client.key");
        myPubKey = getPubKey(pubKeyFile);

        System.out.println("Client on port " + inAddress.toString());
        beb = new BEBroadcast(inAddress, myPubKey, mKey);
        for(int i = 0; i < addresses.size(); i++) {
            Address outAddress = addresses.get(i);

            PublicKey outPublicKey = getPubKey(keys_folder + "/" + outAddress.getProcessId().substring(0, 5) + ".pub");
            keys.put(outPublicKey, outAddress.getProcessId().substring(0, 5));
            beb.addServer(outAddress, outPublicKey);
        }

        beb.subscribeDelivery(this);
        System.out.println("ClientFrontend : Sending handshake");
        beb.send_handshake();
    }

    public void sendCommand(Serializable unsignedCommand) {

        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
	        SignedObject signedObject = new SignedObject(unsignedCommand, mKey, signature);

            BEBSend send_event = new BEBSend(signedObject);
            beb.send(send_event);

            if (unsignedCommand instanceof BlockchainCommand) {
                commandLock.put(((BlockchainCommand) unsignedCommand).getSequenceNumber(), new ReentrantLock());
            }

        } catch (IOException | InvalidKeyException | SignatureException |
                NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void register(){
        seqNumLock.lock();
        System.out.println("Registering client");
        sendCommand(new RegisterCommand(mySeqNum, myPubKey));
        mySeqNum++;
        seqNumLock.unlock();
        //TODO: create Register object and send to server
    }

    public void transfer(PublicKey pKeyDest, int amount){
        seqNumLock.lock();
        System.out.println("Sending " + amount);
        sendCommand(new TransferCommand(mySeqNum, myPubKey, pKeyDest, amount));
        mySeqNum++;
        seqNumLock.unlock();
        //TODO: create Transfer object and send to server
    }

    public void checkBalance(String readType){
        seqNumLock.lock();
        System.out.println("Checking balance");
        CheckBalanceCommand checkCommand;
        if (readType.equals("w")){
            checkCommand = new CheckBalanceCommand(mySeqNum, myPubKey, latestViewSeen, false);
        } else {
            checkCommand = new CheckBalanceCommand(mySeqNum, myPubKey, latestViewSeen, true);
        }
        sendCommand(checkCommand);
        mySeqNum++;
        seqNumLock.unlock();
        //TODO: create Check object and send to server
    }

    /* when we get responses from servers */
    @Override
    public void update(Event event) {
        String eventType = event.getEventName();

        if (!eventType.equals(BEBDeliver.EVENT_NAME)) {
            System.out.println("Should only receive deliver events (??)");
        }

        BEBDeliver typed_event = (BEBDeliver) event;
        Object object = typed_event.getPayload();

        if (!(object instanceof ClientResponse)) {
            System.err.println("Error : Client received a response that was not an instance of ClientResponse");
            return;
        }

        ClientResponse response = (ClientResponse) object;
        int sequenceNumber = response.getSequenceNumber();

        // don't allow an attacker to send multiple responses to
        // commands that weren't sent by the client yet
        if (sequenceNumber >= mySeqNum) {
            System.err.println("Error : Server is responding to commands that weren't yet sent");
            return;
        }

        // check if the value was already delivered
        if (lastResponseDelivered > sequenceNumber || responsesDelivered.contains(sequenceNumber)) {
            return;
        }

        // get lock
        if (!commandLock.containsKey(sequenceNumber)) {
            System.err.println("Error : Command lock wasn't found");
            return;
        }


        if(response.getCommandType().equals("CHECK_BALANCE")) {
            if (response.getTypeRead().equals("s")) {
                processStrongReadResponse(response, sequenceNumber);
            } else if (response.getTypeRead().equals("w")) {
                processWeakReadResponse(response, sequenceNumber);
            }

        } else  {
            processNonReadResponse(response, sequenceNumber);
        }
        
    }

    private void processStrongReadResponse(ClientResponse response, int sequenceNumber) {
        ReentrantLock lock = commandLock.get(sequenceNumber);
        if (lock != null) {

            lock.lock();
            Boolean readQuorum = false;
            HashMap<String, Integer> received = null;
            if (!responsesReceived.containsKey(sequenceNumber)) {
                received = new HashMap<>();
                this.responsesReceived.put(sequenceNumber, received);
            } else {
                received = responsesReceived.get(sequenceNumber);
            }
    
            String hash = hashResponse(response);
            int counter = 1;
            if (received.containsKey(hash)) {
                counter = received.get(hash) + 1;
            }
            // update number of times that this response was seen
            received.put(hash, counter);


            int sum = received.values().stream().reduce(0, (a,b) -> a + b);
            OptionalInt max = received.values().stream().mapToInt(v -> v).max();


            //IF IMPOSSIBLE TO GET QUORUM
            if( ((2*faulty + 1) - max.getAsInt()) > ((3*faulty + 1) - sum )) {
                System.out.println("[" + response.getCommandType() + "] SN : " + response.getSequenceNumber() 
                + " NO QUORUM REACHED DUE TO POSSIBLE CONCURRENCY NEW READ REQUEST SENT" );

                responsesReceived.remove(sequenceNumber);
                commandLock.remove(sequenceNumber);
                responsesDelivered.add(sequenceNumber);
    
                if (sequenceNumber == lastResponseDelivered) {
                    for (int i = sequenceNumber; i < mySeqNum; i++) {
                        if (!responsesDelivered.contains(sequenceNumber)) {
                            break;
                        }
    
                        responsesDelivered.remove(sequenceNumber);
                        lastResponseDelivered = lastResponseDelivered + 1;
                    }
                }

                checkBalance("s");

            }
    
            // check if a majority of equal responses was reached
            if (counter >= 2 * faulty + 1) {
                // deliver response to the client

                //TODO - CHECK IF OK
                lockView.lock();
                if (response.getViewSeen() < latestViewSeen) {
                    System.out.println("======= STALE READ RESPONSE OMMITED (POSSIBLE DELAY) =======");
                    System.out.println("====== CLIENT HAS ALREADY SEEN A MORE UP TO DATE VIEW =====");
                    
                } else {

                    System.out.println(response);
                    latestViewSeen = response.getViewSeen();
                }
                lockView.unlock();
    
                responsesReceived.remove(sequenceNumber);
                commandLock.remove(sequenceNumber);
                responsesDelivered.add(sequenceNumber);
    
                if (sequenceNumber == lastResponseDelivered) {
                    for (int i = sequenceNumber; i < mySeqNum; i++) {
                        if (!responsesDelivered.contains(sequenceNumber)) {
                            break;
                        }
    
                        responsesDelivered.remove(sequenceNumber);
                        lastResponseDelivered = lastResponseDelivered + 1;
                    }
                }
            }
    
            lock.unlock();
        }
    }
    
    private void processWeakReadResponse(ClientResponse response, int sequenceNumber) {
        ReentrantLock lock = commandLock.get(sequenceNumber);
        //System.out.println("HERE " + response.getIssuer().toString());
        if (lock != null) {
            lock.lock();
           // System.out.println("HERE1 " + response.getIssuer().toString());

            if (responsesDelivered.contains(sequenceNumber)) {
                //System.out.println("WEAK READ ALREADY DELIVERED  SN:" + sequenceNumber);
                lock.unlock();
                return;
            }

            HashMap<String, Integer> received = null;
            if (!responsesReceived.containsKey(sequenceNumber)) {
                received = new HashMap<>();
                this.responsesReceived.put(sequenceNumber, received);
            } else {
                received = responsesReceived.get(sequenceNumber);
            }
            //System.out.println("HERE2");

            String id = Integer.toString(sequenceNumber);
            int responseCounter = 1;
            if (received.containsKey(id)) {
                responseCounter = received.get(id) + 1;
            }
            // update number of times that this response was seen
            received.put(id, responseCounter);


            //int sum = received.values().stream().reduce(0, (a,b) -> a + b);
            //System.out.println(response);
            if (!(response.getResponse() instanceof SnapshotAccount)) {
                boolean finished = false;
                if(responseCounter == (3*faulty + 1)) {
                    finished = true;
                    System.out.println("NO VALID UP TO DATE RESPONSE WAS ATTAINED FOR WEAK READ [SN]:" + sequenceNumber);
                }
                //TODO - REFACTOR THIS - UGLY

                System.out.println("HERE3");

                if(finished) {
                    responsesReceived.remove(sequenceNumber);
                    commandLock.remove(sequenceNumber);
                    responsesDelivered.add(sequenceNumber);
        
                    if (sequenceNumber == lastResponseDelivered) {
                        for (int i = sequenceNumber; i < mySeqNum; i++) {
                            if (!responsesDelivered.contains(sequenceNumber)) {
                                break;
                            }
        
                            responsesDelivered.remove(sequenceNumber);
                            lastResponseDelivered = lastResponseDelivered + 1;
                        }
                    }
                }
                lock.unlock();
                return;
            }

            Boolean valid = false;
            SnapshotAccount acc = (SnapshotAccount)response.getResponse();

            int responseView = acc.getVersion();
            int SignatureCounter = 0;
            float balance = acc.getBalance();
            HashMap<PublicKey, SignedObject> signatures = acc.getSignatures();
            System.out.println("SIGNATURES SIZE:" + signatures.size());

            for (PublicKey key : signatures.keySet()) {
                //TO MAKE SURE ITS AN ACTUAL KNOWN KEY AND NOT FORGED ONES
                if ( keys.get(key) == null){
                  System.out.println("NULL");  
                  continue;
                } 
                SignedObject so = signatures.get(key);
                // IGNORE INVALID SIGNATURES
                try {
                    if(!verify(so, key) || (balance != (float)so.getObject()) ){
                        continue;
                    }
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
                SignatureCounter++;
                if(SignatureCounter >= (2*faulty + 1)) {
                    valid = true;
                    break;
                } 
            }
            //System.out.println("HERE4");


            boolean finished = false;
            if( valid) {
                //TODO - CHECK IF OK
                lockView.lock();
                if (response.getViewSeen() < latestViewSeen) {
                    System.out.println("======= STALE READ RESPONSE OMMITED (POSSIBLE DELAY) =======");
                    System.out.println("====== CLIENT HAS ALREADY SEEN A MORE UP TO DATE VIEW =====");
                    
                } else {
   
                    System.out.println(response);
                    finished = true;
                    latestViewSeen = response.getViewSeen();
                }
                lockView.unlock();

                
            }
            
            //System.out.println(responseCounter);
            //System.out.println(responseCounter == (3*faulty + 1));
            //case where valid (but stale) or invalid response is last one
            if(responseCounter == (3*faulty + 1)  && !finished) {
                finished = true;
                System.out.println("NO VALID UP TO DATE RESPONSE WAS ATTAINED FOR WEAK READ [SN]:" + sequenceNumber);
            }

            if(finished) {
                responsesReceived.remove(sequenceNumber);
                commandLock.remove(sequenceNumber);
                responsesDelivered.add(sequenceNumber);
    
                /*if (sequenceNumber == lastResponseDelivered) {
                    for (int i = sequenceNumber; i < mySeqNum; i++) {
                        if (!responsesDelivered.contains(sequenceNumber)) {
                            break;
                        }
    
                        responsesDelivered.remove(sequenceNumber);
                    }
                }*/
                lastResponseDelivered = lastResponseDelivered + 1;
            }
           // System.out.println("HERE5");


            lock.unlock();
        }
    }


    private void processNonReadResponse(ClientResponse response, int sequenceNumber) {
        ReentrantLock lock = commandLock.get(sequenceNumber);
        lock.lock();

        HashMap<String, Integer> received = null;
        if (!responsesReceived.containsKey(sequenceNumber)) {
            received = new HashMap<>();
            this.responsesReceived.put(sequenceNumber, received);
        } else {
            received = responsesReceived.get(sequenceNumber);
        }

        String hash = hashResponse(response);
        int counter = 1;
        if (received.containsKey(hash)) {
            counter = received.get(hash) + 1;
        }
        // update number of times that this response was seen
        received.put(hash, counter);

        // check if a majority of equal responses was reached
        if (counter >= 2 * faulty + 1) {
            // deliver response to the client
            System.out.println(response);

            responsesReceived.remove(sequenceNumber);
            commandLock.remove(sequenceNumber);
            responsesDelivered.add(sequenceNumber);

            if (sequenceNumber == lastResponseDelivered) {
                for (int i = sequenceNumber; i < mySeqNum; i++) {
                    if (!responsesDelivered.contains(sequenceNumber)) {
                        break;
                    }

                    responsesDelivered.remove(sequenceNumber);
                    lastResponseDelivered = lastResponseDelivered + 1;
                }
            }
        }

        lock.unlock();
    }


    private String hashResponse(ClientResponse response) {
        MessageDigest messageDigest = null;

        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        // get response attributes and compute the hash
        int sequenceNumber = response.getSequenceNumber();
        String commandType = response.getCommandType();
        boolean applied = response.wasApplied();
        Object object = response.getResponse();

        String attributeMerge = sequenceNumber + commandType + applied;
        if (object != null) {
            attributeMerge += object.toString();
        }

        byte[] digest = messageDigest.digest(attributeMerge.getBytes());
        String hash = Base64.getEncoder().encodeToString(digest);
        return hash;
    }

    private boolean verify(SignedObject signedObject, PublicKey publicKey) {
        try {
            //System.out.println("VERIFICATION");
            //System.out.println(signedObject.verify(publicKey, Signature.getInstance("SHA256withRSA")));
            return signedObject.verify(publicKey, Signature.getInstance("SHA256withRSA"));
        } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private static PrivateKey getPrivateKey(String file) {

        try {
            //Encoder enc = Base64.getEncoder();
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);
            fis.close();
            //System.out.println(new String(bytes));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        //hack
        PrivateKey k = null;
        return k;
    }

    private static PublicKey getPubKey(String file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);
            fis.close();
            X509EncodedKeySpec ks = new X509EncodedKeySpec(bytes, "RSA");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey pub = kf.generatePublic(ks);
            return pub;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        //hack
        PublicKey k = null;
        return k;
    }

    public void close () {
        this.beb.close();
    }
}
