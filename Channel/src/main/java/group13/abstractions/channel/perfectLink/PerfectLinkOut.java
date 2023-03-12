package group13.channel.perfectLink;

import com.sun.source.tree.Tree;
import group13.channel.perfectLink.events.Pp2pSend;
import group13.primitives.Address;
import group13.primitives.Event;
import group13.primitives.EventListener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;

public class PerfectLinkOut implements EventListener {

    private int source_process_id;
    private Address destination;
    private int destination_process_id;

    private DatagramSocket send_socket;
    private int sequence_number;

    private TreeMap<Integer, DatagramPacket> sent_packets;


    public PerfectLinkOut (int source_process_id, Address destination) {
        this.source_process_id = source_process_id;
        this.destination = destination;
        this.sequence_number = 0;
        this.sent_packets = new TreeMap<>();

        // create socket with any port number
        try {
            send_socket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("Error : Couldn't bind an address to the socket");
            throw new RuntimeException(e);
        }

        class RetransmitPacketsTask extends TimerTask {

            @Override
            public void run() {
                for (int sequence_number : sent_packets.keySet()) {
                    DatagramPacket packet = sent_packets.get(sequence_number);

                    try {
                        send_socket.send(packet);
                    } catch (IOException e) {
                        // TODO : must die? or just retry?
                        System.out.println("Error : Unable to send packet (must die? or just retry?)");
                        throw new RuntimeException(e);
                    }
                }

            }
        }

        Timer time = new Timer();
        RetransmitPacketsTask task = new RetransmitPacketsTask();
        time.scheduleAtFixedRate(task, 300, 300);
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
        // code 0 represents a normal message being sent
        packetData[0] = (byte) 0x00;

        // sequence number
        packetData[1] = (byte) (this.sequence_number >> 24);
        packetData[2] = (byte) (this.sequence_number >> 16);
        packetData[3] = (byte) (this.sequence_number >> 8);
        packetData[4] = (byte) this.sequence_number;

        // process id
        packetData[5] = (byte) (this.source_process_id >> 24);
        packetData[6] = (byte) (this.source_process_id >> 16);
        packetData[7] = (byte) (this.source_process_id >> 8);
        packetData[8] = (byte) this.source_process_id;

        System.arraycopy(data, 0, packetData, PerfectLink.HEADER_SIZE, data.length);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, this.destination.getInet_address(), this.destination.getPort());

        try {
            send_socket.send(packet);
        } catch (IOException e) {
            // TODO : must die? or just retry?
            System.out.println("Error : Unable to send packet (must die? or just retry?)");
            throw new RuntimeException(e);
        }

        sent_packets.put(this.sequence_number, packet);
        this.sequence_number += 1;
    }

    public void send_ack(int ack_sequence_number) {

        byte[] packetData = new byte[PerfectLink.HEADER_SIZE];

        // prepend type_of_message + sequence_number + process_id
        // code 0 represents am ACK message being sent
        packetData[0] = (byte) 0x01;

        // sequence number
        packetData[1] = (byte) (ack_sequence_number >> 24);
        packetData[2] = (byte) (ack_sequence_number >> 16);
        packetData[3] = (byte) (ack_sequence_number >> 8);
        packetData[4] = (byte) ack_sequence_number;

        // process id
        packetData[5] = (byte) (this.source_process_id >> 24);
        packetData[6] = (byte) (this.source_process_id >> 16);
        packetData[7] = (byte) (this.source_process_id >> 8);
        packetData[8] = (byte) this.source_process_id;

        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, this.destination.getInet_address(), this.destination.getPort());

        try {
            send_socket.send(packet);
        } catch (IOException e) {
            // TODO : must die? or just retry?
            System.out.println("Error : Unable to send packet (must die? or just retry?)");
            throw new RuntimeException(e);
        }
    }

    public void acked_packet (int sequence_number) {
        if (this.sent_packets.containsKey(sequence_number)) {
            this.sent_packets.remove(sequence_number);
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
