package group13.channel.perfectLink;
import group13.primitives.Address;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class Network extends Thread {

    protected HashMap<String, PerfectLink> links;
    private Address inAddress;
    private DatagramSocket inSocket;

    private List<Thread> threadPool;


    public Network (Address inAddress) {
        this.inAddress = inAddress;
        this.links = new HashMap<>();
        this.threadPool = new ArrayList<>();

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
                String outProcessId = this.getProcessId(packetData);

                if (! this.links.containsKey(outProcessId)) {
                    //TODO: MARTELADA ADD SENDER SO IT CAN RECEIVE MSG FROM CLIENT(PID UNKNOWN)
                    // processId, new Address(9876)
                    System.out.println("Error : Received message from unknown process id");
                }

                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        // deliver contents to respective PerfectLinkIn
                        links.get(outProcessId).receive(packetData, packet.getLength(), packet.getPort());
                    }
                };

                thread.start();
                this.threadPool.add(thread);

            } catch (SocketException e) {
                // socket closed
            } catch (IOException e) {
                System.out.println("Error : Couldn't read or write to socket");
                throw new RuntimeException(e);
            }
        }
    }

    synchronized public PerfectLink createLink (Address outAddress) {
        PerfectLink link = new PerfectLink(this.inAddress, outAddress);

        String outProcessId = outAddress.getProcessId();
        this.links.put(outProcessId, link);

        return link;
    }

    // message parse
    public String getProcessId(byte[] packetData) {
        int processIdStart = PerfectLink.MESSAGE_TYPE_SIZE + PerfectLink.SEQUENCE_NUMBER_SIZE;
        byte[] processId = new byte[32];

        for (int i = processIdStart; i < processIdStart + PerfectLink.PROCESS_ID_SIZE; i++) {
            processId[i - processIdStart] = packetData[i];
        }

        return Base64.getEncoder().encodeToString(processId);
    }

    public void close () {
        this.inSocket.close();

        for (String processId : this.links.keySet()) {
            PerfectLink link = this.links.get(processId);
            link.close();
        }

        for (Thread thread : this.threadPool) {
            thread.interrupt();
        }
        this.interrupt();
    }
}
