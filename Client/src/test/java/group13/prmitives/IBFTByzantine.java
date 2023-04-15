package group13.prmitives;

import group13.blockchain.auxiliary.IBFTBlock;
import group13.blockchain.auxiliary.IBFTCommit;
import group13.blockchain.auxiliary.IBFTOperation;
import group13.blockchain.auxiliary.IBFTPrePrepare;
import group13.blockchain.auxiliary.IBFTPrepare;
import group13.blockchain.auxiliary.SnapOperation;
import group13.blockchain.commands.BlockchainCommand;
import group13.blockchain.consensus.IBFT;
import group13.blockchain.member.BMember;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.channel.bestEffortBroadcast.events.BEBDeliver;

import group13.channel.bestEffortBroadcast.events.BEBSend;
import group13.primitives.*;

import java.io.*;
import java.security.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.Base64;


public class IBFTByzantine extends IBFT {

    private static final int CHECKRECEPTION = 10000; //500ms

    protected int nrProcesses, byzantineP, quorum;
    protected int instance;
    protected String input;
    protected BEBroadcast broadcast;
    protected BMember _server;
    protected int round = 1;

    protected long beg;

    protected Lock lockBlocks = new ReentrantLock();
    protected Lock lockPrepare = new ReentrantLock();
    protected Lock lockCommit = new ReentrantLock();
    protected Lock lockPending = new ReentrantLock();
    protected Lock lockReceived = new ReentrantLock();
    protected HashMap<String, HashMap<Integer, Set<PublicKey>>> prepares = new HashMap<>();
    protected HashMap<String, HashMap<Integer, Set<PublicKey>>> commits = new HashMap<>();
    protected HashMap<String, IBFTBlock> blocks = new HashMap<>();

    // tuple key <sequenceNumber, ClientPublicKey> -> List of pending BlockchainCommand's
    protected HashMap<PublicKey, HashMap<Integer, BlockchainCommand>> pendingCommands = new HashMap<>();
    protected HashMap<PublicKey, HashMap<Integer, BlockchainCommand>> receivedCommands = new HashMap<>();
    protected HashMap<PublicKey, HashMap<Integer, CheckReceptionTask>> receptionTimers = new HashMap<>();

    protected PrivateKey myKey;
    protected PublicKey myPubKey;

    public boolean leaderFailed = false;


    private Timer timer;
    protected PublicKey leaderPK;
    private boolean isLeader;

    private int maxSeenValidInstance = -1;

    private int counter_Pre = 0;
    private int counter_PPre = 0;
    private int counter_Com = 0;

    public boolean byzantineRepeatedCommands = false;
    public void setByzantineRepeatedCommands() {byzantineRepeatedCommands = true;}
    public boolean byzantineSuppressCommands = false;
    public void setByzantineSuppressCommands() {byzantineSuppressCommands = true;}

    public IBFTByzantine(int n, int f, PublicKey inPublicKey, PrivateKey inPrivateKey, PublicKey leaderPK, BEBroadcast beb, BMember server) {
        super(n, f, inPublicKey, inPrivateKey, leaderPK, beb, server);
        this.isLeader = leaderPK.equals(inPublicKey);
        nrProcesses = n;
        byzantineP = f;
        quorum = (nrProcesses + byzantineP)/2 + 1;
        this.leaderPK = leaderPK;
        _server = server;
        broadcast = beb;
        broadcast.subscribeDelivery(this);

        myKey = inPrivateKey;
        myPubKey = inPublicKey;

        timer = new Timer();
    }

    private class CheckReceptionTask extends TimerTask {

        private int sequenceNumber;
        private PublicKey pKey;

        public CheckReceptionTask(int sequenceNumber, PublicKey pKey) {
            this.sequenceNumber = sequenceNumber;
            this.pKey = pKey;

            // schedule task
            timer.schedule(this, CHECKRECEPTION);
        }

        @Override
        public void run() {
            lockPending.lock();
            lockReceived.lock();
            if (!receivedCommands.containsKey(pKey) || !receivedCommands.get(pKey).containsKey(sequenceNumber)) {
                System.out.println("LEADER FAILED: HASNT SENT THE PREPREPARE YET");
                leaderFailed = true;
                lockReceived.unlock();
                lockPending.unlock();
                this.cancel();
                return;                
            } 
            lockReceived.unlock();
            lockPending.unlock();
            this.cancel();
        }

