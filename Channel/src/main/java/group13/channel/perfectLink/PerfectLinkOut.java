package group13.channel.perfectLink;

import group13.primitives.Address;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Objects;

public class PerfectLinkOut {

    private int sequenceNumber;
    private int inProcessId;
    private Address outAddress;
    private PerfectLink link;
    private DatagramSocket outSocket;

    public PerfectLinkOut(PerfectLink link, int inProcessId, Address outAddress) {
        this.sequenceNumber = 0;
        this.inProcessId = inProcessId;
        this.outAddress = outAddress;
        this.link = link;

        try {
            this.outSocket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(byte[] data) {

        byte[] packetData = new byte[data.length + PerfectLink.HEADER_SIZE];

        // prepend type_of_message + sequence_number + process_id
        // code 0 represents a normal message being sent
        packetData[0] = (byte) 0x00;

        // sequence number
        packetData[1] = (byte) (this.sequenceNumber >> 24);
        packetData[2] = (byte) (this.sequenceNumber >> 16);
        packetData[3] = (byte) (this.sequenceNumber >> 8);
        packetData[4] = (byte) this.sequenceNumber;

        // process id
        packetData[5] = (byte) (this.inProcessId >> 24);
        packetData[6] = (byte) (this.inProcessId >> 16);
        packetData[7] = (byte) (this.inProcessId >> 8);
        packetData[8] = (byte) this.inProcessId;

        System.arraycopy(data, 0, packetData, PerfectLink.HEADER_SIZE, data.length);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, this.outAddress.getInetAddress(), this.outAddress.getPort());

        try {
            this.outSocket.send(packet);
        } catch (IOException e) {
            // TODO : must die? or just retry?
            System.out.println("Error : Unable to send packet (must die? or just retry?)");
            throw new RuntimeException(e);
        }

        this.link.packet_send(sequenceNumber, packetData);
        this.sequenceNumber += 1;
    }

    public void send_ack(int ackSequenceNumber) {

        byte[] packetData = new byte[PerfectLink.HEADER_SIZE];

        // prepend type_of_message + sequence_number + process_id
        // code 0 represents am ACK message being sent
        packetData[0] = (byte) 0x01;

        // sequence number
        packetData[1] = (byte) (ackSequenceNumber >> 24);
        packetData[2] = (byte) (ackSequenceNumber >> 16);
        packetData[3] = (byte) (ackSequenceNumber >> 8);
        packetData[4] = (byte) ackSequenceNumber;

        // process id
        packetData[5] = (byte) (this.inProcessId >> 24);
        packetData[6] = (byte) (this.inProcessId >> 16);
        packetData[7] = (byte) (this.inProcessId >> 8);
        packetData[8] = (byte) this.inProcessId;

        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, this.outAddress.getInetAddress(), this.outAddress.getPort());

        try {
            this.outSocket.send(packet);
        } catch (IOException e) {
            // TODO : must die? or just retry?
            System.out.println("Error : Unable to send packet (must die? or just retry?)");
            throw new RuntimeException(e);
        }
    }


    @Override
    public int hashCode() {
        return Objects.hash(this.outAddress);
    }

    public void close() {
        this.outSocket.close();
    }

}
