package group13.client;

import group13.blockchain.TES.ClientResponse;
import group13.blockchain.commands.CheckBalanceCommand;
import group13.blockchain.commands.RegisterCommand;
import group13.blockchain.commands.TransferCommand;
import group13.primitives.Address;
import group13.prmitives.AddressCounter;
import group13.prmitives.BMemberAndIBFTTester;
import group13.prmitives.BMemberTester;
import group13.prmitives.ClientFrontendTester;
import org.junit.jupiter.api.*;


import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
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

    public static AddressCounter addressCounter;

    @BeforeAll
    public static void init() {

        addressCounter = new AddressCounter(5000, 8000);
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
        int base = addressCounter.getReplicaAddress();
        p1_addr = new Address(base+4);
        p2_addr = new Address(base+5);
        p3_addr = new Address(base+6);
        p4_addr = new Address(base+7);

        p1I_addr = new Address(base);
        p2I_addr = new Address(base+1);
        p3I_addr = new Address(base+2);
        p4I_addr = new Address(base+3);

        int clientBase = addressCounter.getClientAddress();
        c1_addr = new Address(clientBase);
        c2_addr = new Address(clientBase+1);
        c3_addr = new Address(clientBase+2);

        addressCounter.generateNewAddresses(10);

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
    }

    @AfterEach
    void cleanup() {

        // wait for all messages to be propagated
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public boolean receivedAllExpected(ClientFrontendTester clientFrontend) {
        HashMap<Integer, ArrayList<ClientResponse>> responses = clientFrontend.getDeliveredResponses();
        HashMap<Integer, ClientResponse> expectedResponses = clientFrontend.getExpectedResponses();


        if (responses.size() != expectedResponses.size()) {
            System.out.println("not the same responses received");
            return false;
        }

        for (Integer sequenceNumber : expectedResponses.keySet()) {
            // check if the received responses have the expected one
            if (!responses.containsKey(sequenceNumber)) {
                System.out.println("expected response not received");
                return false;
            }

            // if it was only delivered once
            if (responses.get(sequenceNumber).size() != 1) {
                System.out.println("delivered more than once");
                return false;
            }

            ClientResponse expected = expectedResponses.get(sequenceNumber);
            ClientResponse received = responses.get(sequenceNumber).get(0);

            if (!expected.equals(received)) {
                System.out.println("for sequence number " + sequenceNumber +  " the expected and received responses weren't the same");
                System.out.println("Expected : " + expected);
                System.out.println("Received : " + received);
                return false;
            }
        }

        return true;
    }



    /** ----------------------------------------
     * ---          READ TESTS               ---
     * ----------------------------------------- */
 
    @Test
    @DisplayName("Check Strong read")
    public void CheckStrongReadTest () {
        System.out.println("===============================");
        System.out.println("Test : Check Strong read");

        c1_frontend.registerLogger(true);
        c1_frontend.registerLogger(false);
        c2_frontend.registerLogger(true);
        c3_frontend.registerLogger(true);
        // wait for registers to reach the replicas
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        c1_frontend.transferLogger(c2_keys.getPublic(), 5, true);
        c1_frontend.transferLogger(c3_keys.getPublic(), 10, true);
        c2_frontend.transferLogger(c3_keys.getPublic(), 40, true);

        // wait for all commands to be propagated
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        c1_frontend.checkBalanceLogger("s", 75, true);
        c2_frontend.checkBalanceLogger("s", 60, true);
        c3_frontend.checkBalanceLogger("s", 150, true);

        try {
            Thread.sleep(4500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("====== CHECK =======");
        // check if responses were delivered only once
        assertTrue(receivedAllExpected(c1_frontend));
        assertTrue(receivedAllExpected(c2_frontend));
        assertTrue(receivedAllExpected(c3_frontend));
    }

    @Test
    @DisplayName("Check Strong read Retry after no quorum")
    public void CheckStrongReadRetryTest () {
        System.out.println("===============================");
        System.out.println("Test : Check Strong read Retry after no quorum");
        
        //setting flag for fake random reads to be sent
        p1_bMember.setRead(true);
        p2_bMember.setRead(true);

        c1_frontend.registerLogger(true);
        c1_frontend.registerLogger(false);
        c2_frontend.registerLogger(true);
        c3_frontend.registerLogger(true);
        // wait for registers to reach the replicas
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        c1_frontend.transferLogger(c2_keys.getPublic(), 5, true);
        c1_frontend.transferLogger(c3_keys.getPublic(), 10, true);
        

        // wait for all commands to be propagated
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }   

        

        c1_frontend.checkBalance("s");
        c2_frontend.checkBalance("s");
        c3_frontend.checkBalance("s");

        
        
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        boolean read_r1 = c1_frontend.retryStrongRead();
        boolean read_r2 = c2_frontend.retryStrongRead();
        boolean read_r3 = c3_frontend.retryStrongRead();

        String reason_1 = c1_frontend.getReasonFailed();
        String reason_2 = c2_frontend.getReasonFailed();
        String reason_3 = c3_frontend.getReasonFailed();

        //allow for real reads to be sent again
        p1_bMember.setRead(false);
        p2_bMember.setRead(false);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        System.out.println("====== CHECK =======");
        assertEquals("NO_QUORUM", reason_1);
        assertEquals("NO_QUORUM", reason_2);
        assertEquals("NO_QUORUM", reason_3);
        assertEquals(true, read_r1);
        assertEquals(true, read_r1 == read_r2 == read_r3);
        assertEquals(75, c1_frontend.getReadResult());
        assertEquals(105, c2_frontend.getReadResult());
        assertEquals(110, c3_frontend.getReadResult());
        
    }
    

    @Test
    @DisplayName("Check Strong read waits for blocks in consenus")
    public void CheckStrongReadAwaitsTest () {
        System.out.println("===============================");
        System.out.println("Test : Check Strong read waits for blocks in consenus");
        
        //setting flag for fake read function to work
        p1_bMember.setRead(true);
        p2_bMember.setRead(true);
        p3_bMember.setRead(true);
        p4_bMember.setRead(true);

        c1_frontend.registerLogger(true);
        c1_frontend.registerLogger(false);
        c2_frontend.registerLogger(true);

        // wait for registers to reach the replicas
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        c1_frontend.transferLogger(c2_keys.getPublic(), 5, true);
        
        

        // wait for all commands to be propagated
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }   

        //Artificially decrease last applied to simulate block in consensus when read received
        p1_bMember.decreaseLastApplied();
        p2_bMember.decreaseLastApplied();
        p3_bMember.decreaseLastApplied();
        p4_bMember.decreaseLastApplied();
        

        c1_frontend.checkBalance("s");
        c2_frontend.checkBalance("s");
        
        //time for reads to be processed (put on wait)
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //trigger the reads that were awaiting 
        p1_bMember.deliver(p1_bMember.getLastApplied() + 1);
        p2_bMember.deliver(p2_bMember.getLastApplied() + 1);
        p3_bMember.deliver(p3_bMember.getLastApplied() + 1);
        p4_bMember.deliver(p4_bMember.getLastApplied() + 1);

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        
        System.out.println("====== CHECK =======");
        assertEquals(true, c1_frontend.getReadSuccessful());
        assertEquals(true, c2_frontend.getReadSuccessful());
        assertEquals(null, c1_frontend.getReasonFailed());
        assertEquals(null, c2_frontend.getReasonFailed());
        assertEquals(90, c1_frontend.getReadResult());
        assertEquals(105, c2_frontend.getReadResult());
      
        
    }


    @Test
    @DisplayName("Check Weak read")
    public void CheckWeakReadTest () {
        System.out.println("===============================");
        System.out.println("Test : Check Weak read");

        c1_frontend.registerLogger(true);
        c1_frontend.registerLogger(false);
        c2_frontend.registerLogger(true);
        c3_frontend.registerLogger(true);
        // wait for registers to reach the replicas
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        c1_frontend.transferLogger(c2_keys.getPublic(), 5, true);
        c1_frontend.transferLogger(c3_keys.getPublic(), 10, true);
        c2_frontend.transferLogger(c3_keys.getPublic(), 40, true);

        // wait for all commands to be propagated
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        c1_frontend.checkBalance("w");
        c2_frontend.checkBalance("w");
        c3_frontend.checkBalance("w");

        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("====== CHECK =======");
        assertEquals(null, c1_frontend.getReasonFailed());
        assertEquals(null, c2_frontend.getReasonFailed());
        assertEquals(null, c3_frontend.getReasonFailed());

        assertEquals(75, c1_frontend.getReadResult());
        assertEquals(60, c2_frontend.getReadResult());
        assertEquals(150, c3_frontend.getReadResult());
    }


    @Test
    @DisplayName("Check Weak read - Only 1 Node Responds")
    public void CheckWeakReadOneNodeTest () {
        System.out.println("===============================");
        System.out.println("Test : Check Weak read - Only 1 Node Responds");

        //setting flag for fake read function to work
        p1_bMember.setRead(true);
        p2_bMember.setRead(true);
        p3_bMember.setRead(true);
        p4_bMember.setRead(true);

        c1_frontend.registerLogger(true);
        c1_frontend.registerLogger(false);
        c2_frontend.registerLogger(true);
        c3_frontend.registerLogger(true);
        // wait for registers to reach the replicas
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        c1_frontend.transferLogger(c2_keys.getPublic(), 5, true);
        c1_frontend.transferLogger(c3_keys.getPublic(), 10, true);
        c2_frontend.transferLogger(c3_keys.getPublic(), 40, true);

        // wait for all commands to be propagated
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        p1_bMember.respondWeakRead(false);
        p2_bMember.respondWeakRead(false);
        p4_bMember.respondWeakRead(false);

        c1_frontend.checkBalance("w");
        c2_frontend.checkBalance("w");
        c3_frontend.checkBalance("w");

        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("====== CHECK =======");
        assertEquals(null, c1_frontend.getReasonFailed());
        assertEquals(null, c2_frontend.getReasonFailed());
        assertEquals(null, c3_frontend.getReasonFailed());
        assertEquals(1, c1_frontend.getNrResponsesWeakRead());
        assertEquals(1, c2_frontend.getNrResponsesWeakRead());
        assertEquals(1, c3_frontend.getNrResponsesWeakRead());
        assertEquals(75, c1_frontend.getReadResult());
        assertEquals(60, c2_frontend.getReadResult());
        assertEquals(150, c3_frontend.getReadResult());
    } 

    @Test
    @DisplayName("Check Weak read 1 node - False Signatures ")
    public void CheckWeakReadOneNodeFalseSignaturesTest () {
        System.out.println("===============================");
        System.out.println("Test : Check Weak read 1 node - False Signatures ");

        //setting flag for fake read function to work
        p1_bMember.setRead(true);
        p2_bMember.setRead(true);
        p3_bMember.setRead(true);
        p4_bMember.setRead(true);

        c1_frontend.registerLogger(true);
        c1_frontend.registerLogger(false);
        c2_frontend.registerLogger(true);
        c3_frontend.registerLogger(true);
        // wait for registers to reach the replicas
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        c1_frontend.transferLogger(c2_keys.getPublic(), 5, true);
        c1_frontend.transferLogger(c3_keys.getPublic(), 10, true);
        c2_frontend.transferLogger(c3_keys.getPublic(), 40, true);

        // wait for all commands to be propagated
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Only 1 node will respond but with fake signatures so no valid read
        p1_bMember.respondWeakRead(false);
        p2_bMember.respondWeakRead(false);
        p4_bMember.respondWeakRead(false);

        p3_bMember.falsify_signatures(true);

        c1_frontend.checkBalance("w");
        c2_frontend.checkBalance("w");
        c3_frontend.checkBalance("w");

        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("====== CHECK =======");
        

        //will receive 1 response but invalid
        assertEquals(1, c1_frontend.getNrResponsesWeakRead());
        assertEquals(1, c2_frontend.getNrResponsesWeakRead());
        assertEquals(1, c3_frontend.getNrResponsesWeakRead());

        assertEquals(false, c1_frontend.getReadSuccessful());
        assertEquals(false, c2_frontend.getReadSuccessful());
        assertEquals(false, c3_frontend.getReadSuccessful());

        assertEquals("FAILED_SIGNATURES", c1_frontend.getReasonFailed());
        assertEquals("FAILED_SIGNATURES", c2_frontend.getReasonFailed());
        assertEquals("FAILED_SIGNATURES", c3_frontend.getReasonFailed());

        // -100 means invalid read
        assertEquals(-100, c1_frontend.getReadResult());
        assertEquals(-100, c2_frontend.getReadResult());
        assertEquals(-100, c3_frontend.getReadResult());
    } 


    /** ----------------------------------------
     * --- DETECT BYZANTINE LEADER BEHAVIOR  ---
       ----------------------------------------- */

       
    @Test
    @DisplayName("Detect commands not signed by client")
    public void CheckClientSignature () {
        System.out.println("===============================");
        System.out.println("Test : Detect commands not signed by client");

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

        assertTrue(p2_bMember.getConsensusObject().leaderFailed);
        assertTrue(p3_bMember.getConsensusObject().leaderFailed);
        assertTrue(p4_bMember.getConsensusObject().leaderFailed);

    }


    @Test
    @DisplayName("Detect repeated commands")
    public void CheckRepeatedCommands () {
        System.out.println("===============================");
        System.out.println("Test : Detect repeated commands");

        // set p1 to repeat commands
        p1_bMember.sendRepeatedCommands = true;

        c1_frontend.registerLogger(true);
        c2_frontend.registerLogger(true);
        c3_frontend.registerLogger(true);
        // wait for registers to reach the replicas
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        c1_frontend.transferLogger(c2_keys.getPublic(), 5, true);
        c1_frontend.transferLogger(c3_keys.getPublic(), 10, true);
        c2_frontend.transferLogger(c3_keys.getPublic(), 40, true);

        // wait for all commands to be propagated
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        c1_frontend.checkBalanceLogger("s", 75, true);
        c2_frontend.checkBalanceLogger("s", 60, true);
        c3_frontend.checkBalanceLogger("s", 150, true);

        // wait for all commands to be propagated
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        System.out.println("====== CHECK =======");
        // check if responses were delivered only once
        assertTrue(receivedAllExpected(c1_frontend));
        assertTrue(receivedAllExpected(c2_frontend));
        assertTrue(receivedAllExpected(c3_frontend));

    }


    @Test
    @DisplayName("Detect commands for which there wasnt Preprepare")
    public void CheckDroppedCommands () {
        System.out.println("===============================");
        System.out.println("Test : Detect commands for which there wasnt Preprepare");

        // set p1 to drop commands
        p1_bMember.dropCommands = true;
        c1_frontend.register();
        c2_frontend.register();

        c1_frontend.checkBalance("s");
        c2_frontend.checkBalance("s");
        c1_frontend.checkBalance("w");
        c2_frontend.checkBalance("w");

        try {
            Thread.sleep(11000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertTrue(p2_bMember.getConsensusObject().leaderFailed);
        assertTrue(p3_bMember.getConsensusObject().leaderFailed);
        assertTrue(p4_bMember.getConsensusObject().leaderFailed);
    }
    

    /** ----------------------------------------
     * ---         BYZANTINE REPLICAS        ---
     * ----------------------------------------- */

     
    @Test
    @DisplayName("Test when replica sends repeated commands")
    public void EndureRepeatedCommands () {
        System.out.println("===============================");
        System.out.println("Test : Test when replica sends repeated commands");

        addressCounter.generateNewAddresses(10);
        // set p1 to drop commands
        int base = addressCounter.getReplicaAddress();
        p1_addr = new Address(base+4);
        p2_addr = new Address(base+5);
        p3_addr = new Address(base+6);
        p4_addr = new Address(base+7);

        p1I_addr = new Address(base);
        p2I_addr = new Address(base+1);
        p3I_addr = new Address(base+2);
        p4I_addr = new Address(base+3);

        int clientBase = addressCounter.getClientAddress();
        c1_addr = new Address(clientBase);
        c2_addr = new Address(clientBase+1);
        c3_addr = new Address(clientBase+2);

        addressCounter.generateNewAddresses(10);

        List<Address> addresses = List.of(p1_addr, p2_addr, p3_addr, p4_addr);
        List<PublicKey> serverPKs = List.of(
                p1_keys.getPublic(), p2_keys.getPublic(), p3_keys.getPublic(), p4_keys.getPublic()
        );

        ArrayList<Address> serverList = new ArrayList<>(addresses);
        p1_bMember = new BMemberAndIBFTTester(); p1_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p1I_addr, p1_addr, p1_keys, p1_addr);
        p2_bMember = new BMemberTester(); p2_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p2I_addr, p2_addr, p2_keys, p1_addr);
        p3_bMember = new BMemberTester(); p3_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p3I_addr, p3_addr, p3_keys, p1_addr);
        p4_bMember = new BMemberTester(); p4_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p4I_addr, p4_addr, p4_keys, p1_addr);
        ((BMemberAndIBFTTester)p1_bMember).setByzantineRepeatedCommands();
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
        
        c1_frontend.register();
        c2_frontend.register();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        c1_frontend.transfer(c2_keys.getPublic(), 10);

        c1_frontend.checkBalance("s");
        c2_frontend.checkBalance("s");
        c1_frontend.checkBalance("w");
        c2_frontend.checkBalance("w");

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(110, p2_bMember.getClientBalance(c2_keys.getPublic()));
        assertEquals(85, p2_bMember.getClientBalance(c1_keys.getPublic()));
        //assertEquals(2, p3_bMember.getLedgerSize());
        //assertEquals(2, p4_bMember.getLedgerSize());
    }


    @Test
    @DisplayName("Test when replica supresses commands")
    public void EndureSuppressedCommands () {
        System.out.println("===============================");
        System.out.println("Test : Test when replica supresses commands");

        addressCounter.generateNewAddresses(10);
        // set p1 to drop commands
        int base = addressCounter.getReplicaAddress();
        p1_addr = new Address(base+4);
        p2_addr = new Address(base+5);
        p3_addr = new Address(base+6);
        p4_addr = new Address(base+7);

        p1I_addr = new Address(base);
        p2I_addr = new Address(base+1);
        p3I_addr = new Address(base+2);
        p4I_addr = new Address(base+3);

        int clientBase = addressCounter.getClientAddress();
        c1_addr = new Address(clientBase);
        c2_addr = new Address(clientBase+1);
        c3_addr = new Address(clientBase+2);

        addressCounter.generateNewAddresses(10);

        List<Address> addresses = List.of(p1_addr, p2_addr, p3_addr, p4_addr);
        List<PublicKey> serverPKs = List.of(
                p1_keys.getPublic(), p2_keys.getPublic(), p3_keys.getPublic(), p4_keys.getPublic()
        );

        ArrayList<Address> serverList = new ArrayList<>(addresses);
        p1_bMember = new BMemberAndIBFTTester(); p1_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p1I_addr, p1_addr, p1_keys, p1_addr);
        p2_bMember = new BMemberTester(); p2_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p2I_addr, p2_addr, p2_keys, p1_addr);
        p3_bMember = new BMemberTester(); p3_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p3I_addr, p3_addr, p3_keys, p1_addr);
        p4_bMember = new BMemberTester(); p4_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p4I_addr, p4_addr, p4_keys, p1_addr);
        ((BMemberAndIBFTTester)p1_bMember).setByzantineSuppressCommands();
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
        
        c1_frontend.register();
        c2_frontend.register();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        c1_frontend.transfer(c2_keys.getPublic(), 10);

        c1_frontend.checkBalance("s");
        c2_frontend.checkBalance("s");
        c1_frontend.checkBalance("w");
        c2_frontend.checkBalance("w");

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(110, p2_bMember.getClientBalance(c2_keys.getPublic()));
        assertEquals(85, p2_bMember.getClientBalance(c1_keys.getPublic()));
        //assertEquals(2, p3_bMember.getLedgerSize());
        //assertEquals(2, p4_bMember.getLedgerSize());
    }


    @Test
    @DisplayName("Test when replica supresses commands")
    public void ByzantineTriesConsensusAlone () {
        System.out.println("===============================");
        System.out.println("Test : Test when replica supresses commands");

        addressCounter.generateNewAddresses(10);
        // set p1 to drop commands
        int base = addressCounter.getReplicaAddress();
        p1_addr = new Address(base+4);
        p2_addr = new Address(base+5);
        p3_addr = new Address(base+6);
        p4_addr = new Address(base+7);

        p1I_addr = new Address(base);
        p2I_addr = new Address(base+1);
        p3I_addr = new Address(base+2);
        p4I_addr = new Address(base+3);

        int clientBase = addressCounter.getClientAddress();
        c1_addr = new Address(clientBase);
        c2_addr = new Address(clientBase+1);
        c3_addr = new Address(clientBase+2);

        addressCounter.generateNewAddresses(10);

        List<Address> addresses = List.of(p1_addr, p2_addr, p3_addr, p4_addr);
        List<PublicKey> serverPKs = List.of(
                p1_keys.getPublic(), p2_keys.getPublic(), p3_keys.getPublic(), p4_keys.getPublic()
        );

        ArrayList<Address> serverList = new ArrayList<>(addresses);
        p1_bMember = new BMemberAndIBFTTester(); p1_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p1I_addr, p1_addr, p1_keys, p1_addr);
        p2_bMember = new BMemberAndIBFTTester(); p2_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p2I_addr, p2_addr, p2_keys, p1_addr);
        p3_bMember = new BMemberAndIBFTTester(); p3_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p3I_addr, p3_addr, p3_keys, p1_addr);
        p4_bMember = new BMemberAndIBFTTester(); p4_bMember.createBMemberTester(serverList, serverPKs, 1, 4, p4I_addr, p4_addr, p4_keys, p1_addr);
        ((BMemberAndIBFTTester)p1_bMember).setByzantineRepeatedCommands();
        ((BMemberAndIBFTTester)p2_bMember).setByzantineSuppressCommands();
        ((BMemberAndIBFTTester)p3_bMember).setByzantineSuppressCommands();
        ((BMemberAndIBFTTester)p4_bMember).setByzantineSuppressCommands();
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
        
        c1_frontend.register();
        c2_frontend.register();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        c1_frontend.transfer(c2_keys.getPublic(), 10);

        c1_frontend.checkBalance("s");
        c2_frontend.checkBalance("s");
        c1_frontend.checkBalance("w");
        c2_frontend.checkBalance("w");

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(4, p2_bMember.getNumberClients());
        assertEquals(4, p3_bMember.getNumberClients());
        assertEquals(4, p3_bMember.getNumberClients());
    }
    





    /** ----------------------------------------
     * ---          CLIENT TESTS               ---
     * ----------------------------------------- */
    
    @Test
    @DisplayName("Byzantine replica tries to force client delivery by sending 2f+1 equal responses")
    public void ReplicaForcesClientDelivery() {

        // client sends a check balance command with sequence number 0
        c1_frontend.fakeCommandSend(0);

        // try to deliver fake check balance to the client
        float fakeBalance = 10000;
        CheckBalanceCommand balanceCommand = new CheckBalanceCommand(0, c1_keys.getPublic(), 1, true);
        ClientResponse response = new ClientResponse(balanceCommand, fakeBalance, true);
        p1_bMember.forceClientResponse(response);

        // wait for responses to be propagated
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("====== CHECK =======");
        HashMap<Integer, ArrayList<ClientResponse>> deliveredResponses =  c1_frontend.getDeliveredResponses();
        assertEquals(0, deliveredResponses.size());
    }
}