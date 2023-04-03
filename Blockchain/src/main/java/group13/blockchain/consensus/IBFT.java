package group13.blockchain.consensus;

import group13.blockchain.auxiliary.IBFTBlock;
import group13.blockchain.auxiliary.IBFTCommit;
import group13.blockchain.auxiliary.IBFTOperation;
import group13.blockchain.auxiliary.IBFTPrePrepare;
import group13.blockchain.auxiliary.IBFTPrepare;
import group13.blockchain.commands.BlockchainCommand;
import group13.blockchain.member.BMember;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.channel.bestEffortBroadcast.events.BEBDeliver;

import group13.channel.bestEffortBroadcast.events.BEBSend;
import group13.primitives.*;

import javax.swing.plaf.synth.SynthTextAreaUI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;


public class IBFT implements EventListener{

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

    protected boolean leaderFailed = false;


    private Timer timer;
    protected PublicKey leaderPK;
    private boolean isLeader;

    public IBFT(int n, int f, PublicKey inPublicKey, PrivateKey inPrivateKey, PublicKey leaderPK, BEBroadcast beb, BMember server) {

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
                System.out.print("Generated signedObject of type:"+(IBFTPrePrepare)signedObject.getObject()+
                            "    with signature:"+signedObject.getSignature());
                this.broadcast.send(new BEBSend(signedObject)); 
                System.out.println("SENT PREPREPARE MESSAGE");
            } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | 
                    IOException | ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


