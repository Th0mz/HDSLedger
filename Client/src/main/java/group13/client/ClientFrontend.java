package group13.client;

import group13.primitives.*;
import group13.channel.bestEffortBroadcast.*;
import group13.channel.bestEffortBroadcast.events.*;
import group13.channel.perfectLink.PerfectLinkIn;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

public class ClientFrontend implements EventListener {

    private BEBroadcast beb;
    private PrivateKey mKey;

    public ClientFrontend(Address inAddress, List<Address> addresses) {
        String consensus_folder;
        try {
            consensus_folder = new File("../private-key-client.key").getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mKey = getPrivateKey(consensus_folder);
        
        System.out.println("Client on port " + inAddress.toString());
        beb = new BEBroadcast(inAddress);
        for(int i = 0; i < addresses.size(); i++)
            beb.addServer(addresses.get(i));

        beb.subscribeDelivery(this);
        beb.send_handshake();
    }

    public void sendCommand(String message) {

        byte[] payload = message.getBytes();
        byte[] signature = sign(payload, mKey);
        System.out.println("FRONTEEEEEEEEEEEEEEEND" + beb.getInAddress().getProcessId());
        BEBSend send_event = new BEBSend(concatBytes(payload, signature));
        beb.send(send_event);
    }

    /* when we get responses from servers */
    @Override
    public void update(Event event) {
        String eventType = event.getEventName();

        if (!eventType.equals(BEBDeliver.EVENT_NAME)) {
            System.out.println("Should only receive deliver events (??)");
        }
        BEBDeliver ev = (BEBDeliver) event;
        
        byte[] byte_stream = ev.getPayload();
        String payload = new String(byte_stream, StandardCharsets.UTF_8);
        
        System.out.println("CONSENSUS RESULT: ");
        System.out.println(payload);
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
            System.out.println("ABCBSAHCBKSACBKJSCBKASCBSAKCBAKSJCB");
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
