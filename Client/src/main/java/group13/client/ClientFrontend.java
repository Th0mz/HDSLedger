package group13.client;

import group13.blockchain.TES.ClientResponse;
import group13.blockchain.TES.SnapshotAccount;
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

public class ClientFrontend implements EventListener {

    protected BEBroadcast beb;
    protected PrivateKey mKey;
    protected PublicKey myPubKey;
    protected int mySeqNum = 0;
    protected int latestViewSeen = -1;


    //* FOR TESTING PURPOSES */
    protected boolean testing = false;
    protected HashMap<Integer, ArrayList<ClientResponse>> responses;
    protected boolean readRetried = false;
    protected float readResult = -100;
    protected boolean readSuccesful = false;
    protected String reasonReadFailed = null;
    protected int responsesWeakRead = 0;
    //*======================= */

    private int lastResponseDelivered = 0;
    private HashSet<Integer> responsesDelivered = new HashSet<>();
    protected HashMap<Integer, ReentrantLock> commandLock = new HashMap<>();
    private HashMap<Integer, HashMap<String, Set<PublicKey>>> responsesReceived = new HashMap<>();

    protected int faulty;
    private ReentrantLock seqNumLock = new ReentrantLock();
    private ReentrantLock lockView = new ReentrantLock();

    public HashMap<PublicKey, String> keys = new HashMap<>();

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

    public int register(){
        int usedSequenceNumber = -1;
        seqNumLock.lock();
        System.out.println("Registering client");
        sendCommand(new RegisterCommand(mySeqNum, myPubKey));
        usedSequenceNumber = mySeqNum;
        mySeqNum++;
        seqNumLock.unlock();
        //TODO: create Register object and send to server
        return usedSequenceNumber;
    }

    public int transfer(PublicKey pKeyDest, int amount){
        int usedSequenceNumber = -1;
        seqNumLock.lock();
        System.out.println("Sending " + amount);
        sendCommand(new TransferCommand(mySeqNum, myPubKey, pKeyDest, amount));
        usedSequenceNumber = mySeqNum;
        mySeqNum++;
        seqNumLock.unlock();
        //TODO: create Transfer object and send to server
        return usedSequenceNumber;

    }

