package group13.channel.perfectLink;

import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.primitives.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfectLinkTest {

    private SendObject MESSAGE = new SendObject("test message");

    private Address p1_addr, p2_addr;
    private NetworkTester p1_network, p2_network;
    private PerfectLinkTester p1_to_p2, p2_to_p1;

    private  AboveModule am_process1, am_process2;
    private AboveModuleListener el_process1, el_process2;

    @BeforeEach
    void setUp () {
        p1_addr = new Address("localhost", 5000);
        p2_addr = new Address("localhost", 5001);

        p1_network = new NetworkTester(p1_addr);
        p2_network = new NetworkTester(p2_addr);

        p1_to_p2 = p1_network.createLink(p2_addr);
        p2_to_p1 = p2_network.createLink(p1_addr);

        // above module process 1
        am_process1 = new AboveModule();
        p1_to_p2.subscribeDelivery(am_process1.getEventListner());
        el_process1 = am_process1.getEventListner();

        // above module process 2
        am_process2 = new AboveModule();
        p2_to_p1.subscribeDelivery(am_process2.getEventListner());
        el_process2 = am_process2.getEventListner();
    }

    @AfterEach
    void cleanUp () {
        // wait for packet in the network to disappear
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        p1_network.close();
        p2_network.close();
    }


    @Test
    @DisplayName("Message exchange between 2 end points")
    public void SendMessagesTest () {

        System.out.println("============================================");
        System.out.println("Test : Message exchange between 2 end points");
        // process 1 sends a message to process 2
        p1_to_p2.send(MESSAGE);

        // wait for messages to be received
        int delay = maxBoundForMessageRetransmission(1, 1);
        try {
            Thread.sleep(delay);
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
        assertTrue(MESSAGE.equals(deliver_event.getPayload()));
        assertTrue(p1_addr.getProcessId().equals(deliver_event.getProcessId()));

         el_process1.clean_events();
         el_process2.clean_events();

        // process 2 sends a message to process 1
        p2_to_p1.send(MESSAGE);

        // wait for messages to be received
        delay = maxBoundForMessageRetransmission(1, 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // check process1 received events
        assertEquals(1, el_process1.get_all_events_num());

        received_events = el_process1.get_events(Pp2pDeliver.EVENT_NAME);
        assertEquals(1, received_events.size());
        deliver_event = (Pp2pDeliver) received_events.get(0);
        assertTrue(MESSAGE.equals(deliver_event.getPayload()));
        assertTrue(p2_addr.getProcessId().equals(deliver_event.getProcessId()));

        // check process2 received events
        assertEquals(0, el_process2.get_all_events_num());
    }

    @Test
    @DisplayName("Message retransmission because original message was lost/corrupted in the network")
    public void MessageRetransmissionOriginalLostTest() {

        System.out.println("========================================================================================");
        System.out.println("Test : Message retransmission because original message was lost/corrupted in the network");

        // process 2 will not receive any message from process 1
        p2_to_p1.setInProblems(true);

        // process 1 send message to process
        p1_to_p2.send(MESSAGE);

        // wait for two retransmissions
        int delay = maxBoundForMessageRetransmission(1, 2) + 30;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // check process1 received events
        assertEquals(0, el_process1.get_all_events_num());

        // check process2 received events (didn't receive any message)
        assertEquals(0, el_process2.get_all_events_num());

        // check retransmit queue sizes
        assertEquals(1, p1_to_p2.getRetransmitQueueSize());
        assertEquals(0, p2_to_p1.getRetransmitQueueSize());

        // allow process 2 to receive the messages
        p2_to_p1.setInProblems(false);

        // wait for process 1 to retransmit
        delay = maxBoundForMessageRetransmission(3, 4);
        try {
            Thread.sleep(delay);
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
        assertTrue(MESSAGE.equals(deliver_event.getPayload()));
        assertTrue(p1_addr.getProcessId().equals(deliver_event.getProcessId()));


        // check retransmit queue sizes
        assertEquals(0, p1_to_p2.getRetransmitQueueSize());
        assertEquals(0, p2_to_p1.getRetransmitQueueSize());
    }

    @Test
    @DisplayName("Message retransmission because the other process socket is not open")
    public void MessageRetransmissionSocketNotOpenTest() {

        System.out.println("==========================================================================");
        System.out.println("Test : Message retransmission because the other process socket is not open");

        Address p3_addr = new Address("localhost", 6000);
        Address p4_addr = new Address("localhost", 6001);

        Network p3_network = new Network(p3_addr);
        PerfectLink p3_to_p4 = p3_network.createLink(p4_addr);

        // above module process 1
        AboveModule am_process1 = new AboveModule();
        p3_to_p4.subscribeDelivery(am_process1.getEventListner());

        // process 1 sends a message to process 2
        p3_to_p4.send(MESSAGE);

        // wait for two retransmissions
        int delay = maxBoundForMessageRetransmission(1, 2) + 30;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Network p4_network = new Network(p4_addr);
        PerfectLink p4_to_p3 = p4_network.createLink(p3_addr);

        // above module process 2
        AboveModule am_process2 = new AboveModule();
        p4_to_p3.subscribeDelivery(am_process2.getEventListner());

        // wait for the message to arrive and for the retransmit timer to
        // be triggered again. this time, no message will be retransmitted
        // because the process has acknowledged the receipt of the message.
        delay = maxBoundForMessageRetransmission(3, 4);
        try {
            Thread.sleep(delay);
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
        assertTrue(MESSAGE.equals(deliver_event.getPayload()));
        assertTrue(p3_addr.getProcessId().equals(deliver_event.getProcessId()));
    }

    @Test
    @DisplayName("Message retransmission because ACK was lost/corrupted in the network")
    public void MessageRetransmissionACKLostTest() {

        System.out.println("===========================================================================");
        System.out.println("Test : Message retransmission because ACK was lost/corrupted in the network");

        // process 1 send message to process
        p1_to_p2.send(MESSAGE);

        // network problems will affect all packets
        // that process 2 tries to send to process 1
        // (process 2 will try to ack messages but
        // process 1 will never receive the deliver
        // confimation, so it will keep retransmitting)
        p1_to_p2.setInProblems(true);

        // wait for the message to arrive
        int delay = maxBoundForMessageRetransmission(1, 1);
        try {
            Thread.sleep(delay);
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
        assertTrue(MESSAGE.equals(deliver_event.getPayload()));
        assertTrue(p1_addr.getProcessId().equals(deliver_event.getProcessId()));


        // check retransmit queue sizes
        assertEquals(1, p1_to_p2.getRetransmitQueueSize());
        assertEquals(0, p2_to_p1.getRetransmitQueueSize());

        // process 1 will retransmit the message again and
        // again (process 2 must not deliver the same message)
        delay = maxBoundForMessageRetransmission(2, 3);
        try {
            Thread.sleep(delay);
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
        assertTrue(MESSAGE.equals(deliver_event.getPayload()));
        assertTrue(p1_addr.getProcessId().equals(deliver_event.getProcessId()));


        // check retransmit queue sizes
        assertEquals(1, p1_to_p2.getRetransmitQueueSize());
        assertEquals(0, p2_to_p1.getRetransmitQueueSize());

        // process 1 can now receive the ack messages coming from process 2
        p1_to_p2.setInProblems(false);

        // wait for the ack to reach process 1
        delay = maxBoundForMessageRetransmission(1, 1);
        try {
            Thread.sleep(delay);
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
        assertTrue(MESSAGE.equals(deliver_event.getPayload()));
        assertTrue(p1_addr.getProcessId().equals(deliver_event.getProcessId()));


        // check retransmit queue sizes
        assertEquals(0, p1_to_p2.getRetransmitQueueSize());
        assertEquals(0, p2_to_p1.getRetransmitQueueSize());
    }

    public void perfectLinkSend(Address inAddress, Address outAddress, DatagramSocket outSocket, Object data, int sequenceNumber) {

        String senderId = inAddress.getProcessId();
        NetworkMessage message = new NetworkMessage(senderId, sequenceNumber, data, NetworkMessage.messageTypes.SEND);

        // Convert message object to byte stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] packetData = outputStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length,
                outAddress.getInetAddress(), outAddress.getPort());

        try {
            outSocket.send(packet);
        } catch (SocketException e) {
            // socket closed
        } catch (IOException e) {
            // TODO : must die? or just retry?
            System.out.println("Error : Unable to send packet (must die? or just retry?)");
            throw new RuntimeException(e);
        }

        // close open fd
        try {
            outputStream.close();
            objectOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Sliding window test")
    public void SlidingWindowTest () {

        System.out.println("============================================");
        System.out.println("Test : Sliding window test");

        // setup simple process 3 perfect link
        Address p3_addr = new Address(9999);
        DatagramSocket p3OutSocket = null;
        try {
            p3OutSocket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        PerfectLink p2_to_p3 =  p2_network.createLink(p3_addr);
        p2_to_p3.subscribeDelivery(am_process2.getEventListner());

        System.out.println(" > send messages from p3 to p2 with the following sequence numbers (1, 2, 4, 5)");
        perfectLinkSend(p3_addr, p2_addr, p3OutSocket, MESSAGE, 1);
        perfectLinkSend(p3_addr, p2_addr, p3OutSocket, MESSAGE, 1);
        perfectLinkSend(p3_addr, p2_addr, p3OutSocket, MESSAGE, 2);
        perfectLinkSend(p3_addr, p2_addr, p3OutSocket, MESSAGE, 4);
        perfectLinkSend(p3_addr, p2_addr, p3OutSocket, MESSAGE, 5);

        // check process2 received events
        assertEquals(0, el_process2.get_all_events_num());

        // wait for all packets to arrive
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println(" > send message with sequence number 0 (window base of p2) from p3 to p2");
        perfectLinkSend(p3_addr, p2_addr, p3OutSocket, MESSAGE, 0);

        // wait for all packets to arrive
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        // ckeck if process 2 delivered messages with sequence number 0, 1, 2
        assertEquals(3, el_process2.get_all_events_num());
        List<Event> received_events = el_process2.get_events(Pp2pDeliver.EVENT_NAME);
        assertEquals(3, received_events.size());

        // clean received events
        el_process2.clean_events();

        System.out.println(" > send messages from p3 to p2 with the following sequence numbers (1, 2, 4, 5, 6, 7, 8)");
        // out of the window frame
        perfectLinkSend(p3_addr, p2_addr, p3OutSocket, MESSAGE, 1);
        perfectLinkSend(p3_addr, p2_addr, p3OutSocket, MESSAGE, 2);

        // on the window frame and above
        perfectLinkSend(p3_addr, p2_addr, p3OutSocket, MESSAGE, 4);
        perfectLinkSend(p3_addr, p2_addr, p3OutSocket, MESSAGE, 5);
        perfectLinkSend(p3_addr, p2_addr, p3OutSocket, MESSAGE, 6);
        perfectLinkSend(p3_addr, p2_addr, p3OutSocket, MESSAGE, 7);
        perfectLinkSend(p3_addr, p2_addr, p3OutSocket, MESSAGE, 8);

        // wait for all packets to arrive
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println(" > send message with sequence number 3 (window base of p2) from p3 to p2");
        perfectLinkSend(p3_addr, p2_addr, p3OutSocket, MESSAGE, 3);

        assertEquals(0, el_process2.get_all_events_num());

        // wait for all packets to arrive
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(5, el_process2.get_all_events_num());
        received_events = el_process2.get_events(Pp2pDeliver.EVENT_NAME);
        assertEquals(5, received_events.size());

        p3OutSocket.close();
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
}