package group13.channel.perfectLink;

import group13.channel.bestEffortBroadcast.events.BEBSend;
import group13.primitives.Address;
import group13.primitives.FreshObject;
import group13.primitives.NetworkMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Objects;

import javax.sql.rowset.spi.SyncResolver;

public class PerfectLinkOut {

    private int sequenceNumber;

    private Address inAddress;
    private Address outAddress;

    private PerfectLink link;
    private DatagramSocket outSocket;

    private PublicKey inPublicKey;
    private PrivateKey inPrivateKey;

    public PerfectLinkOut(PerfectLink link, Address inAddress, Address outAddress, PublicKey inPublicKey, PrivateKey inPrivateKey) {
        this.sequenceNumber = 0;
        this.inAddress = inAddress;
        this.outAddress = outAddress;
        this.link = link;

        this.inPublicKey = inPublicKey;
        this.inPrivateKey = inPrivateKey;

        try {
            this.outSocket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void send(Object data) {

        // DEBUG :
        /*
        System.out.println("[" + inAddress.getProcessId() + "] Sending object :\n" + data.toString());
        /**/

        // sign fresh object
        FreshObject freshObject = new FreshObject(this.sequenceNumber, data);
        SignedObject signedObject = null;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signedObject = new SignedObject(freshObject, this.inPrivateKey, signature);
        } catch (IOException | InvalidKeyException | SignatureException |
                 NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        NetworkMessage message = new NetworkMessage(this.inPublicKey, this.sequenceNumber, signedObject, NetworkMessage.messageTypes.SEND);

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
            System.err.println("Error : Unable to send packet (must die? or just retry?)");
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
    public void retransmit (byte[] payload) {

        // DEBUG :
        /*
        System.out.println("[" + inAddress.getProcessId() + "] Retransmitting payload :\n" + payload.toString());
        /**/

        DatagramPacket packet = new DatagramPacket(payload, payload.length,
                this.outAddress.getInetAddress(), this.outAddress.getPort());

        try {
            this.outSocket.send(packet);
        } catch (SocketException e) {
            // socket closed
        } catch (IOException e) {
            // TODO : must die? or just retry?
            System.err.println("Error : Unable to send packet (must die? or just retry?)");
            throw new RuntimeException(e);
        }
    }

    public void send_ack(int ackSequenceNumber) {

        // DEBUG :
        /*
        System.out.println("[" + inAddress.getProcessId() + "] " + ackSequenceNumber + " - ACK :\n");
        /**/

        // sign fresh object
        FreshObject freshObject = new FreshObject(ackSequenceNumber, null);
        SignedObject signedObject = null;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signedObject = new SignedObject(freshObject, this.inPrivateKey, signature);
        } catch (IOException | InvalidKeyException | SignatureException |
                 NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        NetworkMessage message = new NetworkMessage(this.inPublicKey, ackSequenceNumber , signedObject, NetworkMessage.messageTypes.ACK);

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
            System.err.println("Error : Unable to send packet (must die? or just retry?)");
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

    public synchronized void send_handshake () {

        // DEBUG :
        /*
        System.out.println("[" + inAddress.getProcessId() + "] Sending handshake message");
        /**/

        // sign fresh object
        FreshObject freshObject = new FreshObject(this.sequenceNumber, this.inAddress);
        SignedObject signedObject = null;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signedObject = new SignedObject(freshObject, this.inPrivateKey, signature);
        } catch (IOException | InvalidKeyException | SignatureException |
                 NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        NetworkMessage message = new NetworkMessage(this.inPublicKey, this.sequenceNumber, signedObject, NetworkMessage.messageTypes.HANDSHAKE);

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
            System.err.println("Error : Unable to send packet (must die? or just retry?)");
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
