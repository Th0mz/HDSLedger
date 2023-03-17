package group13.blockchain.consensus;

import java.io.IOException;
import java.io.FileInputStream;
import java.security.NoSuchAlgorithmException;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import group13.blockchain.member.BMember;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.channel.bestEffortBroadcast.events.BEBSend;

public class IBFTByzantine extends IBFT {

    private boolean isStartByzantine = false;
    private boolean isPrePrepareByzantine = false;
    private boolean isPrepareByzantine = false;
    private boolean isCommitByzantine = false;
    

    public IBFTByzantine(int n, int f, String leader, BEBroadcast beb, BMember server) {
        super(n, f, leader, beb, server);
        System.out.println("BYZANTINOOOOOOOOOOOOOOO" + beb.getInAddress().getProcessId());
    }

    @Override
    public boolean start(int instance, byte[] value) {

        if(value.length <= 256) {
            return false;
        }
        this.instance = instance;
        input = new String(extractMsg(value, value.length - 256));
        round = 1;
        preparedRound = -1;
        preparedValue = null;

        byte[] msg = new String("0\n" + instance + "\n" + round + "\n" + input).getBytes();
        byte[] signature = sign(msg, super.myKey);
        if(isStartByzantine) {
            System.out.println("-------------------------");
            System.out.println("SENT PRE PREPARE FROM BYZANTINE PID" + pId);
            System.out.println("-------------------------");
            broadcast.send(new BEBSend(concatBytes(msg, signature)));
        } else {
            if ( leader == pId ) {
                //broadcast
                ///Message -> PRE_PREPARE, instance, round, input
                System.out.println("-------------------------");
                System.out.println("SENT PRE PREPARE FROM LEADER PID" + pId);
                System.out.println("-------------------------");
                broadcast.send(new BEBSend(concatBytes(msg, signature)));
                //add signature ???
            }
        }
        return true;
    }

    @Override
    public void prePrepare(byte[] msg, String src) {
        if(!isPrePrepareByzantine)
            super.prePrepare(msg, src);
        else {
            String[] params = new String(msg).split("\n");
            byte[] wrong = new String("0\n" + params[1] + params[2] + "WRONG MESSAGE").getBytes();
            super.prePrepare(wrong, src);
        }
    }

    @Override
    public void prepare(byte[] msg, String src) {
        String[] params = new String(msg).split("\n");
        if(!isPrepareByzantine)
            super.prepare(msg, src);
        else {
            preparedValue = params[3];
            byte[] payload = new String("2\n" + params[1] + "\n" + params[2] + "\n" + "WRONG MESSAGE").getBytes();
            byte[] signature = sign(payload, super.myKey);
            broadcast.send(new BEBSend(concatBytes(payload, signature)));
            System.out.println("SENT BROADCAST OF WRONG COMMIT");
        }
    }

    @Override
    public void commit(byte[] msg, String src) {
        String[] params = new String(msg).split("\n");
        if(!isCommitByzantine)
            super.commit(msg, src);
        else {
            _server.deliver(Integer.parseInt(params[1]), "WRONG_MSG");
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

    public void setStartByzantine() {isStartByzantine = true;}
    public void setPrePrepareByzantine() {isPrePrepareByzantine = true;}
    public void setPrepareByzantine() {isPrepareByzantine = true;}
    public void setCommitByzantine() {isCommitByzantine = true;}
    public void clearAllByzantine() {isStartByzantine = false; isPrePrepareByzantine=false; isPrepareByzantine=false;isCommitByzantine=false;}
    
}
