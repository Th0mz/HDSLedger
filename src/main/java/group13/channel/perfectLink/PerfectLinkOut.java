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


    public PerfectLinkOut (int processId, Address destination) {
        this.processId = processId;
        this.destination = destination;

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

        // TODO : send method must include the process id in the paccket sent
        //byte[] packetData = new byte[data.length + 5];
        // int seqNum;

        // seqNum = r * 10 + processId;
        // seqNum = 1;
        // TODO : usedSeqNum.add(seqNum);

        // prepend type_of_message + sequence_number to message
        //packetData[0] = (byte) 0x00; //0x0 indicates that the message is original send
        //packetData[1] = (byte) (seqNum >> 24);
        //packetData[2] = (byte) (seqNum >> 16);
        //packetData[3] = (byte) (seqNum >> 8);
        //packetData[4] = (byte) seqNum;

        //System.arraycopy(data, 0, packetData, 4, data.length);
        DatagramPacket packet = new DatagramPacket(data, data.length, this.destination.getInet_address(), this.destination.getPort());
        // TODO : toBeSentPackets.put(seqNum, packet);

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
