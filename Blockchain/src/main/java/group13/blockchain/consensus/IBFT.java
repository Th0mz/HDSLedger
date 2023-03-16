package group13.blockchain.consensus;

import group13.blockchain.member.BMember;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.channel.bestEffortBroadcast.events.BEBDeliver;

import group13.channel.bestEffortBroadcast.events.BEBSend;
import group13.primitives.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Key;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;


public class IBFT implements EventListener{
    
    private int nrProcesses, byzantineP, quorum;
    private int pId, preparedRound, instance;
    private String input, preparedValue;
    private BEBroadcast broadcast;
    private BMember _server;
    private int round = 1;

    private Lock lockPrepare = new ReentrantLock();
    private Lock lockCommit = new ReentrantLock();
    private int prepared, commited;
    private HashMap<String, Set<Integer>> prepares = new HashMap<>();
    private HashMap<String, Set<Integer>> commits = new HashMap<>();
    private List<PublicKey> publicKeys = new ArrayList<PublicKey>(4);
    private PrivateKey myKey;
    //private HashMap<byte[],byte[]> validPrePreapares = new HashMap<>();
    
    //Timer (eventually)

    private int leader;

    public IBFT(int id, int n, int f, int leader, int port, BEBroadcast beb, BMember server) {
        pId = id;
        nrProcesses = n;
        byzantineP = f;
        quorum = (nrProcesses + byzantineP)/2 + 1;
        this.leader = leader;
        _server = server;
        broadcast = beb;
        broadcast.subscribeDelivery(this);
        publicKeys = new ArrayList<PublicKey>(nrProcesses);
        myKey = getPrivateKey("public-key-" + pId+1 + ".key");
        for (int i = 0; i < nrProcesses; i++){
            publicKeys.add(getPubKey("public-key-" + i+1 + ".pub"));
        }

    }

    private int leader(int instance, int round) {
        return round % nrProcesses;
    }

    public void start(int instance, String value) {

        this.instance = instance;
        input = value;
        round = 1;
        preparedRound = -1;
        preparedValue = null;

        if ( leader == pId ) {
            //broadcast
            ///Message ->  0(PRE_PREPARE), instance, round, input, signature
            byte[] msg = new String("0\n" + instance + "\n" + round + "\n" + input).getBytes();
            byte[] signature = sign(msg, myKey);

            broadcast.send(new BEBSend(concatBytes(msg, signature)));
        }

        //no timer for now
    }


    public void update(Event event) {
        if (event.getEventName() == BEBDeliver.EVENT_NAME) {
            BEBDeliver typed_event = (BEBDeliver) event;
            byte[] payload = typed_event.getPayload();
            int src = typed_event.getProcessID();

            byte[] signature = extractSignature(payload, payload.length, 256);
            byte[] msg = extractMsg(payload, payload.length - 256);
            //String msg = new String(payload, 0, payload.length - 256);
            //problema aqui se o input acabar com espaço
            String msgType = new String(new byte[]{payload[0]});
            //String[] params = msg.split("\n");


            System.out.println("-------------------------");
            System.out.println("RECEIVED UPDATE WITH MESSAGE: " + msgType);
            System.out.println("-------------------------");
            
            //verify signature
            //check round matches
            // 0 -> PRE-PREPARE; 1 -> PREPARE; 2-> COMMIT
            if( msgType.equals("0") && leader == src  && verify(payload, signature, publicKeys.get(src))/* && round == Integer.parseInt(params[2])*/) {
                /*lock.lock();
                validPrePreapares.put(msg, signature);
                lock.unlock();*/
                prePrepare(msg, src);
            } else if (msgType.equals("1") && verify(payload, signature, publicKeys.get(src)) /* && round == Integer.parseInt(params[2])*/) {
                prepare(msg, src);
            } else if (msgType.equals("2") && verify(payload, signature, publicKeys.get(src)) ) {
                commit(msg, src);
            }
        }
    }

    private void prePrepare(byte[] msg, int src){
        String[] params = new String(msg).split("\n");
        //timer -- maybe not for now
        System.out.println("-----------------------");
        System.out.println("-----------------------");
        System.out.println("PRE PREPARE");
        System.out.println("Value: " + params[3]);
        System.out.println("-----------------------");
        System.out.println("-----------------------");

        byte[] payload = new String("1\n" + params[1] + "\n" + params[2] + "\n" + params[3]).getBytes();
        // Send signedPrePrepare for validation that Prepares are not bogus
        //byte[] signedPrePrepare = concatBytes(msg, validPrePreapares.get(msg));
        //byte[] payloadPlusPrePrepare = concatBytes(payload, signedPrePrepare);
        byte[] signature = sign(payload, myKey);
        BEBSend send_event = new BEBSend(concatBytes(payload, signature));

        this.broadcast.send(send_event);
    }

    private void prepare(byte[] msg, int src) {
        String[] params = new String(msg).split("\n");
        String key = params[1]+params[2]+params[3];
        Set<Integer> setPrepares;
        lockPrepare.lock();
        if(! prepares.containsKey(key) ) {
            setPrepares = new HashSet<Integer>();
            prepares.put(key, setPrepares);
            System.out.println("ADDED KEY: " + key);
        }
        setPrepares = prepares.get(key);
        lockPrepare.unlock();
        int prepareCount = setPrepares.size();
        
        if (prepareCount < quorum) {

            System.out.println("SIZE BEFORE: " + prepareCount);
            lockPrepare.lock();
            setPrepares.add(src);
            prepareCount = setPrepares.size();
            prepares.put(key, setPrepares);
            lockPrepare.unlock();
            System.out.println("SIZE AFTER: " + prepareCount);

            //SEND AS SOON AS QUORUM REACHED AND NO NEED AFTER
            if( prepareCount == quorum ) {
    
                preparedRound = Integer.parseInt(params[2]);
                preparedValue = params[3];
                byte[] payload = new String("2\n" + params[1] + "\n" + params[2] + "\n" + params[3]).getBytes();
                byte[] signature = sign(payload, myKey);
                BEBSend send_event = new BEBSend(concatBytes(payload, signature));
                broadcast.send(send_event);
                System.out.println("SENT BROADCAST OF COMMIT");
            }
        }
    }

    private void commit(byte[] msg, int src) {

        Set<Integer> setCommits;
        String[] params = new String(msg).split("\n");
        String key = params[1]+params[2]+params[3];

        lockCommit.lock();
        if(! commits.containsKey(key) ) {
            setCommits = new HashSet<Integer>();
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
                System.out.println("-----------------------");
                System.out.println("-----------------------");
                System.out.println("DECIDE");
                System.out.println("Value decided: " + params[3]);
                System.out.println("-----------------------");
                System.out.println("-----------------------");
                _server.deliver(Integer.parseInt(params[1]), params[3]);
                // DECIDE( params[1], params[3], commits.get(key));
            }
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
