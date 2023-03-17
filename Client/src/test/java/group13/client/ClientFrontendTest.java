package group13.client;

import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.bestEffortBroadcast.events.BEBSend;
import group13.primitives.Address;
import group13.primitives.Event;
import group13.prmitives.AboveModuleListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientFrontendTest {

    private static String MESSAGE = "test";
    @Test
    @DisplayName("Check hanshake messages")
    public void CheckHandshakeMessagesTest () {
        Address client_addr = new Address(9876);
        Address p1_addr = new Address(5001);
        Address p2_addr = new Address(5002);

        BEBroadcast p1_beb = new BEBroadcast(new Address(5001));
        BEBroadcast p2_beb = new BEBroadcast(new Address(5002));

        p1_beb.addServer(p2_addr);
        p2_beb.addServer(p1_addr);

        AboveModuleListener am_process1 = new AboveModuleListener();
        AboveModuleListener am_process2 = new AboveModuleListener();

        p1_beb.subscribeDelivery(am_process1);
        p2_beb.subscribeDelivery(am_process2);

        // check if p1 broadcast is working
        p1_beb.send(new BEBSend(MESSAGE.getBytes()));

        // wait for messages to be received
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // check process1 received events
        assertEquals(0, am_process1.get_all_events_num());

        // check process2 received events
        assertEquals(1, am_process2.get_all_events_num());

        List<Event> received_events = am_process2.get_events(BEBDeliver.EVENT_NAME);
        assertEquals(1, received_events.size());
        BEBDeliver deliver_event = (BEBDeliver) received_events.get(0);

        // check sender id
        assertTrue(Arrays.equals(deliver_event.getPayload(), MESSAGE.getBytes()));
        assertTrue(p1_addr.getProcessId().equals(deliver_event.getProcessId()));

        am_process1.clean_events();
        am_process2.clean_events();

        // check if p2 broadcast is working
        p2_beb.send(new BEBSend(MESSAGE.getBytes()));

        // wait for messages to be received
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // check process1 received events
        assertEquals(1, am_process1.get_all_events_num());

        received_events = am_process1.get_events(BEBDeliver.EVENT_NAME);
        assertEquals(1, received_events.size());
        deliver_event = (BEBDeliver) received_events.get(0);

        // check sender id
        assertTrue(Arrays.equals(deliver_event.getPayload(), MESSAGE.getBytes()));
        assertTrue(p2_addr.getProcessId().equals(deliver_event.getProcessId()));

        // check process2 received events
        assertEquals(0, am_process2.get_all_events_num());

        am_process1.clean_events();
        am_process2.clean_events();


        ClientFrontend frontend = new ClientFrontend(client_addr, List.of(p1_addr, p2_addr));

        // TODO : frontend wait for handshake responses
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        p2_beb.unicast(client_addr.getProcessId(), "yoooo".getBytes());

        // TODO : frontend wait for handshake responses
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}