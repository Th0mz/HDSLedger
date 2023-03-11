package group13.channel.perfectLink;

import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.primitives.Address;
import group13.primitives.EventHandler;
import group13.primitives.EventListener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class PerfectLinkIn extends Thread {

    // Process definition
    // TODO : (needed?) private int processId;
    private Address address;

    private DatagramSocket recv_socket;
    private EventHandler plEventHandler;

    public PerfectLinkIn(Address address, EventHandler plEventHandler) {
        this.address = address;

        try {
            recv_socket = new DatagramSocket(address.getPort(), address.getInet_address());
        } catch (SocketException e) {
            System.out.println("Error : Couldn't bind " + address.getHostname() + ":"
                    + address.getPort() + " to the perfect link socket" );
            throw new RuntimeException(e);
        }

        this.plEventHandler = plEventHandler;
        this.start();
    }

    public Address getAddress() {
        return address;
    }

    @Override
    public void run() {
        /*
         Function that waits for new packets to arrive and process them
        * this functionality is run in a separate thread
        */

        while (true) {
            byte[] buffer = new byte[500];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            try {
                recv_socket.receive(packet);

                InetAddress address = packet.getAddress();
                String hostname = address.getHostAddress();
                int port = packet.getPort();
                Address source = new Address(hostname, port);

                String message = new String(packet.getData(), 0, packet.getLength());

                Pp2pDeliver deliver_event = new Pp2pDeliver(source, message);
                plEventHandler.trigger(deliver_event);

            } catch (SocketException e) {
                // socket closed
            } catch (IOException e) {
                System.out.println("Error : Couldn't read or write to socket");
                throw new RuntimeException(e);
            }

            /*
            try {
                recv_socket.receive(packet);
                int typeOfMessage = getMessageType(packet.getData());
                int sequenceNumber = getSeqNum(packet.getData());
                if (typeOfMessage == 0) {
                    // Means it's a 'send' message
                    // Needs to send ack
                    // Needs to deliver if hasnt done so
                    if (!deliveredRecPkts.contains(sequenceNumber)) {

                        // mark it as delivered
                        deliveredRecPkts.add(sequenceNumber);

                        // deliver the message to the above module
                        InetAddress address = packet.getAddress();
                        String hostname = address.getHostAddress();
                        int port = packet.getPort();
                        Address source = new Address(hostname, port);

                        // TODO : need to make a function that extracts the payload
                        //        from the packet byte stream
                        Pp2pDeliver deliver_event = new Pp2pDeliver(source, "teste");
                        eventHandler.trigger(deliver_event);

                        // send 'ack' message
                        buffer[0] = (byte) 0xff; //simply change type to ack and send
                        packet = new DatagramPacket(buffer, buffer.length, address, port);
                        recv_socket.send(packet);
                    }
                } else if (typeOfMessage == 1) {
                    // Means it's an 'ack' message
                    // Needs to remove from toBeSent
                    if (toBeSentPackets.containsKey(sequenceNumber)) {
                        toBeSentPackets.remove(sequenceNumber);
                    } else {
                        // byzantine behavior ?
                        // do nothing
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading/writing socket");
                System.exit(-1);
            }
            */

        }
    }

    public int getSeqNum(byte[] ackData) {
        return(ackData[1] << 24) | ((ackData[2] & 0xFF) << 16) | ((ackData[3] & 0xFF) << 8) | (ackData[4] & 0xFF);
    }

    public int getMessageType(byte[] data) {
        if(data[0] == 0x00)
            return 0; // it's a 'send' message
        else if (data[0] == 0xff)
            return 1; // it's an 'ack' message
        return -1; //unknown type
    }

    public void close () {
        this.recv_socket.close();
    }
}
