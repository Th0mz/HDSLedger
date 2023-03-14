package group13.channel.perfectLink;
import group13.primitives.Address;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;

public class Network extends Thread {

    private HashMap<Integer, PerfectLink> links;
    private int inProcessId;
    private Address inAddress;
    private DatagramSocket inSocket;

    public Network (int inProcessId, Address inAddress) {
        this.inProcessId = inProcessId;
        this.inAddress = inAddress;
        this.links = new HashMap<>();

        try {
            inSocket = new DatagramSocket(inAddress.getPort(), inAddress.getInetAddress());
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        this.start();
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
                inSocket.receive(packet);
                byte[] packetData = packet.getData();
                int outProcessId = this.getProcessId(packetData);

                if (! this.links.containsKey(outProcessId)) {
                    //TODO: MARTELADA ADD SENDER SO IT CAN RECEIVE MSG FROM CLIENT(PID UNKNOWN)
                    // processId, new Address(9876)
                    this.createLink(100, new Address(9999));
                    System.out.println("Error : Received message from unknown process id");
                }

                // deliver contents to respective PerfectLinkIn
                this.links.get(outProcessId).receive(packetData, packet.getLength(), packet.getPort());

            } catch (SocketException e) {
                // socket closed
            } catch (IOException e) {
                System.out.println("Error : Couldn't read or write to socket");
                throw new RuntimeException(e);
            }
        }
    }

    public PerfectLink createLink (int outProcessId, Address outAddress) {
        PerfectLink link = new PerfectLink(this.inProcessId, this.inAddress, outProcessId, outAddress);
        this.links.put(outProcessId, link);

        return link;
    }

    // message parse
    public int getProcessId(byte[] packetData) {
        return (packetData[5] << 24) | (packetData[6] << 16) | (packetData[7] << 8) | packetData[8];
    }

    public void close () {
        this.inSocket.close();

        for (int processId : this.links.keySet()) {
            PerfectLink link = this.links.get(processId);
            link.close();
        }

        this.interrupt();
    }
}
