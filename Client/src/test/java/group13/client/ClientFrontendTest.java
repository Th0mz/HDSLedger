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

    @BeforeAll
    public static void init() {
        p1_addr = new Address(6000);
        p2_addr = new Address(6001);
        p3_addr = new Address(6002);
        p4_addr = new Address(6003);

        p1I_addr = new Address(5000);
        p2I_addr = new Address(5001);
        p3I_addr = new Address(5002);
        p4I_addr = new Address(5003);

        client_addr = new Address(1234);

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

        clientFrontend = new ClientFrontendTester(client_addr, addresses, serverPKs, client_keys);

        // wait for handshake to be propagated
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void cleanup() {

    }

    @Test
    @DisplayName("Check handshake messages")
    public void CheckHandshakeMessagesTest () {
        System.out.println("===============================");
        System.out.println("Test : Check handshake messages");


        System.out.println("finish");
    }
}