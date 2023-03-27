package group13.channel.perfectLink;

import group13.primitives.Address;
import group13.primitives.NetworkMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
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

        String senderId = this.inAddress.getProcessId();
        NetworkMessage message = new NetworkMessage(senderId, this.sequenceNumber, data, NetworkMessage.messageTypes.SEND);

        // Convert message object to byte stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] packetData = outputStream.toByteArray();
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

        // close open fd
        try {
            outputStream.close();
            objectOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

        String senderId = this.inAddress.getProcessId();
        NetworkMessage message = new NetworkMessage(senderId, this.sequenceNumber, null, NetworkMessage.messageTypes.ACK);

        // Convert message object to byte stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] packetData = outputStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length,
                this.outAddress.getInetAddress(), this.outAddress.getPort());

        try {
            this.outSocket.send(packet);
        } catch (IOException e) {
            // TODO : must die? or just retry?
            System.out.println("Error : Unable to send packet (must die? or just retry?)");
            throw new RuntimeException(e);
        }

        // close open fd
        try {
            outputStream.close();
            objectOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void send_handshake () {

        String senderId = this.inAddress.getProcessId();
        String address = this.inAddress.toString();
        byte[] payload = address.getBytes(StandardCharsets.UTF_8);

        NetworkMessage message = new NetworkMessage(senderId, this.sequenceNumber, payload, NetworkMessage.messageTypes.HANDSHAKE);

        // Convert message object to byte stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] packetData = outputStream.toByteArray();
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

        // close open fd
        try {
            outputStream.close();
            objectOutputStream.close();
        } catch (IOException e) {
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
