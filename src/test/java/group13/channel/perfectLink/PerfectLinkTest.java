package group13.channel.perfectLink;

import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.primitives.AboveModuleListener;
import group13.primitives.Address;
import group13.primitives.Event;
import group13.primitives.EventListener;
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
    @DisplayName("Send two messages and check reception")
    public void SendMessagesTest () {

        Address in_addr = new Address("localhost", 5000);

        // point that receives all messages directed to in_address
        PerfectLinkIn in = new PerfectLinkIn(in_addr);
        // point that sends messages to in_address
        int out_process_id = 2;
        PerfectLinkOut process1 = new PerfectLinkOut(out_process_id, in_addr);

        AboveModuleListener above_module = new AboveModuleListener();
        in.subscribeDelivery(above_module);

        byte[] message_bytes = MESSAGE.getBytes();
        process1.send(message_bytes);

        // wait for messages to be received
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // TODO : more than to check the number of messages delivered
        // it must check if they come from the right senders (processId)
        assertEquals(1, above_module.get_all_events_num());

        List<Event> received_events = above_module.get_events(Pp2pDeliver.EVENT_NAME);
        assertEquals(1, received_events.size());
        Pp2pDeliver deliver_event = (Pp2pDeliver) received_events.get(0);
        assertTrue(deliver_event.getPayload().equals(MESSAGE));

        in.interrupt();
        in.close();
        process1.close();
    }
}