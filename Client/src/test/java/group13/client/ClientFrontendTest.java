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

    private static Address p1_addr, p2_addr, p3_addr, p4_addr;
    private static Address p1I_addr, p2I_addr, p3I_addr, p4I_addr;
    private static KeyPair p1_keys, p2_keys, p3_keys, p4_keys;
    private BMemberTester p1_bMember, p2_bMember, p3_bMember, p4_bMember;

    private static Address c1_addr, c2_addr, c3_addr;
    private static KeyPair c1_keys, c2_keys, c3_keys;
    private ClientFrontendTester c1_frontend, c2_frontend, c3_frontend;

    private int base = 5000;
    private int clientBase = 8000;

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
            c1_keys = keyPairGen.generateKeyPair();
            c2_keys = keyPairGen.generateKeyPair();
            c3_keys = keyPairGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setup() {

        

    }

    /* @AfterEach
    void cleanup() {

        // wait for all messages to be propagated
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        p1_bMember.close();
        p2_bMember.close();
        p3_bMember.close();
        p4_bMember.close();
        c1_frontend.close();
        c2_frontend.close();
        c3_frontend.close();
    } */


    @Test
    @DisplayName("Check handshake messages")
    public void CheckHandshakeMessagesTest () {
        System.out.println("===============================");
        System.out.println("Test : Check handshake messages");

        base += 10;
        clientBase += 10;
        p1_addr = new Address(base+4);
        p2_addr = new Address(base+5);
        p3_addr = new Address(base+6);
        p4_addr = new Address(base+7);

        p1I_addr = new Address(base);
        p2I_addr = new Address(base+1);
        p3I_addr = new Address(base+2);
        p4I_addr = new Address(base+3);

        c1_addr = new Address(clientBase);
        c2_addr = new Address(clientBase+1);
        c3_addr = new Address(clientBase+2);

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

        List<Address> interfaceAddresses = List.of(p1I_addr, p2I_addr, p3I_addr, p4I_addr);
        c1_frontend = new ClientFrontendTester(c1_addr, interfaceAddresses, serverPKs, c1_keys, 1, false);
        c1_frontend.setKeys();
        c2_frontend = new ClientFrontendTester(c2_addr, interfaceAddresses, serverPKs, c2_keys, 1, false);
        c2_frontend.setKeys();
        c3_frontend = new ClientFrontendTester(c3_addr, interfaceAddresses, serverPKs, c3_keys, 1, false);
        c3_frontend.setKeys();

        // wait for handshake to be propagated
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        c1_frontend.register();
        c2_frontend.register();
        c3_frontend.register();
        // wait for registers to reach the replicas
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        c1_frontend.transfer(c2_keys.getPublic(), 5);
        c1_frontend.transfer(c3_keys.getPublic(), 10);
        c2_frontend.transfer(c3_keys.getPublic(), 40);
        c3_frontend.checkBalance("s");


        // wait for all commands to be propagated
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // TODO : Check if the commands delivered are
        //  the supposed ones in each client
    }

    /** ----------------------------------------
     * ---          READ TESTS               ---
     * ----------------------------------------- */

    @Test
    @DisplayName("Check Strong Read")
    public void CheckStrongReadTest () {
        System.out.println("===============================");
        System.out.println("Test : Check Strong read");

        base += 20;
        clientBase += 20;
        p1_addr = new Address(base+4);
        p2_addr = new Address(base+5);
        p3_addr = new Address(base+6);
        p4_addr = new Address(base+7);

        p1I_addr = new Address(base);
        p2I_addr = new Address(base+1);
        p3I_addr = new Address(base+2);
        p4I_addr = new Address(base+3);

        c1_addr = new Address(clientBase);
        c2_addr = new Address(clientBase+1);
        c3_addr = new Address(clientBase+2);

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

        List<Address> interfaceAddresses = List.of(p1I_addr, p2I_addr, p3I_addr, p4I_addr);
        c1_frontend = new ClientFrontendTester(c1_addr, interfaceAddresses, serverPKs, c1_keys, 1, false);
        c1_frontend.setKeys();
        c2_frontend = new ClientFrontendTester(c2_addr, interfaceAddresses, serverPKs, c2_keys, 1, false);
        c2_frontend.setKeys();
        c3_frontend = new ClientFrontendTester(c3_addr, interfaceAddresses, serverPKs, c3_keys, 1, false);
        c3_frontend.setKeys();

        // wait for handshake to be propagated
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        c1_frontend.register();
        c2_frontend.register();
        c3_frontend.register();
        // wait for registers to reach the replicas
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("\n===\n REGISTER PHASE DONE \n===\n");
        c1_frontend.transfer(c2_keys.getPublic(), 5);
        c1_frontend.transfer(c3_keys.getPublic(), 10);
        c2_frontend.transfer(c3_keys.getPublic(), 40);



        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("\n===\n TRANSFER PHASE DONE \n===\n");
        c1_frontend.checkBalance("s");
        c1_frontend.checkBalance("s");
        c2_frontend.checkBalance("s");
        c3_frontend.checkBalance("s");

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(true, c1_frontend.getReadSuccessful());
        assertEquals(75, c1_frontend.getReadResult());
        assertEquals(true, c2_frontend.getReadSuccessful());
        assertEquals(60, c2_frontend.getReadResult());
        assertEquals(true, c3_frontend.getReadSuccessful());
        assertEquals(150, c3_frontend.getReadResult());

    }


    /** ----------------------------------------
     * --- DETECT BYZANTINE LEADER BEHAVIOR  ---
       ----------------------------------------- */

    @Test
    @DisplayName("Detect commands not signed by client")
    public void CheckClientSignature () {
        System.out.println("===============================");
        System.out.println("Test : Detect commands not signed by client");

        base += 30;
        clientBase += 30;
        p1_addr = new Address(base+4);
        p2_addr = new Address(base+5);
        p3_addr = new Address(base+6);
        p4_addr = new Address(base+7);

        p1I_addr = new Address(base);
        p2I_addr = new Address(base+1);
        p3I_addr = new Address(base+2);
        p4I_addr = new Address(base+3);

        c1_addr = new Address(clientBase);
        c2_addr = new Address(clientBase+1);
        c3_addr = new Address(clientBase+2);

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

        List<Address> interfaceAddresses = List.of(p1I_addr, p2I_addr, p3I_addr, p4I_addr);
        c1_frontend = new ClientFrontendTester(c1_addr, interfaceAddresses, serverPKs, c1_keys, 1, false);
        c1_frontend.setKeys();
        c2_frontend = new ClientFrontendTester(c2_addr, interfaceAddresses, serverPKs, c2_keys, 1, false);
        c2_frontend.setKeys();
        c3_frontend = new ClientFrontendTester(c3_addr, interfaceAddresses, serverPKs, c3_keys, 1, false);
        c3_frontend.setKeys();

        // wait for handshake to be propagated
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // set p1 to alter the client request
        p1_bMember.alterClientRequest = true;

        c1_frontend.register();
        c2_frontend.register();
        c3_frontend.register();
        // wait for registers to reach the replicas
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        c1_frontend.transfer(c2_keys.getPublic(), 5);
        c1_frontend.transfer(c3_keys.getPublic(), 10);
        c2_frontend.transfer(c3_keys.getPublic(), 40);

        // wait for all commands to be propagated
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // TODO : Check if the commands delivered are
        //  the supposed ones in each client
    }


    @Test
    @DisplayName("Detect repeated commands")
    public void CheckRepeatedCommands () {
        System.out.println("===============================");
        System.out.println("Test : Detect repeated commands");

        base += 444;
        clientBase += 444;
        p1_addr = new Address(base+4);
        p2_addr = new Address(base+5);
        p3_addr = new Address(base+6);
        p4_addr = new Address(base+7);

        p1I_addr = new Address(base);
        p2I_addr = new Address(base+1);
        p3I_addr = new Address(base+2);
        p4I_addr = new Address(base+3);

        c1_addr = new Address(clientBase);
        c2_addr = new Address(clientBase+1);
        c3_addr = new Address(clientBase+2);

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

        List<Address> interfaceAddresses = List.of(p1I_addr, p2I_addr, p3I_addr, p4I_addr);
        c1_frontend = new ClientFrontendTester(c1_addr, interfaceAddresses, serverPKs, c1_keys, 1, false);
        c1_frontend.setKeys();
        c2_frontend = new ClientFrontendTester(c2_addr, interfaceAddresses, serverPKs, c2_keys, 1, false);
        c2_frontend.setKeys();
        c3_frontend = new ClientFrontendTester(c3_addr, interfaceAddresses, serverPKs, c3_keys, 1, false);
        c3_frontend.setKeys();

        // wait for handshake to be propagated
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // set p1 to repeat commands
        p1_bMember.sendRepeatedCommands = true;

        c1_frontend.register();
        c2_frontend.register();
        c3_frontend.register();
        // wait for registers to reach the replicas
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        c1_frontend.transfer(c2_keys.getPublic(), 5);
        c1_frontend.transfer(c3_keys.getPublic(), 10);
        c2_frontend.transfer(c3_keys.getPublic(), 40);
        c3_frontend.checkBalance("s");


        // wait for all commands to be propagated
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // TODO : Check if the commands delivered are
        //  the supposed ones in each client

    }


    @Test
    @DisplayName("Detect commands for which there wasnt Preprepare")
    public void CheckDroppedCommands () {
        System.out.println("===============================");
        System.out.println("Test : Detect commands for which there wasnt Preprepare");

        base += 60;
        clientBase += 60;
        p1_addr = new Address(base+4);
        p2_addr = new Address(base+5);
        p3_addr = new Address(base+6);
        p4_addr = new Address(base+7);

        p1I_addr = new Address(base);
        p2I_addr = new Address(base+1);
        p3I_addr = new Address(base+2);
        p4I_addr = new Address(base+3);

        c1_addr = new Address(clientBase);
        c2_addr = new Address(clientBase+1);
        c3_addr = new Address(clientBase+2);

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

        List<Address> interfaceAddresses = List.of(p1I_addr, p2I_addr, p3I_addr, p4I_addr);
        c1_frontend = new ClientFrontendTester(c1_addr, interfaceAddresses, serverPKs, c1_keys, 1, false);
        c1_frontend.setKeys();
        c2_frontend = new ClientFrontendTester(c2_addr, interfaceAddresses, serverPKs, c2_keys, 1, false);
        c2_frontend.setKeys();
        c3_frontend = new ClientFrontendTester(c3_addr, interfaceAddresses, serverPKs, c3_keys, 1, false);
        c3_frontend.setKeys();

        // wait for handshake to be propagated
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        // set p1 to drop commands
        p1_bMember.dropCommands = true;
        c1_frontend.register();

        try {
            Thread.sleep(11000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertTrue(p2_bMember.getConsensusObject().leaderFailed);
        assertTrue(p3_bMember.getConsensusObject().leaderFailed);
        assertTrue(p4_bMember.getConsensusObject().leaderFailed);

        // TODO : Check if the commands delivered are
        //  the supposed ones in each client

    }
}