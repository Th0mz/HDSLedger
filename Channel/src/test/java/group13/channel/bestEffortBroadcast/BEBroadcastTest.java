package group13.channel.bestEffortBroadcast;

import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.bestEffortBroadcast.events.BEBSend;
import group13.channel.perfectLink.PerfectLink;
import group13.primitives.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class BEBroadcastTest {

    private SendObject MESSAGE = new SendObject("test message");
    private SendObject PROCESS1_MESSAGE = new SendObject("process1");
    private SendObject PROCESS2_MESSAGE = new SendObject("process2");
    private SendObject PROCESS3_MESSAGE = new SendObject("process3");
    private SendObject PROCESS4_MESSAGE = new SendObject("process4");

    public static Address p1_addr, p2_addr, p3_addr, p4_addr;
    public static BEBroadcastTester process1, process2, process3, process4;
    public static AboveModuleListener am_process1, am_process2, am_process3, am_process4;

    @BeforeAll
    public static void init() {
        p1_addr = new Address(5000);
        p2_addr = new Address(5001);
        p3_addr = new Address(5002);
        p4_addr = new Address(5003);

        // broadcast module instances
        process1 = new BEBroadcastTester(p1_addr);
        process2 = new BEBroadcastTester(p2_addr);
        process3 = new BEBroadcastTester(p3_addr);
        process4 = new BEBroadcastTester(p4_addr);

        List<BEBroadcast> broadcast_modules = new ArrayList<>(List.of(process1, process2, process3, process4));

        for (BEBroadcast sender_module : broadcast_modules) {
            sender_module.addServer(process1.getInAddress());
            sender_module.addServer(process2.getInAddress());
            sender_module.addServer(process3.getInAddress());
            sender_module.addServer(process4.getInAddress());
        }

        am_process1 = new AboveModuleListener();
        am_process2 = new AboveModuleListener();
        am_process3 = new AboveModuleListener();
        am_process4 = new AboveModuleListener();

        // setup event architecture
        process1.subscribeDelivery(am_process1);
        process2.subscribeDelivery(am_process2);
        process3.subscribeDelivery(am_process3);
        process4.subscribeDelivery(am_process4);
    }

    @AfterEach
    public void cleanupTest () {

        am_process1.clean_events();
        am_process2.clean_events();
        am_process3.clean_events();
        am_process4.clean_events();

        // wait for packet in the network to disappear
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    public static void cleanupClass () {

        process1.close();
        process2.close();
        process3.close();
        process4.close();

        // wait for packet in the network to disappear
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Broadcast message and check reception")
    public void BroadcastMessageTest () {

        System.out.println("=============================================");
        System.out.println("Test : Broadcast message and check reception");
        // broadcast(message);
        BEBSend send_event = new BEBSend(MESSAGE);
        process1.send(send_event);

        // wait for messages to be received
        int delay = maxBoundForMessageRetransmission(1, 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // check if each above module received only one BEBDeliver event
        // with payload equal to the sent message (MESSAGE)
        List<AboveModuleListener> modules = new ArrayList<>(List.of(am_process1 ,am_process2, am_process3, am_process4));
        for (AboveModuleListener above_module : modules) {
            assertEquals(1, above_module.get_all_events_num());

            List<Event> received_events = above_module.get_events(BEBDeliver.EVENT_NAME);
            assertEquals(1, received_events.size());
            BEBDeliver deliver_event = (BEBDeliver) received_events.get(0);

            // check sender id
            assertTrue(MESSAGE.equals(deliver_event.getPayload()));
            assertTrue(p1_addr.getProcessId().equals(deliver_event.getProcessId()));
        }
    }


    @Test
    @DisplayName("All processes broadcast a message")
    public void AllBroadcastMessageTest () {

        System.out.println("========================================");
        System.out.println("Test : All processes broadcast a message");

        // broadcast(message);
        BEBSend send_event1 = new BEBSend(PROCESS1_MESSAGE);
        BEBSend send_event2 = new BEBSend(PROCESS2_MESSAGE);
        BEBSend send_event3 = new BEBSend(PROCESS3_MESSAGE);
        BEBSend send_event4 = new BEBSend(PROCESS4_MESSAGE);

        process1.send(send_event1);
        process2.send(send_event2);
        process3.send(send_event3);
        process4.send(send_event4);

        // wait for messages to be received
        int delay = maxBoundForMessageRetransmission(1, 2);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<BEBDeliver> expected_deliver = List.of(
                new BEBDeliver(p1_addr.getProcessId(), PROCESS1_MESSAGE),
                new BEBDeliver(p2_addr.getProcessId(), PROCESS2_MESSAGE),
                new BEBDeliver(p3_addr.getProcessId(), PROCESS3_MESSAGE),
                new BEBDeliver(p4_addr.getProcessId(), PROCESS4_MESSAGE)
        );

        // check if each above module received only one BEBDeliver event
        // with payload equal to the sent message (MESSAGE)
        List<AboveModuleListener> modules = new ArrayList<>(List.of(am_process1 ,am_process2, am_process3, am_process4));
        for (AboveModuleListener above_module : modules) {
            assertEquals(4, above_module.get_all_events_num());

            List<Event> received_events = above_module.get_events(BEBDeliver.EVENT_NAME);
            assertEquals(4, received_events.size());

            // check if the received packets are the expected ones
            for (BEBDeliver expected : expected_deliver) {
                boolean found = false;
                for (Event event : received_events) {
                    BEBDeliver received = (BEBDeliver) event;
                    if (received.equals(expected)) {
                        found = true;
                        break;
                    }
                }

                assertTrue(found);
            }
        }
    }


    public int maxBoundForMessageRetransmission (int retriesStart, int retriesEnd) {
        int timeout = PerfectLink.RETRANSMIT_DELTA * (retriesEnd - retriesStart + 1);

        if (retriesStart == 1) {
            retriesStart = 2;
        }

        for (int tries = retriesStart - 1; tries < retriesEnd; tries++) {
            timeout += 1 << tries + PerfectLink.RETRANSMIT_RATE;
        }

        return timeout;
    }


    @Test
    @DisplayName("Slow processes eventually receive messages")
    public void  SlowProcessTest () {

        System.out.println("=================================================");
        System.out.println("Test : Slow processes eventually receive messages");

        // make process 2 and process 3 network connection slow
        process2.setInProblems(p1_addr, true);
        process3.setInProblems(p1_addr, true);

        BEBSend send_event = new BEBSend(PROCESS1_MESSAGE);
        process1.send(send_event);

        // wait for messages to be received (and allow for
        // other messages to be retransmitted)
        int delay = maxBoundForMessageRetransmission(1, 2);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //check if process 1 and process 4 delivered the message
        List<AboveModuleListener> modules = new ArrayList<>(List.of(am_process1, am_process4));
        for (AboveModuleListener above_module : modules) {
            assertEquals(1, above_module.get_all_events_num());

            List<Event> received_events = above_module.get_events(BEBDeliver.EVENT_NAME);
            assertEquals(1, received_events.size());
            BEBDeliver deliver_event = (BEBDeliver) received_events.get(0);

            // check sender id
            assertTrue(PROCESS1_MESSAGE.equals(deliver_event.getPayload()));
            assertTrue(p1_addr.getProcessId().equals(deliver_event.getProcessId()));
        }

        assertEquals(0, am_process2.get_all_events_num());
        assertEquals(0, am_process3.get_all_events_num());

        // make process 2 and process 3 network connection slow
        process2.setInProblems(p1_addr, false);
        process3.setInProblems(p1_addr, false);

        // wait for retransmission
        delay = maxBoundForMessageRetransmission(3, 3);
        try {
            Thread.sleep(delay + 30);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //check if process 2 and process e delivered the message
        modules = new ArrayList<>(List.of(am_process2, am_process3));
        for (AboveModuleListener above_module : modules) {
            assertEquals(1, above_module.get_all_events_num());

            List<Event> received_events = above_module.get_events(BEBDeliver.EVENT_NAME);
            assertEquals(1, received_events.size());
            BEBDeliver deliver_event = (BEBDeliver) received_events.get(0);

            // check sender id
            assertTrue(PROCESS1_MESSAGE.equals(deliver_event.getPayload()));
            assertTrue(p1_addr.getProcessId().equals(deliver_event.getProcessId()));
        }
    }
}