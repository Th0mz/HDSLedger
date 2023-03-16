package group13.channel.perfectLink;

import group13.primitives.Address;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Base64;
import java.util.Objects;

public class PerfectLinkOut {

    private int sequenceNumber;

    private Address inAddress;
    private Address outAddress;

    private PerfectLink link;
    private DatagramSocket outSocket;

    public PerfectLinkOut(PerfectLink link, Address inAddress, Address outAddress) {
        this.sequenceNumber = 0;
        this.inAddress = inAddress;
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
        byte[] inProcessId = Base64.getDecoder().decode(this.inAddress.getProcessId());
        System.arraycopy(inProcessId, 0, packetData,
                PerfectLink.MESSAGE_TYPE_SIZE + PerfectLink.SEQUENCE_NUMBER_SIZE,
                inProcessId.length);


        System.arraycopy(data, 0, packetData, PerfectLink.HEADER_SIZE, data.length);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length,
                this.outAddress.getInetAddress(), this.outAddress.getPort());

        try {
            this.outSocket.send(packet);
        } catch (SocketException e) {
            // socket closed
        } catch (IOException e) {
            // TODO : must die? or just retry?
            System.out.println("Error : Unable to send packet (must die? or just retry?)");
            throw new RuntimeException(e);
        }

        this.link.packet_send(sequenceNumber, packetData);
        this.sequenceNumber += 1;
    }
    public void retransmit (byte[] payload, int sequenceNumber) {
        DatagramPacket packet = new DatagramPacket(payload, payload.length,
                this.outAddress.getInetAddress(), this.outAddress.getPort());

        try {
            this.outSocket.send(packet);
        } catch (SocketException e) {
            // socket closed
        } catch (IOException e) {
            // TODO : must die? or just retry?
            System.out.println("Error : Unable to send packet (must die? or just retry?)");
            throw new RuntimeException(e);
        }
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
        byte[] inProcessId = Base64.getDecoder().decode(this.inAddress.getProcessId());
        System.arraycopy(inProcessId, 0, packetData,
                PerfectLink.MESSAGE_TYPE_SIZE + PerfectLink.SEQUENCE_NUMBER_SIZE,
                inProcessId.length);

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
