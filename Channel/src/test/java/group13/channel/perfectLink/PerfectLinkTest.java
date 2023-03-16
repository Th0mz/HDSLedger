package group13.channel.perfectLink;

import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.primitives.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfectLinkTest {
    public static String MESSAGE = "test message";
    public int delivered_messages;

    @BeforeEach
    void SetUp () {
        delivered_messages = 0;
    }

    @Test
    @DisplayName("Message exchange between 2 end points")
    public void SendMessagesTest () {

        Address p1_addr = new Address("localhost", 5000);
        Address p2_addr = new Address("localhost", 5001);

        Network p1_network = new Network(p1_addr);
        Network p2_network = new Network(p2_addr);

        PerfectLink p1_to_p2 = p1_network.createLink(p2_addr);
        PerfectLink p2_to_p1 = p2_network.createLink(p1_addr);

        // above module process 1
        AboveModule am_process1 = new AboveModule();
        p1_to_p2.subscribeDelivery(am_process1.getEventListner());

        // above module process 2
        AboveModule am_process2 = new AboveModule();
        p2_to_p1.subscribeDelivery(am_process2.getEventListner());


        // process 1 sends a message to process 2
        p1_to_p2.send(MESSAGE.getBytes());

        // wait for messages to be received
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // check process1 received events
        AboveModuleListener el_process1 = am_process1.getEventListner();
        assertEquals(0, el_process1.get_all_events_num());

        // check process2 received events
        AboveModuleListener el_process2 = am_process2.getEventListner();
        assertEquals(1, el_process2.get_all_events_num());

        List<Event> received_events = el_process2.get_events(Pp2pDeliver.EVENT_NAME);
        assertEquals(1, received_events.size());
        Pp2pDeliver deliver_event = (Pp2pDeliver) received_events.get(0);
        assertTrue(Arrays.equals(deliver_event.getPayload(), MESSAGE.getBytes()));
        assertTrue(p1_addr.getProcessId().equals(deliver_event.getProcessId()));


         el_process1.clean_events();
         el_process2.clean_events();

        // process 2 sends a message to process 1
        p2_to_p1.send(MESSAGE.getBytes());

        // wait for messages to be received
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // check process1 received events
        assertEquals(1, el_process1.get_all_events_num());

        received_events = el_process1.get_events(Pp2pDeliver.EVENT_NAME);
        assertEquals(1, received_events.size());
        deliver_event = (Pp2pDeliver) received_events.get(0);
        assertTrue(Arrays.equals(deliver_event.getPayload(), MESSAGE.getBytes()));
        assertTrue(p2_addr.getProcessId().equals(deliver_event.getProcessId()));

        // check process2 received events
        assertEquals(0, el_process2.get_all_events_num());

        p1_network.close();
        p2_network.close();
    }

    @Test
    @DisplayName("Message retransmission because original message was lost/corrupted in the network")
    public void MessageRetransmissionOriginalLostTest() {
        Address p1_addr = new Address("localhost", 5000);
        Address p2_addr = new Address("localhost", 5001);

        NetworkTester p1_network = new NetworkTester(p1_addr);
        NetworkTester p2_network = new NetworkTester(p2_addr);

        PerfectLinkTester p1_to_p2 = p1_network.createTestLink(p2_addr, false);
        PerfectLinkTester p2_to_p1 = p2_network.createTestLink(p1_addr, false);

        // above module process 1
        AboveModule am_process1 = new AboveModule();
        p1_to_p2.subscribeDelivery(am_process1.getEventListner());

        // above module process 2
        AboveModule am_process2 = new AboveModule();
        p2_to_p1.subscribeDelivery(am_process2.getEventListner());

        // process 2 will not receive any message from process 1
        p2_to_p1.setInProblems(true);

        // process 1 send message to process
        p1_to_p2.send(MESSAGE.getBytes());

        // wait for two retransmissions
        try {
            Thread.sleep(PerfectLink.RETRANSMIT_DELTA * 2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // check process1 received events
        AboveModuleListener el_process1 = am_process1.getEventListner();
        assertEquals(0, el_process1.get_all_events_num());

        // check process2 received events (didn't receive any message)
        AboveModuleListener el_process2 = am_process2.getEventListner();
        assertEquals(0, el_process2.get_all_events_num());

        // check retransmit queue sizes
        assertEquals(1, p1_to_p2.getRetransmitQueueSize());
        assertEquals(0, p2_to_p1.getRetransmitQueueSize());

        // allow process 2 to receive the messages
        p2_to_p1.setInProblems(false);

        // wait for process 1 to retransmit
        try {
            Thread.sleep(PerfectLink.RETRANSMIT_DELTA * 2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // check process1 received events
        assertEquals(0, el_process1.get_all_events_num());

        // check process2 received events
        assertEquals(1, el_process2.get_all_events_num());

        List<Event> received_events = el_process2.get_events(Pp2pDeliver.EVENT_NAME);
        assertEquals(1, received_events.size());
        Pp2pDeliver deliver_event = (Pp2pDeliver) received_events.get(0);
        assertTrue(Arrays.equals(deliver_event.getPayload(), MESSAGE.getBytes()));
        assertTrue(p1_addr.getProcessId().equals(deliver_event.getProcessId()));


        // check retransmit queue sizes
        assertEquals(0, p1_to_p2.getRetransmitQueueSize());
        assertEquals(0, p2_to_p1.getRetransmitQueueSize());

        p1_network.close();
        p2_network.close();
    }

    @Test
    @DisplayName("Message retransmission because the other process socket is not open")
    public void MessageRetransmissionSocketNotOpenTest() {
        Address p1_addr = new Address("localhost", 5000);
        Address p2_addr = new Address("localhost", 5001);

        Network p1_network = new Network(p1_addr);
        PerfectLink p1_to_p2 = p1_network.createLink(p2_addr);

        // above module process 1
        AboveModule am_process1 = new AboveModule();
        p1_to_p2.subscribeDelivery(am_process1.getEventListner());

        // process 1 sends a message to process 2
        p1_to_p2.send(MESSAGE.getBytes());

        // wait for two retransmissions
        try {
            Thread.sleep(PerfectLink.RETRANSMIT_DELTA * 2 - 10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // +- 300ms (RETRANSMIT TIME) to create the other endpoint
        Network p2_network = new Network(p2_addr);
        PerfectLink p2_to_p1 = p2_network.createLink(p1_addr);

        // above module process 2
        AboveModule am_process2 = new AboveModule();
        p2_to_p1.subscribeDelivery(am_process2.getEventListner());

        // wait for the message to arrive and for the retransmit timer to
        // be triggered again. this time, no message will be retransmitted
        // because the process has acknowledged the receipt of the message.
        try {
            Thread.sleep(PerfectLink.RETRANSMIT_DELTA * 2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // check process1 received events
        AboveModuleListener el_process1 = am_process1.getEventListner();
        assertEquals(0, el_process1.get_all_events_num());

        // check process2 received events
        AboveModuleListener el_process2 = am_process2.getEventListner();
        assertEquals(1, el_process2.get_all_events_num());

        List<Event> received_events = el_process2.get_events(Pp2pDeliver.EVENT_NAME);
        assertEquals(1, received_events.size());
        Pp2pDeliver deliver_event = (Pp2pDeliver) received_events.get(0);
        assertTrue(Arrays.equals(deliver_event.getPayload(), MESSAGE.getBytes()));
        assertTrue(p1_addr.getProcessId().equals(deliver_event.getProcessId()));

        p1_network.close();
        p2_network.close();
    }

    @Test
    @DisplayName("Message retransmission because ACK was lost/corrupted in the network")
    public void MessageRetransmissionACKLostTest() {

        Address p1_addr = new Address("localhost", 5000);
        Address p2_addr = new Address("localhost", 5001);

        NetworkTester p1_network = new NetworkTester(p1_addr);
        NetworkTester p2_network = new NetworkTester(p2_addr);

        PerfectLinkTester p1_to_p2 = p1_network.createTestLink(p2_addr, false);
        PerfectLinkTester p2_to_p1 = p2_network.createTestLink(p1_addr, false);

        // above module process 1
        AboveModule am_process1 = new AboveModule();
        p1_to_p2.subscribeDelivery(am_process1.getEventListner());

        // above module process 2
        AboveModule am_process2 = new AboveModule();
        p2_to_p1.subscribeDelivery(am_process2.getEventListner());

        // process 1 send message to process
        p1_to_p2.send(MESSAGE.getBytes());

        // network problems will affect all packets
        // that process 2 tries to send to process 1
        // (process 2 will try to ack messages but
        // process 1 will never receive the deliver
        // confimation, so it will keep retransmitting)
        p1_to_p2.setInProblems(true);

        // wait for the message to arrive
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // check process1 received events
        AboveModuleListener el_process1 = am_process1.getEventListner();
        assertEquals(0, el_process1.get_all_events_num());

        // check process2 received events
        AboveModuleListener el_process2 = am_process2.getEventListner();
        assertEquals(1, el_process2.get_all_events_num());

        List<Event> received_events = el_process2.get_events(Pp2pDeliver.EVENT_NAME);
        assertEquals(1, received_events.size());
        Pp2pDeliver deliver_event = (Pp2pDeliver) received_events.get(0);
        assertTrue(Arrays.equals(deliver_event.getPayload(), MESSAGE.getBytes()));
        assertTrue(p1_addr.getProcessId().equals(deliver_event.getProcessId()));


        // check retransmit queue sizes
        assertEquals(1, p1_to_p2.getRetransmitQueueSize());
        assertEquals(0, p2_to_p1.getRetransmitQueueSize());

        // process 1 will retransmit the message again and
        // again (process 2 must not deliver the same message)
        try {
            Thread.sleep(PerfectLink.RETRANSMIT_DELTA * 2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // check process1 received events
        assertEquals(0, el_process1.get_all_events_num());

        // check process2 received events
        assertEquals(1, el_process2.get_all_events_num());

        received_events = el_process2.get_events(Pp2pDeliver.EVENT_NAME);
        assertEquals(1, received_events.size());
        deliver_event = (Pp2pDeliver) received_events.get(0);
        assertTrue(Arrays.equals(deliver_event.getPayload(), MESSAGE.getBytes()));
        assertTrue(p1_addr.getProcessId().equals(deliver_event.getProcessId()));


        // check retransmit queue sizes
        assertEquals(1, p1_to_p2.getRetransmitQueueSize());
        assertEquals(0, p2_to_p1.getRetransmitQueueSize());

        // process 1 can now receive the ack messages coming from process 2
        p1_to_p2.setInProblems(false);

        // wait for the ack to reach process 1
        try {
            Thread.sleep(PerfectLink.RETRANSMIT_DELTA);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // check process1 received events
        assertEquals(0, el_process1.get_all_events_num());

        // check process2 received events
        assertEquals(1, el_process2.get_all_events_num());

        received_events = el_process2.get_events(Pp2pDeliver.EVENT_NAME);
        assertEquals(1, received_events.size());
        deliver_event = (Pp2pDeliver) received_events.get(0);
        assertTrue(Arrays.equals(deliver_event.getPayload(), MESSAGE.getBytes()));
        assertTrue(p1_addr.getProcessId().equals(deliver_event.getProcessId()));


        // check retransmit queue sizes
        assertEquals(0, p1_to_p2.getRetransmitQueueSize());
        assertEquals(0, p2_to_p1.getRetransmitQueueSize());

        p1_network.close();
        p2_network.close();
    }
}