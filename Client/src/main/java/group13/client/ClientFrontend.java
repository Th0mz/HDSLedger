package group13.client;

import group13.primitives.Address;
import group13.primitives.*;
import group13.blockchain.commands.BlockchainCommand;
import group13.blockchain.commands.CheckBalanceCommand;
import group13.blockchain.commands.RegisterCommand;
import group13.blockchain.commands.TransferCommand;
import group13.channel.bestEffortBroadcast.*;
import group13.channel.bestEffortBroadcast.events.*;
import group13.channel.perfectLink.PerfectLinkIn;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.spec.ECFieldF2m;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ClientFrontend implements EventListener {

    protected BEBroadcast beb;
    protected PrivateKey mKey;
    protected PublicKey myPubKey;
    protected int mySeqNum = 0;
    private ReentrantLock seqNumLock = new ReentrantLock();

    public ClientFrontend() {}

    public ClientFrontend(Address inAddress, List<Address> addresses, String pubKeyFile) {
        String keys_folder;
        String consensus_folder;

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

            System.out.print("Generated signedObject of type:"+(BlockchainCommand)signedObject.getObject()+"    with signature:"+signedObject.getSignature());
            BEBSend send_event = new BEBSend(signedObject);
            beb.send(send_event);
        } catch (IOException | InvalidKeyException | SignatureException | 
                NoSuchAlgorithmException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void register(){
        seqNumLock.lock();
        System.out.print("Registering client:"+ myPubKey);
        sendCommand(new RegisterCommand(mySeqNum, myPubKey));
        mySeqNum++;
        seqNumLock.unlock();
        //TODO: create Register object and send to server
    }

    public void transfer(PublicKey pKeyDest, int amount){
        seqNumLock.lock();
        System.out.print("Sending " + amount + "from "+ myPubKey + " to " + pKeyDest);
        sendCommand(new TransferCommand(mySeqNum, myPubKey, pKeyDest, amount));
        mySeqNum++;
        seqNumLock.unlock();
        //TODO: create Transfer object and send to server
    }

    public void checkBalance(){
        seqNumLock.lock();
        System.out.print("Checking balance of "+ myPubKey);
        sendCommand(new CheckBalanceCommand(mySeqNum, myPubKey));
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

        BEBDeliver ev = (BEBDeliver) event;
        System.out.println("received message");

        // TODO : display received message
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
}
