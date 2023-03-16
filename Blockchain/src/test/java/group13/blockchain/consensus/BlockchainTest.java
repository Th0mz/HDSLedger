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

    public BlockchainTest() {
        ArrayList<Address> listOfServers = new ArrayList<>(Arrays.asList(new Address[]{
            new Address(2222), new Address(3333), new Address(4444), new Address(5555) 
        }));

        server1 = new BMember();
        server2 = new BMember();
        server3 = new BMember();
        server4 = new BMemberByzantine();

        server1.createBMember(0, listOfServers, 1, 4, 
                            2345, new Address(2222), true);
        server2.createBMember(1, listOfServers, 1, 4, 
                            3456, new Address(3333), false);
        server3.createBMember(2, listOfServers, 1, 4, 
                            4567, new Address(4444), false);
        server4.createBMemberByzantine(3, listOfServers, 1, 4, 
                            5678, new Address(5555), false);
        
        ibft1 = server1.getConsensusObject();
        ibft2 = server2.getConsensusObject();
        ibft3 = server3.getConsensusObject();
        byz4 = server4.getConsensusObject();
        System.out.println("INITIALIZED BYZ4");
        System.out.println(byz4);
        System.out.println(server1);
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

        assertEquals(CONSENSUS_MESSAGE, server1.getConsensusResult(0));
        assertEquals(CONSENSUS_MESSAGE, server2.getConsensusResult(0));
        assertEquals(CONSENSUS_MESSAGE, server3.getConsensusResult(0));
        byz4.clearAllByzantine();
    }

}
