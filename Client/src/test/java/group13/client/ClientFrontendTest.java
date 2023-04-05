package group13.client;

import group13.blockchain.member.BMember;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.bestEffortBroadcast.events.BEBSend;
import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.primitives.Address;
import group13.primitives.Event;
import group13.prmitives.AboveModuleListener;
import group13.prmitives.BMemberTester;
import group13.prmitives.ClientFrontendTester;
import org.junit.jupiter.api.*;


import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientFrontendTest {

    private static Address client_addr, p1_addr, p2_addr, p3_addr, p4_addr;
    private static Address p1I_addr, p2I_addr, p3I_addr, p4I_addr;
    private static KeyPair client_keys, p1_keys, p2_keys, p3_keys, p4_keys;

    private BMemberTester p1_bMember, p2_bMember, p3_bMember, p4_bMember;
    private ClientFrontendTester clientFrontend;
    private int base;
    private int clientBase;

    public ClientFrontendTest() {
        base = 5000;
        clientBase = 9999;
    }

    @BeforeAll
    public static void init() {

        // Generate keys
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            p1_keys = keyPairGen.generateKeyPair();
            p2_keys = keyPairGen.generateKeyPair();
            p3_keys = keyPairGen.generateKeyPair();
            p4_keys = keyPairGen.generateKeyPair();
            client_keys = keyPairGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setup() {

        System.out.println("BEFORE -- BASE: " + base + " CLIENTBASE: " + clientBase);

        
        System.out.println("AFTER -- BASE: " + base + " CLIENTBASE: " + clientBase);

    }

    @AfterEach
    void cleanup() {
        /* p1_bMember.close();
        p2_bMember.close();
        p3_bMember.close();
        p4_bMember.close(); */
    }

    @Test
    @DisplayName("Check handshake messages")
    public void CheckHandshakeMessagesTest () {
        System.out.println("===============================");
        System.out.println("Test : Check handshake messages");

        p1_addr = new Address(base+4);
        p2_addr = new Address(base+5);
        p3_addr = new Address(base+6);
        p4_addr = new Address(base+7);

        p1I_addr = new Address(base);
        p2I_addr = new Address(base+1);
        p3I_addr = new Address(base+2);
        p4I_addr = new Address(base+3);

        client_addr = new Address(clientBase);

        List<Address> addresses = List.of(p1_addr, p2_addr, p3_addr, p4_addr);
        List<PublicKey> serverPKs = List.of(
                p1_keys.getPublic(), p2_keys.getPublic(), p3_keys.getPublic(), p4_keys.getPublic()
        );

        ArrayList<Address> serverList = new ArrayList<>(addresses);
        p1_bMember = new BMemberTester(); p1_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p1I_addr, p1_addr, p1_keys, p1_addr);
        p2_bMember = new BMemberTester(); p2_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p2I_addr, p2_addr, p2_keys, p1_addr);
        p3_bMember = new BMemberTester(); p3_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p3I_addr, p3_addr, p3_keys, p1_addr);
        p4_bMember = new BMemberTester(); p4_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p4I_addr, p4_addr, p4_keys, p1_addr);

        // wait for servers to stabilize
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        clientFrontend = new ClientFrontendTester(client_addr, addresses, serverPKs, client_keys, 1);

        // wait for handshake to be propagated
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        clientFrontend.register();
        clientFrontend.transfer(p2_keys.getPublic(), 5);
        clientFrontend.transfer(p3_keys.getPublic(), 10);
        clientFrontend.transfer(p4_keys.getPublic(), 15);
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();

        // wait for all commands to be propagated
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /** ----------------------------------------
     * --- DETECT BYZANTINE LEADER BEHAVIOR  ---
       ----------------------------------------- */ 
    @Test
    @DisplayName("Detect commands not signed by client")
    public void CheckClientSignature () {
        System.out.println("===============================");
        System.out.println("Test : Secondary replicas test client's signature");

        //Never clash on same ports
        base += 100;
        clientBase -= 1;

        p1_addr = new Address(base+4);
        p2_addr = new Address(base+5);
        p3_addr = new Address(base+6);
        p4_addr = new Address(base+7);

        p1I_addr = new Address(base);
        p2I_addr = new Address(base+1);
        p3I_addr = new Address(base+2);
        p4I_addr = new Address(base+3);

        client_addr = new Address(clientBase);

        

        List<Address> addresses = List.of(p1_addr, p2_addr, p3_addr, p4_addr);
        List<PublicKey> serverPKs = List.of(
                p1_keys.getPublic(), p2_keys.getPublic(), p3_keys.getPublic(), p4_keys.getPublic()
        );

        ArrayList<Address> serverList = new ArrayList<>(addresses);
        p1_bMember = new BMemberTester(); p1_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p1I_addr, p1_addr, p1_keys, p1_addr);
        p2_bMember = new BMemberTester(); p2_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p2I_addr, p2_addr, p2_keys, p1_addr);
        p3_bMember = new BMemberTester(); p3_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p3I_addr, p3_addr, p3_keys, p1_addr);
        p4_bMember = new BMemberTester(); p4_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p4I_addr, p4_addr, p4_keys, p1_addr);

        p1_bMember.alterClientRequest = true;
        // wait for servers to stabilize
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        clientFrontend = new ClientFrontendTester(client_addr, addresses, serverPKs, client_keys, 1);

        // wait for handshake to be propagated
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        clientFrontend.register();
        clientFrontend.transfer(p2_keys.getPublic(), 5);
        clientFrontend.transfer(p3_keys.getPublic(), 10);
        clientFrontend.transfer(p4_keys.getPublic(), 15);
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();

        // wait for all commands to be propagated
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertTrue(p2_bMember.getConsensusObject().leaderFailed);
        assertTrue(p3_bMember.getConsensusObject().leaderFailed);
        assertTrue(p4_bMember.getConsensusObject().leaderFailed);
    }


    @Test
    @DisplayName("Detect repeated commands")
    public void CheckRepeatedCommands () {
        System.out.println("===============================");
        System.out.println("Test : Secondary replicas test client's signature");

        //Never clash on same ports
        base += 200;
        clientBase -= 2;

        p1_addr = new Address(base+4);
        p2_addr = new Address(base+5);
        p3_addr = new Address(base+6);
        p4_addr = new Address(base+7);

        p1I_addr = new Address(base);
        p2I_addr = new Address(base+1);
        p3I_addr = new Address(base+2);
        p4I_addr = new Address(base+3);

        client_addr = new Address(clientBase);

        List<Address> addresses = List.of(p1_addr, p2_addr, p3_addr, p4_addr);
        List<PublicKey> serverPKs = List.of(
                p1_keys.getPublic(), p2_keys.getPublic(), p3_keys.getPublic(), p4_keys.getPublic()
        );

        ArrayList<Address> serverList = new ArrayList<>(addresses);
        p1_bMember = new BMemberTester(); p1_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p1I_addr, p1_addr, p1_keys, p1_addr);
        p2_bMember = new BMemberTester(); p2_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p2I_addr, p2_addr, p2_keys, p1_addr);
        p3_bMember = new BMemberTester(); p3_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p3I_addr, p3_addr, p3_keys, p1_addr);
        p4_bMember = new BMemberTester(); p4_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p4I_addr, p4_addr, p4_keys, p1_addr);

        p1_bMember.sendRepeatedCommands = true;
        // wait for servers to stabilize
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        clientFrontend = new ClientFrontendTester(client_addr, addresses, serverPKs, client_keys, 1);

        // wait for handshake to be propagated
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        clientFrontend.register();
        clientFrontend.transfer(p2_keys.getPublic(), 5);
        clientFrontend.transfer(p3_keys.getPublic(), 10);
        clientFrontend.transfer(p4_keys.getPublic(), 15);
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();

        // wait for all commands to be propagated
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertTrue(p2_bMember.getConsensusObject().leaderFailed);
        assertTrue(p3_bMember.getConsensusObject().leaderFailed);
        assertTrue(p4_bMember.getConsensusObject().leaderFailed);
    }


    @Test
    @DisplayName("Detect commands for which there wasnt Preprepare")
    public void CheckDroppedCommands () {
        System.out.println("===============================");
        System.out.println("Test : Secondary replicas test client's signature");

        //Never clash on same ports
        base += 300;
        clientBase -= 3;

        p1_addr = new Address(base+4);
        p2_addr = new Address(base+5);
        p3_addr = new Address(base+6);
        p4_addr = new Address(base+7);

        p1I_addr = new Address(base);
        p2I_addr = new Address(base+1);
        p3I_addr = new Address(base+2);
        p4I_addr = new Address(base+3);

        client_addr = new Address(clientBase);

        List<Address> addresses = List.of(p1_addr, p2_addr, p3_addr, p4_addr);
        List<PublicKey> serverPKs = List.of(
                p1_keys.getPublic(), p2_keys.getPublic(), p3_keys.getPublic(), p4_keys.getPublic()
        );

        ArrayList<Address> serverList = new ArrayList<>(addresses);
        p1_bMember = new BMemberTester(); p1_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p1I_addr, p1_addr, p1_keys, p1_addr);
        p2_bMember = new BMemberTester(); p2_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p2I_addr, p2_addr, p2_keys, p1_addr);
        p3_bMember = new BMemberTester(); p3_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p3I_addr, p3_addr, p3_keys, p1_addr);
        p4_bMember = new BMemberTester(); p4_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p4I_addr, p4_addr, p4_keys, p1_addr);

        p1_bMember.dropCommands = true;
        // wait for servers to stabilize
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        clientFrontend = new ClientFrontendTester(client_addr, addresses, serverPKs, client_keys, 1);

        // wait for handshake to be propagated
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        clientFrontend.register();
        clientFrontend.transfer(p2_keys.getPublic(), 5);
        clientFrontend.transfer(p3_keys.getPublic(), 10);
        clientFrontend.transfer(p4_keys.getPublic(), 15);
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();
        clientFrontend.checkBalance();

        // wait for all commands to be propagated
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertTrue(p2_bMember.getConsensusObject().leaderFailed);
        assertTrue(p3_bMember.getConsensusObject().leaderFailed);
        assertTrue(p4_bMember.getConsensusObject().leaderFailed);
    }


}