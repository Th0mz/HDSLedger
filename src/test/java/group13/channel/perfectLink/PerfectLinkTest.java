package group13.channel.perfectLink;

import group13.channel.primitives.Address;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.cert.TrustAnchor;
import java.sql.Struct;

class PerfectLinkTest {


    @Test
    @DisplayName("Send and check received message")
    public void CheckMessageTest () {
        Address in_addr = new Address("localhost", 5000);

        PerfectLinkIn in = new PerfectLinkIn(in_addr);
        PerfectLinkOut process1 = new PerfectLinkOut(in_addr);
        PerfectLinkOut process2 = new PerfectLinkOut(in_addr);

        byte[] message = "test message".getBytes();
        process1.send(message);
        process2.send(message);

        // TODO : eventListner that listens for the delivery of in channel

    }
}