    public int checkBalance(String readType){
        int usedSequenceNumber = -1;
        //System.out.println("HEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
        seqNumLock.lock();
       // System.out.println("Checking balance");
        CheckBalanceCommand checkCommand;
        if (readType.equals("w")){
            checkCommand = new CheckBalanceCommand(mySeqNum, myPubKey, latestViewSeen, false);
        } else {
            checkCommand = new CheckBalanceCommand(mySeqNum, myPubKey, latestViewSeen, true);
        }
        sendCommand(checkCommand);
        usedSequenceNumber = mySeqNum;
        mySeqNum++;
        seqNumLock.unlock();
        //TODO: create Check object and send to server
        return usedSequenceNumber;
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
        PublicKey sender = typed_event.getProcessPK();

        if (!(object instanceof ClientResponse)) {
            System.err.println("Error : Client received a response that was not an instance of ClientResponse");
            return;
        }

        ClientResponse response = (ClientResponse) object;
        int sequenceNumber = response.getSequenceNumber();

        // don't allow an attacker to send multiple responses to
        // commands that weren't sent by the client yet
        seqNumLock.lock();
        if (sequenceNumber >= mySeqNum) {
            System.err.println("Error : Server is responding to commands that weren't yet sent");
            seqNumLock.unlock();
            return;
        }
        seqNumLock.unlock();

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
                processStrongReadResponse(response, sequenceNumber, sender);
            } else if (response.getTypeRead().equals("w")) {
                processWeakReadResponse(response, sequenceNumber, sender);
            }

        } else  {
            processNonReadResponse(response, sequenceNumber, sender);
        }
        
    }

    private void processStrongReadResponse(ClientResponse response, int sequenceNumber, PublicKey sender) {
        ReentrantLock lock = commandLock.get(sequenceNumber);
        
        if (lock != null) {

            lock.lock();

            Boolean readQuorum = false;
            HashMap<String, Set<PublicKey>> received = null;
            if (!responsesReceived.containsKey(sequenceNumber)) {
                received = new HashMap<>();
                this.responsesReceived.put(sequenceNumber, received);
            } else {
                received = responsesReceived.get(sequenceNumber);
            }
    
            String hash = hashResponse(response);
            Set<PublicKey> publicKeys = null;
            if (!received.containsKey(hash)) {
                publicKeys = new HashSet<>();
            } else {
                publicKeys = received.get(hash);
            }

            // update number of times that this response was seen
            publicKeys.add(sender);
            received.put(hash, publicKeys);
            int counter = publicKeys.size();

            System.out.println("\n========================\n========================\n========================");
            System.out.println("TENTATIVE RESPONSE @client: " + myPubKey.hashCode());
            System.out.println(response);
            System.out.println("COUNTER " + counter);
            //System.out.println(response.getIssuer());
            //System.out.println(myPubKey);


            System.out.println("\n========================\n========================\n========================");

            int sum = 0;
            int max = 0;
            for (Set<PublicKey> set : received.values()) {
                int size = set.size();
                sum += size;
                if (size > max) {
                    max = size;
                }
            }

            //IF IMPOSSIBLE TO GET QUORUM
            if( ((2*faulty + 1) - max) > ((3*faulty + 1) - sum )) {
                System.out.println("[" + response.getCommandType() + "] SN : " + response.getSequenceNumber() 
                + " NO QUORUM REACHED DUE TO POSSIBLE CONCURRENCY NEW READ REQUEST SENT" );

                readSuccesful = false;
                

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

                readRetried = true;
                reasonReadFailed = "NO_QUORUM";
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

                    System.out.println("\n========================\n========================\n========================");
                    System.out.println("VALID RESPONSE");
                    System.out.println(response);
                    System.out.println("\n========================\n========================\n========================");

                    latestViewSeen = response.getViewSeen();

                    //for tests
                    readSuccesful = true;
                    readResult = (float) response.getResponse();
                }
                lockView.unlock();
    
                responsesReceived.remove(sequenceNumber);
                commandLock.remove(sequenceNumber);
                responsesDelivered.add(sequenceNumber);
                if (testing) {
                    ArrayList<ClientResponse> deliveredResponses = null;
                    if (!responses.containsKey(sequenceNumber)) {
                        deliveredResponses = new ArrayList<>();
                    } else {
                        deliveredResponses = responses.get(sequenceNumber);
                    }

                    deliveredResponses.add(response);
                    responses.put(sequenceNumber, deliveredResponses);
                }
                
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
    
    private void processWeakReadResponse(ClientResponse response, int sequenceNumber, PublicKey sender) {
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

            HashMap<String, Set<PublicKey>> received = null;
            if (!responsesReceived.containsKey(sequenceNumber)) {
                received = new HashMap<>();
                this.responsesReceived.put(sequenceNumber, received);
            } else {
                received = responsesReceived.get(sequenceNumber);
            }
            //System.out.println("HERE2");

            String id = Integer.toString(sequenceNumber);
            Set<PublicKey> publicKeys = null;
            if (!received.containsKey(id)) {
                publicKeys = new HashSet<>();
            } else {
                publicKeys = received.get(id);
            }

            // update number of times that this response was seen
            publicKeys.add(sender);
            received.put(id, publicKeys);
            int responseCounter = publicKeys.size();
            responsesWeakRead = responseCounter;

            //int sum = received.values().stream().reduce(0, (a,b) -> a + b);
            //System.out.println(response);
            if (!(response.getResponse() instanceof SnapshotAccount)) {
                boolean finished = false;
                if(responseCounter == (3*faulty + 1)) {
                    finished = true;
                    System.out.println("NO VALID UP TO DATE RESPONSE WAS ATTAINED FOR WEAK READ [SN]:" + sequenceNumber);
                }
                //TODO - REFACTOR THIS - UGLY

                readSuccesful = false;
                reasonReadFailed = "NO_QUORUM";

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
                  reasonReadFailed = "FAILED_SIGNATURES";
                  System.out.println("NULL");  
                  continue;
                } 
                SignedObject so = signatures.get(key);
                // IGNORE INVALID SIGNATURES
                try {
                    if(!verify(so, key) || (balance != (float)so.getObject()) ){
                        reasonReadFailed = "FAILED_SIGNATURES";
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
                    readSuccesful = true;
                    readResult = balance;
                    latestViewSeen = response.getViewSeen();
                }
                lockView.unlock();

                
            }
            
            //System.out.println(responseCounter);
            //System.out.println(responseCounter == (3*faulty + 1));
            //case where valid (but stale) or invalid response is last one
            if(responseCounter == (3*faulty + 1)  && !finished) {
                finished = true;
                readSuccesful = false;
                System.out.println("NO VALID UP TO DATE RESPONSE WAS ATTAINED FOR WEAK READ [SN]:" + sequenceNumber);
            }

            if(finished) {
                responsesReceived.remove(sequenceNumber);
                commandLock.remove(sequenceNumber);
                responsesDelivered.add(sequenceNumber);
                if (testing) {
                    ArrayList<ClientResponse> deliveredResponses = null;
                    if (!responses.containsKey(sequenceNumber)) {
                        deliveredResponses = new ArrayList<>();
                    } else {
                        deliveredResponses = responses.get(sequenceNumber);
                    }

                    deliveredResponses.add(response);
                    responses.put(sequenceNumber, deliveredResponses);
                }
    
                if (sequenceNumber == lastResponseDelivered) {
                    for (int i = sequenceNumber; i < mySeqNum; i++) {
                        if (!responsesDelivered.contains(sequenceNumber)) {
                            break;
                        }
    
                        responsesDelivered.remove(sequenceNumber);
                    }
                }
                lastResponseDelivered = lastResponseDelivered + 1;
            }

            lock.unlock();
        }
    }

    public int getNrResponsesWeakRead(){
        return responsesWeakRead;
    }

    private void processNonReadResponse(ClientResponse response, int sequenceNumber, PublicKey sender) {
        ReentrantLock lock = commandLock.get(sequenceNumber);
        if(lock != null){

            lock.lock();
    
            HashMap<String, Set<PublicKey>> received = null;
            if (!responsesReceived.containsKey(sequenceNumber)) {
                received = new HashMap<>();
                this.responsesReceived.put(sequenceNumber, received);
            } else {
                received = responsesReceived.get(sequenceNumber);
            }
    
            String hash = hashResponse(response);
            Set<PublicKey> publicKeys = null;
            if (!received.containsKey(hash)) {
                publicKeys = new HashSet<>();
            } else {
                publicKeys = received.get(hash);
            }
    
            // update number of times that this response was seen
            publicKeys.add(sender);
            received.put(hash, publicKeys);
            int counter = publicKeys.size();
    
            // check if a majority of equal responses was reached
            if (counter >= 2 * faulty + 1) {
                // deliver response to the client
                System.out.println(response);
    
                responsesReceived.remove(sequenceNumber);
                commandLock.remove(sequenceNumber);
                responsesDelivered.add(sequenceNumber);

                if (testing) {
                    ArrayList<ClientResponse> deliveredResponses = null;
                    if (!responses.containsKey(sequenceNumber)) {
                        deliveredResponses = new ArrayList<>();
                    } else {
                        deliveredResponses = responses.get(sequenceNumber);
                    }

                    deliveredResponses.add(response);
                    responses.put(sequenceNumber, deliveredResponses);
                }
    
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

    public boolean retryStrongRead(){
        return readRetried;
    }

    public boolean getReadSuccessful(){
        return readSuccesful;
    }

    public String getReasonFailed() {
        return reasonReadFailed;
    }
    public Float getReadResult(){

        return readResult;
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