        public void terminate() {
            this.cancel();
        }
    }

    public void waitForCommand(BlockchainCommand command) {
        PublicKey pKey = command.getPublicKey();
        int seqNum = command.getSequenceNumber();

        lockPending.lock();
        //TODO: se ja existir na lista de comandos recebidos nao faz nada
        if(!pendingCommands.containsKey(pKey))
            pendingCommands.put(pKey, new HashMap<Integer, BlockchainCommand>());
        if(!pendingCommands.get(pKey).containsKey(command.getSequenceNumber())) {
            pendingCommands.get(pKey).put(command.getSequenceNumber(), command);
        }

        HashMap<Integer, CheckReceptionTask> hmp = new HashMap<Integer, CheckReceptionTask>();
        hmp.put(seqNum, new CheckReceptionTask(seqNum, pKey));
        receptionTimers.put(pKey, hmp);
        
        lockPending.unlock();
    }

    public int getMaxSeenValidInstance(){
        return maxSeenValidInstance;
    }

    public void start(int instance, IBFTBlock block) {

        this.instance = instance;
        //input = new String(payload);
        round = 1;

        if ( isLeader ) {
            Signature signature;
            try {
                IBFTPrePrepare prePrepare = new IBFTPrePrepare(block, myPubKey, block.getId(), block.getInstance());
                signature = Signature.getInstance("SHA256withRSA");
                SignedObject signedObject = new SignedObject(prePrepare, myKey, signature);
              //  System.out.print("Generated signedObject of type:"+(IBFTPrePrepare)signedObject.getObject()+
              //              "    with signature:"+signedObject.getSignature());
                this.broadcast.send(new BEBSend(signedObject)); 
                counter_PPre++;
               // System.out.println("\n\n\n--------------------------");
               // System.out.println("SENT PREPREPARE MESSAGE N "+ counter_PPre );
               // System.out.println("\n\n\n--------------------------");
                
            } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | 
                    IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void sendSnapShot(HashMap<PublicKey, SignedObject > snap, int version) {
        this.broadcast.send(new BEBSend(new SnapOperation(myPubKey, snap, version)));
    }

    public void update(Event event) {
        if (event.getEventName().equals(BEBDeliver.EVENT_NAME)) {
            Object payload = ((BEBDeliver)event).getPayload();

            // for snapshots
            if ((payload instanceof SnapOperation)) {
                    //System.out.println("SNAPSHOT RECEIVED @IBFT");
                    SnapOperation operation = ((SnapOperation)payload);
                    _server.deliverSnapShot(operation.GetSignedSnap(), ((BEBDeliver)event).getProcessPK(), operation.getVersion());
                    return;
            }


            if (!(payload instanceof SignedObject))
                return;

            SignedObject signedObject = (SignedObject) payload;
            IBFTOperation op = null;
            try {
                if (!(signedObject.getObject() instanceof IBFTOperation))
                    return;
                op = (IBFTOperation) signedObject.getObject();

                if (!signedObject.verify(op.getPublicKey(), Signature.getInstance("SHA256withRSA")))
                    return;
                
            } catch (ClassNotFoundException | IOException | InvalidKeyException | 
                        SignatureException | NoSuchAlgorithmException  e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            BEBDeliver typed_event = (BEBDeliver) event;
            if (op.getType().equals(IBFTPrePrepare.constType) && leaderPK.equals(typed_event.getProcessPK()))
                prePrepare(((IBFTPrePrepare)op).getBlock(), op.getPublicKey());
            else if (op.getType().equals(IBFTPrepare.constType))
                prepare(op.getId(), op.getInstance(), op.getPublicKey());
            else if (op.getType().equals(IBFTCommit.constType))
                commit(op.getId(), op.getInstance(), op.getPublicKey());
        }
    }

    protected void prePrepare(IBFTBlock block, PublicKey pKey){
        //System.out.println("RECEIVED PREPREPARE");

        try {
            lockBlocks.lock();
            lockCommit.lock();


            String blockId = null;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(block);

                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                blockId = new String(Base64.getEncoder().encode(sha256.digest(baos.toByteArray())));
            }

            int instance = block.getInstance();
            //1ST: CHECK THAT THE COMMANDS INDEED CAME FROM THE CLIENT
            //2ND: CHECK THAT THE LEADER DIDNT SEND REPEATED COMMANDS
            //3RD: CHECK THAT THE LEADER DIDNT SEND REPEATED COMMANDS WITHIN BLOCK
            lockReceived.lock();
            for (SignedObject signedObject : block.getCommandsList()) {
                try {
                    if (!(signedObject.getObject() instanceof BlockchainCommand)) {
                        leaderFailed = true;
                        return;
                    }
        
                    BlockchainCommand bcommand = (BlockchainCommand) signedObject.getObject();
                    if (!signedObject.verify(bcommand.getPublicKey(), Signature.getInstance("SHA256withRSA"))) {
                        System.out.println("LEADER FAILED: WRONG SIGNATURE");
                        leaderFailed = true;
                        return;
                    }
                    
                    if (receivedCommands.containsKey(bcommand.getPublicKey()) && 
                        receivedCommands.get(bcommand.getPublicKey()).containsKey(bcommand.getSequenceNumber())) {
                        System.out.println("LEADER FAILED: SENT REPEATED COMMANDS");
                        leaderFailed = true;
                        return;
                    }
                } catch (ClassNotFoundException | IOException | InvalidKeyException | 
                            SignatureException | NoSuchAlgorithmException  e) {
                    e.printStackTrace();
                }
            }
        
            boolean removeEntireBlock = false;
            for (SignedObject signedObject : block.getCommandsList()) {
                BlockchainCommand command = (BlockchainCommand) signedObject.getObject();
                PublicKey commPKey = command.getPublicKey();
                int seqNumber = command.getSequenceNumber();
                if (receivedCommands.containsKey(commPKey) && 
                        receivedCommands.get(commPKey).containsKey(command.getSequenceNumber())){
                    removeEntireBlock = true; //REPEATED COMMANDS WITHIN BLOCK
                    leaderFailed = true;
                }
                if (pendingCommands.containsKey(commPKey) &&
                    pendingCommands.get(commPKey).containsKey(seqNumber) &&
                    !pendingCommands.get(commPKey).get(seqNumber).equals(command)) {
                    System.out.println("INCORRECT BEHAVIOR: COMMAND FROM CLIENT DIFFERENT FROM WHAT LEADER SENT"); //COMMAND DOESNT MATCH THE ONE SENT BY CLIENT
                    continue;
                }
                
                if(!receivedCommands.containsKey(commPKey))
                    receivedCommands.put(commPKey, new HashMap<Integer, BlockchainCommand>());
                if(!receivedCommands.get(commPKey).containsKey(seqNumber)) {
                    receivedCommands.get(commPKey).put(seqNumber, command);
                }
            }

            if(removeEntireBlock) {
                System.out.println("LEADER FAILED: SENT REPEATED COMMANDS WITHIN BLOCK");
                leaderFailed = true;
                for (SignedObject signedObject : block.getCommandsList()) {
                    BlockchainCommand command = (BlockchainCommand) signedObject.getObject();
                    receivedCommands.get(pKey).remove(command.getSequenceNumber());
                }
                return;
            }

            blocks.put(blockId, block);

            IBFTPrepare prepare = new IBFTPrepare(myPubKey, blockId, instance);
            Signature signature = Signature.getInstance("SHA256withRSA");
            SignedObject signedObject = new SignedObject(prepare, myKey, signature);
            if (byzantineRepeatedCommands) {
                this.broadcast.send(new BEBSend(signedObject));
                this.broadcast.send(new BEBSend(signedObject));
            }           
            if(!byzantineSuppressCommands) this.broadcast.send(new BEBSend(signedObject)); 

            // Can deliver if it already has a quorum of commits 
            if(commits.containsKey(blockId) && commits.get(blockId).containsKey(instance) 
                    && commits.get(blockId).get(instance).size() >= quorum) {
                        System.out.println("EARLY DELIVERY");
                _server.deliver(instance, block);
            }

//            System.out.print("Generated signedObject of type:"+(IBFTPrepare)signedObject.getObject()+
           //             "    with signature:"+signedObject.getSignature());
            counter_Pre++;
           // System.out.println("\n\n\n--------------------------");
           // System.out.println("SENT PREPARE MESSAGE N " +counter_Pre);
           // System.out.println("\n\n\n--------------------------");

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | 
                IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            lockCommit.unlock();
            lockBlocks.unlock();
            lockReceived.unlock();
        }
    }

    protected void prepare(String id, int instance, PublicKey pKey) {
        //System.out.println("RECEIVED PREPARE with block: " + id);
        Set<PublicKey> setPrepares;
        //for(String blk : prepares.keySet())
            //System.out.println("List of blocks: "+ blk);
        lockPrepare.lock();
        if(prepares.containsKey(id) && 
                !prepares.get(id).containsKey(instance) ) {
          //  System.out.println("Contains block but not instance Set");
            setPrepares = new HashSet<PublicKey>();
            prepares.get(id).put(instance, setPrepares);
            //System.out.println("ADDED KEY: " + key);
        } else if(!prepares.containsKey(id)){
           // System.out.println("Does not contain block");
            setPrepares = new HashSet<PublicKey>();
            prepares.put(id, new HashMap<>());
            prepares.get(id).put(instance, setPrepares);
        }
        setPrepares = prepares.get(id).get(instance);
        lockPrepare.unlock();

        int prepareCount = setPrepares.size();

        if (prepareCount < this.quorum) {

            //System.out.println("SIZE BEFORE: " + prepareCount);
            lockPrepare.lock();
           // System.out.println("Before anything setPrepares size: " + setPrepares.size());
            setPrepares.add(pKey);
            prepareCount = setPrepares.size();
           // System.out.println("setPrepares size: " + setPrepares.size());
            prepares.get(id).put(instance, setPrepares);
           // System.out.println("After add, setPrepares size: " + prepares.get(id).get(instance).size());
            //System.out.println(prepares.containsKey(block));
            lockPrepare.unlock();
            //System.out.println("SIZE AFTER: " + prepareCount);

            //SEND AS SOON AS QUORUM REACHED AND NO NEED AFTER
            if( prepareCount == quorum ) {
                try {
                    lockPrepare.lock();
                    if (instance > maxSeenValidInstance) maxSeenValidInstance = instance;
                    lockPrepare.unlock();

                    IBFTCommit prepare = new IBFTCommit(myPubKey, id, instance);
                    Signature signature = Signature.getInstance("SHA256withRSA");
                    SignedObject signedObject = new SignedObject(prepare, myKey, signature);
                   // System.out.print("Generated signedObject of type:"+(IBFTCommit)signedObject.getObject()+
               //                 "    with signature:"+signedObject.getSignature());
                    if (byzantineRepeatedCommands) {
                        this.broadcast.send(new BEBSend(signedObject));
                        this.broadcast.send(new BEBSend(signedObject));
                    }
                    if(!byzantineSuppressCommands) this.broadcast.send(new BEBSend(signedObject)); 
                    counter_Com++;
              //  System.out.println("\n\n\n--------------------------");
               // System.out.println("SENT COMMIT MESSAGE N " +counter_Com);
               // System.out.println("\n\n\n--------------------------");
                } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | 
                        IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    

    protected void commit(String id, int instance, PublicKey pKey) {
        //System.out.println("RECEIVED COMMIT");
        Set<PublicKey> setCommits;
        lockCommit.lock();
        if(commits.containsKey(id) && 
                !commits.get(id).containsKey(instance) ) {
            setCommits = new HashSet<PublicKey>();
            commits.get(id).put(instance, setCommits);
            //System.out.println("ADDED KEY: " + key);
        } else if(!commits.containsKey(id)){
            setCommits = new HashSet<PublicKey>();
            commits.put(id, new HashMap<>());
            commits.get(id).put(instance, setCommits);
        }
        setCommits = commits.get(id).get(instance);
        lockCommit.unlock();

        int commitCount = setCommits.size();

        if (commitCount < this.quorum) {
            lockBlocks.lock();
            lockCommit.lock();
            setCommits.add(pKey);
            commitCount = setCommits.size();
            commits.get(id).put(instance, setCommits);

            //SEND AS SOON AS QUORUM REACHED AND NO NEED AFTER
            if(commitCount == quorum && blocks.containsKey(id)) {
                lockPrepare.lock();
                    if (instance > maxSeenValidInstance) maxSeenValidInstance = instance;
                lockPrepare.unlock();
                _server.deliver(instance, blocks.get(id));
            }
            lockCommit.unlock();
            lockBlocks.unlock();
        }
    }



    public void close() {
        broadcast.close();
    }
}
