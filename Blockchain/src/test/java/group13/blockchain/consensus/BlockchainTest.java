package group13.blockchain.consensus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import group13.blockchain.member.BMember;
import group13.primitives.Address;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;

public class BlockchainTest {

    public static String CONSENSUS_MESSAGE = "test message";
    public static String WRONG_MESSAGE = "wrong message";

    private static BMember server1, server2, server3;
    private static BMemberByzantine server4;
    private static IBFT ibft1, ibft2, ibft3;
    private static IBFTByzantine byz4;

    @BeforeAll
    public static void init() {

        Address server1_addr = new Address(2222);
        Address server2_addr = new Address(3333);
        Address server3_addr = new Address(4444);
        Address server4_addr = new Address(5555);

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
        byz4.start(0, WRONG_MESSAGE);
        ibft1.start(0, CONSENSUS_MESSAGE);

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
    @DisplayName("Byzantine member fakes preprepare")
    public void ByzantinePrePrepareTest () {
        byz4.setPrePrepareByzantine();
        ibft1.start(1, CONSENSUS_MESSAGE);

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
        ibft1.start(2, CONSENSUS_MESSAGE);

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
        ibft1.start(3, CONSENSUS_MESSAGE);

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

}