    public void update(Event event) {
        if (event.getEventName().equals(BEBDeliver.EVENT_NAME)) {
            Object payload = ((BEBDeliver)event).getPayload();
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
        System.out.println("RECEIVED PREPREPARE");

        try {
            lockBlocks.lock();
            lockCommit.lock();

            String blockId = block.getId();
            int instance = block.getInstance();

            //1ST: CHECK THAT THE COMMANDS INDEED CAME FROM THE CLIENT
            //2ND: CHECK THAT THE LEADER DIDNT SEND REPEATED COMMANDS
            //3RD: CHECK THAT THE LEADER DIDNT SEND REPEATED COMMANDS WITHIN BLOCK
            lockReceived.lock();
            for (SignedObject signedObject : block.getCommandsList()) {
                try {
                    if (!(signedObject.getObject() instanceof BlockchainCommand)) {
                        lockReceived.unlock();
                        lockCommit.unlock();
                        lockBlocks.unlock();
                        return;
                    }
        
                    BlockchainCommand bcommand = (BlockchainCommand) signedObject.getObject();
                    if (!signedObject.verify(bcommand.getPublicKey(), Signature.getInstance("SHA256withRSA"))) {
                        lockReceived.unlock();
                        lockCommit.unlock();
                        lockBlocks.unlock();
                        return;
                    }
                    
                    if (receivedCommands.containsKey(bcommand.getPublicKey()) && 
                        receivedCommands.get(bcommand.getPublicKey()).containsKey(bcommand.getSequenceNumber())) {
                        System.out.println("LEADER FAILED: SENT REPEATED COMMANDS");
                        leaderFailed = true;
                        lockReceived.unlock();
                        lockCommit.unlock();
                        lockBlocks.unlock();
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
                if (receivedCommands.containsKey(command.getPublicKey()) && 
                        receivedCommands.get(command.getPublicKey()).containsKey(command.getSequenceNumber())){
                    removeEntireBlock = true; //REPEATED COMMANDS WITHIN BLOCK
                }
                if (!pendingCommands.get(command.getPublicKey()).get(command.getSequenceNumber()).equals(command)) {
                    System.out.println("INCORRECT BEHAVIOR: COMMAND FROM CLIENT DIFFERENT FROM WHAT LEADER SENT"); //COMMAND DOESNT MATCH THE ONE SENT BY CLIENT
                    continue;
                }

                if(!receivedCommands.containsKey(pKey))
                    receivedCommands.put(pKey, new HashMap<Integer, BlockchainCommand>());
                if(!receivedCommands.get(pKey).containsKey(command.getSequenceNumber())) {
                    receivedCommands.get(pKey).put(command.getSequenceNumber(), command);
                }
            }

            if(removeEntireBlock) {
                System.out.println("LEADER FAILED: SENT REPEATED COMMANDS WITHIN BLOCK");
                leaderFailed = true;
                for (SignedObject signedObject : block.getCommandsList()) {
                    BlockchainCommand command = (BlockchainCommand) signedObject.getObject();
                    receivedCommands.get(pKey).remove(command.getSequenceNumber());
                }
                lockReceived.unlock();
                lockCommit.unlock();
                lockBlocks.unlock();
                return;
            }

            blocks.put(blockId, block);

            IBFTPrepare prepare = new IBFTPrepare(myPubKey, blockId, instance);
            Signature signature = Signature.getInstance("SHA256withRSA");
            SignedObject signedObject = new SignedObject(prepare, myKey, signature);           
            this.broadcast.send(new BEBSend(signedObject)); 

            // Can deliver if it already has a quorum of commits 
            if(commits.containsKey(blockId) && commits.get(blockId).containsKey(instance) 
                    && commits.get(blockId).get(instance).size() >= quorum) {
                _server.deliver(instance, block);
            }

            System.out.print("Generated signedObject of type:"+(IBFTPrepare)signedObject.getObject()+
                        "    with signature:"+signedObject.getSignature());
            System.out.println("SENT PREPARE MESSAGE");

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
        System.out.println("RECEIVED PREPARE with block: " + id);
        Set<PublicKey> setPrepares;
        for(String blk : prepares.keySet())
            System.out.println("List of blocks: "+ blk);
        lockPrepare.lock();
        if(prepares.containsKey(id) && 
                !prepares.get(id).containsKey(instance) ) {
            System.out.println("Contains block but not instance Set");
            setPrepares = new HashSet<PublicKey>();
            prepares.get(id).put(instance, setPrepares);
            //System.out.println("ADDED KEY: " + key);
        } else if(!prepares.containsKey(id)){
            System.out.println("Does not contain block");
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
            System.out.println("Before anything setPrepares size: " + setPrepares.size());
            setPrepares.add(pKey);
            prepareCount = setPrepares.size();
            System.out.println("setPrepares size: " + setPrepares.size());
            prepares.get(id).put(instance, setPrepares);
            System.out.println("After add, setPrepares size: " + prepares.get(id).get(instance).size());
            //System.out.println(prepares.containsKey(block));
            lockPrepare.unlock();
            //System.out.println("SIZE AFTER: " + prepareCount);

            //SEND AS SOON AS QUORUM REACHED AND NO NEED AFTER
            if( prepareCount == quorum ) {
                try {
                    IBFTCommit prepare = new IBFTCommit(myPubKey, id, instance);
                    Signature signature = Signature.getInstance("SHA256withRSA");
                    SignedObject signedObject = new SignedObject(prepare, myKey, signature);
                    System.out.print("Generated signedObject of type:"+(IBFTCommit)signedObject.getObject()+
                                "    with signature:"+signedObject.getSignature());
                    this.broadcast.send(new BEBSend(signedObject)); 
                    System.out.println("SENT COMMIT MESSAGE");
                } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | 
                        IOException | ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    protected void commit(String id, int instance, PublicKey pKey) {
        System.out.println("RECEIVED COMMIT");
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
                _server.deliver(instance, blocks.get(id));
            }
            lockCommit.unlock();
            lockBlocks.unlock();
        }
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

    private byte[] extractSignature(byte[] b, int mlength ,int size){
        byte[] signature = new byte[size];
        System.arraycopy( b, mlength - size, signature, 0, size);
        return signature;
    }

    private byte[] extractMsg(byte[] b, int mlength){
        byte[] msg = new byte[mlength];
        System.arraycopy( b, 0, msg, 0, mlength);
        return msg;
    }

    private byte[] sign(byte[] message, PrivateKey privateKey) {
        byte[] signedMessage = null;
        // Sign the message using the private key
        try {

            //System.out.println("SIGNEDDDDDDDDDDDDDDDDDDDDDDDDDDDD");

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(message);
            signedMessage = signature.sign();
            return signedMessage;

        } catch (SignatureException |NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return signedMessage;
    }

    private boolean verify(byte[] message, byte[] signature, PublicKey publicKey) {
        try{
        // Verify the signature using the public key
        //System.out.println("VERIFYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY");

            Signature signatureVerifier = Signature.getInstance("SHA256withRSA");
            signatureVerifier.initVerify(publicKey);
            signatureVerifier.update(message);
            return signatureVerifier.verify(signature);
        } catch (SignatureException |NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return false;
    }

    private byte [] concatBytes(byte[] a, byte[] b){
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
