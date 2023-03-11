package group13.channel.perfectLink;

import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.channel.perfectLink.events.Pp2pSend;
import group13.primitives.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

        PerfectLink process1 = new PerfectLink(1, p1_addr);
        PerfectLink process2 = new PerfectLink(2, p2_addr);

        PerfectLinkOut out_process1 = process1.createLink(p2_addr);
        PerfectLinkOut out_process2 = process2.createLink(p1_addr);

        // above module process 1
        AboveModule am_process1 = new AboveModule();
        process1.subscribeDelivery(am_process1.getEventListner());
        am_process1.getEventHandler().subscribe(Pp2pSend.EVENT_NAME, out_process1);

        // above module process 2
        AboveModule am_process2 = new AboveModule();
        process2.subscribeDelivery(am_process2.getEventListner());
        am_process2.getEventHandler().subscribe(Pp2pSend.EVENT_NAME, out_process2);



        // process 1 sends a message to process 2
        Pp2pSend send_event = new Pp2pSend(MESSAGE);
        am_process1.getEventHandler().trigger(send_event);

        // wait for messages to be received
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
        assertTrue(deliver_event.getPayload().equals(MESSAGE));

         el_process1.clean_events();
         el_process2.clean_events();

        // process 2 sends a message to process 1
        am_process2.getEventHandler().trigger(send_event);

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
        assertTrue(deliver_event.getPayload().equals(MESSAGE));

        // check process2 received events
        assertEquals(0, el_process2.get_all_events_num());
    }
}