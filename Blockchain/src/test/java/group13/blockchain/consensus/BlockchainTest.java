package group13.blockchain.consensus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import group13.blockchain.member.BMember;
import group13.primitives.Address;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;

public class BlockchainTest {

    /* public static String CONSENSUS_MESSAGE = "test message";
    public static String WRONG_MESSAGE = "wrong messageaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private static BMember server1, server2, server3;
    private static BMemberByzantine server4;
    private static IBFT ibft1, ibft2, ibft3;
    private static IBFTByzantine byz4;

    private static PrivateKey mKey;
    private static byte[] msg;
    private static byte[] signature;
    private static byte[] signedMessage, signedMessage2, signedMessage3;

    @BeforeAll
    public static void init() {

        String consensus_folder;
        try {
            consensus_folder = new File("../private-key-client.key").getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mKey = getPrivateKey(consensus_folder);

        msg = CONSENSUS_MESSAGE.getBytes();
        signature = sign(msg, mKey);
        signedMessage = concatBytes(msg, signature);
        signedMessage2 = concatBytes(new String(CONSENSUS_MESSAGE + "abx").getBytes(), sign(new String(CONSENSUS_MESSAGE + "abx").getBytes(),mKey));
        signedMessage3 = concatBytes(new String(CONSENSUS_MESSAGE + "xxx").getBytes(), sign(new String(CONSENSUS_MESSAGE + "xxx").getBytes(),mKey));


        Address server1_addr = new Address(2222);
        System.out.println("server 1 : " + server1_addr.getProcessId());
        Address server2_addr = new Address(3333);
        System.out.println("server 2 : " + server2_addr.getProcessId());
        Address server3_addr = new Address(4444);
        System.out.println("server 3 : " + server3_addr.getProcessId());
        Address server4_addr = new Address(5555);
        System.out.println("server 4 : " + server4_addr.getProcessId());


        ArrayList<Address> listOfServers = new ArrayList<>(List.of(
          server1_addr, server2_addr, server3_addr, server4_addr
        ));

        server1 = new BMember();
        server2 = new BMember();
        server3 = new BMember();
        server4 = new BMemberByzantine();

        String leaderId = server1_addr.getProcessId();
        server1.createBMember(listOfServers, 1, 4,
                new Address(2345), server1_addr, leaderId);
        server2.createBMember(listOfServers, 1, 4,
                new Address(3456), server2_addr, leaderId);
        server3.createBMember(listOfServers, 1, 4,
                new Address(4567), server3_addr, leaderId);
        // this will create a byzantine member
        server4.createBMember(listOfServers, 1, 4,
                new Address(5678), server4_addr, leaderId);
        
        ibft1 = server1.getConsensusObject();
        ibft2 = server2.getConsensusObject();
        ibft3 = server3.getConsensusObject();
        byz4 = server4.getConsensusObject();
    }

    @Test
    @DisplayName("Byzantine member fakes start")
    public void ByzantineStartTest () {
        byz4.setStartByzantine();
        byz4.start(0, WRONG_MESSAGE.getBytes());
        ibft1.start(0, signedMessage);
        

        try{
            Thread.sleep(1000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        server1.printLedger();
        server2.printLedger();
        server3.printLedger();

        assertEquals(CONSENSUS_MESSAGE, server1.getConsensusResult(0));
        assertEquals(CONSENSUS_MESSAGE, server2.getConsensusResult(0));
        assertEquals(CONSENSUS_MESSAGE, server3.getConsensusResult(0));
        byz4.clearAllByzantine();
    }

    
    @Test
    @DisplayName("Multiple Instances")
    public void MultipleInstancesTest () {
        byz4.setStartByzantine();
        byz4.start(0, WRONG_MESSAGE.getBytes());
        ibft1.start(0, signedMessage);
        ibft1.start(1, signedMessage2);
        ibft1.start(2, signedMessage3);


        try{
            Thread.sleep(2000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        server1.printLedger();
        server2.printLedger();
        server3.printLedger();

        assertEquals(CONSENSUS_MESSAGE, server1.getConsensusResult(0));
        assertEquals(CONSENSUS_MESSAGE, server2.getConsensusResult(0));
        assertEquals(CONSENSUS_MESSAGE, server3.getConsensusResult(0));
        assertEquals(CONSENSUS_MESSAGE + "abx", server1.getConsensusResult(1));
        assertEquals(CONSENSUS_MESSAGE + "abx", server2.getConsensusResult(1));
        assertEquals(CONSENSUS_MESSAGE + "abx", server3.getConsensusResult(1));
        assertEquals(CONSENSUS_MESSAGE + "xxx", server1.getConsensusResult(2));
        assertEquals(CONSENSUS_MESSAGE + "xxx", server2.getConsensusResult(2));
        assertEquals(CONSENSUS_MESSAGE + "xxx", server3.getConsensusResult(2));
        byz4.clearAllByzantine();
    }

    
    @Test
    @DisplayName("Byzantine member fakes preprepare")
    public void ByzantinePrePrepareTest () {
        byz4.setPrePrepareByzantine();
        ibft1.start(1, signedMessage);

        try{
            Thread.sleep(1000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(CONSENSUS_MESSAGE, server1.getConsensusResult(1));
        assertEquals(CONSENSUS_MESSAGE, server2.getConsensusResult(1));
        assertEquals(CONSENSUS_MESSAGE, server3.getConsensusResult(1));
        byz4.clearAllByzantine();
    }

    @Test
    @DisplayName("Byzantine member fakes prepare")
    public void ByzantinePrepareTest () {
        byz4.setPrepareByzantine();
        ibft1.start(2, signedMessage);

        try{
            Thread.sleep(1000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(CONSENSUS_MESSAGE, server1.getConsensusResult(2));
        assertEquals(CONSENSUS_MESSAGE, server2.getConsensusResult(2));
        assertEquals(CONSENSUS_MESSAGE, server3.getConsensusResult(2));
        byz4.clearAllByzantine();
    }

    @Test
    @DisplayName("Byzantine member fakes commit")
    public void ByzantineCommitTest () {
        byz4.setCommitByzantine();
        ibft1.start(3, signedMessage);

        try{
            Thread.sleep(1000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(CONSENSUS_MESSAGE, server1.getConsensusResult(3));
        assertEquals(CONSENSUS_MESSAGE, server2.getConsensusResult(3));
        assertEquals(CONSENSUS_MESSAGE, server3.getConsensusResult(3));
        byz4.clearAllByzantine();
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

    public static byte[] sign(byte[] message, PrivateKey privateKey) {
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

    public static byte [] concatBytes(byte[] a, byte[] b){
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    } */
}
