package group13.channel.bestEffortBroadcast;

import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.bestEffortBroadcast.events.BEBSend;
import group13.primitives.AboveModuleListener;
import group13.primitives.Address;
import group13.primitives.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BEBroadcastTest {

    public static String MESSAGE = "test message";

    public int received_messages;

    @BeforeEach
    void SetUp () {
        received_messages = 0;
    }

    @Test
    @DisplayName("Broadcast message and check reception")
    public void BroadcastMessageTest () {

        Address sender_addr = new Address(5000);

        // broadcast module instances
        BEBroadcast sender   = new BEBroadcast(sender_addr);
        BEBroadcast process1 = new BEBroadcast(new Address(5001));
        BEBroadcast process2 = new BEBroadcast(new Address(5002));
        BEBroadcast process3 = new BEBroadcast(new Address(5003));

        List<BEBroadcast> broadcast_modules = new ArrayList<>(List.of(sender, process1, process2, process3));

        for (BEBroadcast sender_module : broadcast_modules) {
            sender_module.addServer(sender.getInAddress());
            sender_module.addServer(process1.getInAddress());
            sender_module.addServer(process2.getInAddress());
            sender_module.addServer(process3.getInAddress());
        }

        // above module (am) specifications of the sender process
        AboveModuleListener am = new AboveModuleListener();

        // above module (am) specification of the receiver processes
        AboveModuleListener am_process1 = new AboveModuleListener();
        AboveModuleListener am_process2 = new AboveModuleListener();
        AboveModuleListener am_process3 = new AboveModuleListener();

        // setup event architecture
        sender.subscribeDelivery(am);
        process1.subscribeDelivery(am_process1);
        process2.subscribeDelivery(am_process2);
        process3.subscribeDelivery(am_process3);

        // broadcast(message);
        BEBSend send_event = new BEBSend(MESSAGE.getBytes());
        sender.send(send_event);

        // wait for messages to be received
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // check if each above module received only one BEBDeliver event
        // with payload equal to the sent message (MESSAGE)
        List<AboveModuleListener> modules = new ArrayList<>(List.of(am, am_process1 ,am_process2, am_process3));
        for (AboveModuleListener above_module : modules) {
            assertEquals(1, above_module.get_all_events_num());

            List<Event> received_events = above_module.get_events(BEBDeliver.EVENT_NAME);
            assertEquals(1, received_events.size());
            BEBDeliver deliver_event = (BEBDeliver) received_events.get(0);

            // check sender id
            assertTrue(Arrays.equals(deliver_event.getPayload(), MESSAGE.getBytes()));
            assertTrue(sender_addr.getProcessId().equals(deliver_event.getProcessId()));
        }

        sender.close();
        process1.close();
        process2.close();
        process3.close();
    }
}