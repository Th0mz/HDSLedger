package group13.channel.perfectLink;

import group13.channel.perfectLink.events.Pp2pSend;
import group13.primitives.Address;
import group13.primitives.Event;
import group13.primitives.EventListener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Objects;

public class PerfectLinkOut implements EventListener {

    private int processId;
    private Address destination;
    private DatagramSocket send_socket;
    private int sequence_number;


    public PerfectLinkOut (int processId, Address destination) {
        this.processId = processId;
        this.destination = destination;
        this.sequence_number = 3;

        // create socket with any port number
        try {
            send_socket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("Error : Couldn't bind an address to the socket");
            throw new RuntimeException(e);
        }
    }

    // handle received events
    public void update(Event event) {
        String eventName = event.getEventName();
        if (eventName == Pp2pSend.EVENT_NAME) {
            Pp2pSend typed_event = (Pp2pSend) event;
            byte[] payload = typed_event.getPayload().getBytes();

            this.send(payload);
        }
    }

    public void send(byte[] data) {

        byte[] packetData = new byte[data.length + PerfectLink.HEADER_SIZE];

        // prepend type_of_message + sequence_number + process_id
        // original id being sent
        packetData[0] = (byte) 0x00;

        // sequence number
        packetData[1] = (byte) (this.sequence_number >> 24);
        packetData[2] = (byte) (this.sequence_number >> 16);
        packetData[3] = (byte) (this.sequence_number >> 8);
        packetData[4] = (byte) this.sequence_number;

        // process id
        packetData[5] = (byte) (this.processId >> 24);
        packetData[6] = (byte) (this.processId >> 16);
        packetData[7] = (byte) (this.processId >> 8);
        packetData[8] = (byte) this.processId;

        System.arraycopy(data, 0, packetData, PerfectLink.HEADER_SIZE, data.length);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, this.destination.getInet_address(), this.destination.getPort());

        try {
            send_socket.send(packet);
        } catch (IOException e) {
            // TODO : must die? or just retry?
            System.out.println("Error : Unable to send packet (must die? or just retry?)");
            throw new RuntimeException(e);
        }
    }

    public Address getDestination() {
        return destination;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PerfectLinkOut object = (PerfectLinkOut) o;
        Address object_address = object.getDestination();

        return object_address.getHostname().equals(this.destination.getHostname()) &&
                object_address.getPort() == this.destination.getPort();
    }

    public void close() {
        this.send_socket.close();
    }

    @Override
    public int hashCode() {
        return Objects.hash(destination);
    }
}
