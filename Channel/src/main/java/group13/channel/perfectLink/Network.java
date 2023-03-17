package group13.channel.perfectLink;
import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.perfectLink.events.NetworkNew;
import group13.primitives.Address;
import group13.primitives.EventHandler;
import group13.primitives.EventListener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
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

    private EventHandler networkHandler;


    public Network (Address inAddress) {
        this.inAddress = inAddress;
        this.links = new HashMap<>();
        this.threadPool = new ArrayList<>();
        this.networkHandler = new EventHandler();

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
                int messageType = this.getMessageType(packetData);


                if (! this.links.containsKey(outProcessId) && messageType == 2) {
                    byte[] byte_stream = new byte[packet.getLength() - PerfectLink.HEADER_SIZE];

                    System.out.println();
                    System.arraycopy(packetData, PerfectLink.HEADER_SIZE, byte_stream, 0, packet.getLength() - PerfectLink.HEADER_SIZE);

                    String payload = new String(byte_stream, StandardCharsets.UTF_8);
                    String[] parts = payload.split(":");

                    if (parts.length != 2) {
                        System.err.println("Error : Invalid address");
                    }

                    Address outAddress = new Address(parts[0], Integer.parseInt(parts[1]));
                    if (!outAddress.getProcessId().equals(outProcessId)) {
                        System.err.println("Error : process id doesnt match with the received address");
                    }

                    // notify about the new link
                    PerfectLink link = this.createLink(outAddress);
                    NetworkNew new_event = new NetworkNew(link);
                    networkHandler.trigger(new_event);

                } else if (!this.links.containsKey(outProcessId) && messageType != 2){
                    // process unknown and first message isn't handshake
                    System.err.println("Error : Received message from unknown process id without handshake");
                    this.close();
                    return;
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

    public PerfectLink getLink (String processId) {
        return this.links.get(processId);
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

    public void subscribeNew(EventListener listener) {
        networkHandler.subscribe(NetworkNew.EVENT_NAME, listener);
    }

    public void unsubscribeNew(EventListener listener) {
        networkHandler.unsubscribe(NetworkNew.EVENT_NAME, listener);
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

    public int getMessageType(byte[] data) {

        if(data[0] == 0x00)
            return 0; // it's a 'send' message
        else if (data[0] == 0x01)
            return 1; // it's an 'ack' message
        else if (data[0] == 0x02)
            return 2;

        return -1; //unknown type
    }
}
