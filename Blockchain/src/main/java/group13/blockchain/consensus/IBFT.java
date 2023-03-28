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


import java.util.Base64;


public class IBFT implements EventListener{

    protected int nrProcesses, byzantineP, quorum;
    protected int instance;
    protected String input;
    protected BEBroadcast broadcast;
    protected BMember _server;
    protected int round = 1;

    protected long beg;

    protected Lock lockPrepare = new ReentrantLock();
    protected Lock lockCommit = new ReentrantLock();
    protected int prepared, commited;
    protected HashMap<String, HashMap<Integer, Set<PublicKey>>> prepares = new HashMap<>();
    protected HashMap<String, HashMap<Integer, Set<PublicKey>>> commits = new HashMap<>();
    protected HashMap<String, PublicKey> publicKeys;
    protected PrivateKey myKey;
    protected PublicKey myPubKey;
    private PublicKey clientPKey;


    //Timer (eventually)

    protected String leader;
    protected String pId;

    Base64.Encoder encoder = Base64.getEncoder();

    public IBFT(int n, int f, String leader, BEBroadcast beb, BMember server) {
        pId = beb.getInAddress().getProcessId();

        nrProcesses = n;
        byzantineP = f;
        quorum = (nrProcesses + byzantineP)/2 + 1;
        this.leader = leader;
        _server = server;
        broadcast = beb;
        broadcast.subscribeDelivery(this);
        publicKeys = new HashMap<String, PublicKey>(nrProcesses);



        String consensus_folder;
        try {
            consensus_folder = new File("./src/main/java/group13/blockchain/consensus").getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // TODO :
        myKey = getPrivateKey(consensus_folder + "/" + pId.substring(0, 5) + ".key");
        myPubKey = getPubKey(consensus_folder + "/" + pId.substring(0, 5) + ".pub");
        clientPKey = getPubKey(consensus_folder + "/public-key-client.pub");
        for (Address outAddress : beb.getAllAddresses()){
            String outProcessId = outAddress.getProcessId();
            PublicKey key = getPubKey(consensus_folder + "/" + outProcessId.substring(0, 5) + ".pub");

            publicKeys.put(outProcessId, key);
        }
    }

    private int leader(int instance, int round) {
        return round % nrProcesses;
    }

    public void start(int instance, IBFTBlock block) {

        this.instance = instance;
        //input = new String(payload);
        round = 1;

        if ( leader.equals(pId) ) {
            Signature signature;
            try {
                IBFTPrePrepare prePrepare = new IBFTPrePrepare(block, myPubKey);
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

            if (op.getType().equals(IBFTPrePrepare.constType) && leader.equals(((BEBDeliver)event).getProcessId())) 
                prePrepare(op.getBlock(), op.getPublicKey());
            else if (op.getType().equals(IBFTPrepare.constType))
                prepare(op.getBlock(), op.getPublicKey());
            else if (op.getType().equals(IBFTCommit.constType))
                commit(op.getBlock(), op.getPublicKey());
            /* BEBDeliver typed_event = (BEBDeliver) event;
            Object payload = typed_event.getPayload();
            String src = typed_event.getProcessId();

            byte[] signature = extractSignature(payload, payload.length, 256);
            byte[] msg = extractMsg(payload, payload.length - 256);
            //String msg = new String(payload, 0, payload.length - 256);
            //problema aqui se o input acabar com espaço
            String msgType = new String(new byte[]{payload[0]});
            //String[] params = msg.split("\n");

            //System.out.println(src);
            boolean signVerified = verify(msg, signature, publicKeys.get(src)); */
            /* 
            System.out.println("-------------------------");
            System.out.println("PID " + pId +" RECEIVED UPDATE WITH MESSAGE: " + msgType + " FROM PID " + src +"\nVERIFY STATUS: " + signVerified);
            System.out.println("-------------------------");*/
            
            //verify signature
            //check round matches
            // 0 -> PRE-PREPARE; 1 -> PREPARE; 2-> COMMIT

            //if( msgType.equals("0") && leader.equals(src) && signVerified/* && round == Integer.parseInt(params[2])*/) {
                //lock.lock();
                //validPrePreapares.put(msg, signature);
                //lock.unlock();


            //    prePrepare(msg, src);
            //} else if (msgType.equals("1") && signVerified /* && round == Integer.parseInt(params[2])*/) {
            //    prepare(msg, src);
            //} else if (msgType.equals("2") && signVerified ) {
            //    commit(msg, src);
            //}
        }
    }

    protected void prePrepare(IBFTBlock block, PublicKey pKey){
        System.out.println("RECEIVED PREPREPARE");

        try {
            IBFTPrepare prepare = new IBFTPrepare(block, myPubKey);
            Signature signature = Signature.getInstance("SHA256withRSA");
            SignedObject signedObject = new SignedObject(prepare, myKey, signature);
            System.out.print("Generated signedObject of type:"+(IBFTPrepare)signedObject.getObject()+
                        "    with signature:"+signedObject.getSignature());
            this.broadcast.send(new BEBSend(signedObject)); 
            System.out.println("SENT PREPARE MESSAGE");
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | 
                IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //String[] params = new String(msg).split("\n");
        //timer -- maybe not for now
        /*System.out.println("-----------------------");
        System.out.println("-----------------------");
        System.out.println("PRE PREPARE");
        System.out.println("Value: " + params[3]);
        System.out.println("-----------------------");
        System.out.println("-----------------------");*/

        //byte[] payload = new String("1\n" + params[1] + "\n" + params[2] + "\n" + params[3]).getBytes();
        // Send signedPrePrepare for validation that Prepares are not bogus
        //byte[] signedPrePrepare = concatBytes(msg, validPrePreapares.get(msg));
        //byte[] payloadPlusPrePrepare = concatBytes(payload, signedPrePrepare);
        //byte[] signature = sign(payload, myKey);
        //BEBSend send_event = new BEBSend(concatBytes(payload, signature));

        //this.broadcast.send(send_event);
    }

    protected void prepare(IBFTBlock block, PublicKey pKey) {
        System.out.println("RECEIVED PREPARE with block: "+block.getId());
        System.out.println("with hash: "+ block.hashCode());
        Set<PublicKey> setPrepares;
        for(String blk : prepares.keySet())
            System.out.println("List of blocks: "+ blk);
        String id = block.getId();
        lockPrepare.lock();
        if(prepares.containsKey(id) && 
                !prepares.get(id).containsKey(block.getInstance()) ) {
            System.out.println("Contains block but not instance Set");
            setPrepares = new HashSet<PublicKey>();
            prepares.get(id).put(block.getInstance(), setPrepares);
            //System.out.println("ADDED KEY: " + key);
        } else if(!prepares.containsKey(id)){
            System.out.println("Does not contain block");
            setPrepares = new HashSet<PublicKey>();
            prepares.put(id, new HashMap<>());
            prepares.get(id).put(block.getInstance(), setPrepares);
        }
        setPrepares = prepares.get(id).get(block.getInstance());
        lockPrepare.unlock();

        int prepareCount = setPrepares.size();

        if (prepareCount < this.quorum) {

            //System.out.println("SIZE BEFORE: " + prepareCount);
            lockPrepare.lock();
            System.out.println("Before anything setPrepares size: " + setPrepares.size());
            setPrepares.add(pKey);
            prepareCount = setPrepares.size();
            System.out.println("setPrepares size: " + setPrepares.size());
            prepares.get(id).put(block.getInstance(), setPrepares);
            System.out.println("After add, setPrepares size: " + prepares.get(id).get(block.getInstance()).size());
            //System.out.println(prepares.containsKey(block));
            lockPrepare.unlock();
            //System.out.println("SIZE AFTER: " + prepareCount);

            //SEND AS SOON AS QUORUM REACHED AND NO NEED AFTER
            if( prepareCount == quorum ) {
                try {
                    IBFTCommit prepare = new IBFTCommit(block, myPubKey);
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
        //String[] params = new String(msg).split("\n");
        //String key = params[1]+params[2]+params[3];
        /* Set<String> setPrepares;

        lockPrepare.lock();
        if(! prepares.containsKey(key) ) {
            setPrepares = new HashSet<String>();
            prepares.put(key, setPrepares);
            //System.out.println("ADDED KEY: " + key);
        }
        setPrepares = prepares.get(key);
        lockPrepare.unlock();
        int prepareCount = setPrepares.size();
        
        if (prepareCount < quorum) {

            //System.out.println("SIZE BEFORE: " + prepareCount);
            lockPrepare.lock();
            setPrepares.add(src);
            prepareCount = setPrepares.size();
            prepares.put(key, setPrepares);
            lockPrepare.unlock();
            //System.out.println("SIZE AFTER: " + prepareCount);

            //SEND AS SOON AS QUORUM REACHED AND NO NEED AFTER
            if( prepareCount == quorum ) {
    
                preparedRound = Integer.parseInt(params[2]);
                preparedValue = params[3];
                byte[] payload = new String("2\n" + params[1] + "\n" + params[2] + "\n" + params[3]).getBytes();
                byte[] signature = sign(payload, myKey);
                BEBSend send_event = new BEBSend(concatBytes(payload, signature));
                broadcast.send(send_event);
                //System.out.println("SENT BROADCAST OF COMMIT");
            }
        } */
    }

    protected void commit(IBFTBlock block, PublicKey pKey) {
        System.out.println("RECEIVED COMMIT");
        Set<PublicKey> setCommits;
        String id = block.getId();
        lockCommit.lock();
        if(commits.containsKey(id) && 
                !commits.get(id).containsKey(block.getInstance()) ) {
            setCommits = new HashSet<PublicKey>();
            commits.get(id).put(block.getInstance(), setCommits);
            //System.out.println("ADDED KEY: " + key);
        } else if(!commits.containsKey(id)){
            setCommits = new HashSet<PublicKey>();
            commits.put(id, new HashMap<>());
            commits.get(id).put(block.getInstance(), setCommits);
        }
        setCommits = commits.get(id).get(block.getInstance());
        lockCommit.unlock();

        int commitCount = setCommits.size();

        if (commitCount < this.quorum) {
            lockCommit.lock();
            setCommits.add(pKey);
            commitCount = setCommits.size();
            commits.get(id).put(block.getInstance(), setCommits);
            lockCommit.unlock();
    
            //SEND AS SOON AS QUORUM REACHED AND NO NEED AFTER
            if( commitCount == quorum) {
                _server.deliver(block.getInstance(), block);
            }
        }
        //String[] params = new String(msg).split("\n");
        /* String key = params[1]+params[2]+params[3];

        lockCommit.lock();
        if(! commits.containsKey(key) ) {
            setCommits = new HashSet<String>();
            commits.put(key, setCommits);
        }
        setCommits = commits.get(key);
        lockCommit.unlock();
        int commitCount = setCommits.size();

        if (commitCount < quorum) {
            lockCommit.lock();
            setCommits.add(src);
            commitCount = setCommits.size();
            commits.put(key, setCommits);
            lockCommit.unlock();
    
            //SEND AS SOON AS QUORUM REACHED AND NO NEED AFTER
            if( commitCount == quorum) {
                //timer stuff
                //Float time = (System.currentTimeMillis() - beg)/1000F;
                //System.out.println(time);
                //System.out.println("-----------------------");
                System.out.println("\n-----------------------");
                System.out.println("DECIDE by PID " + pId);
                System.out.println("Value decided: " + params[3]);
                System.out.println("-----------------------");
                //System.out.println("-----------------------");

                _server.deliver(Integer.parseInt(params[1]), params[3]);
            }
        } */
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
