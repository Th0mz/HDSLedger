package group13.channel.perfectLink;
import group13.channel.perfectLink.events.NetworkNew;
import group13.primitives.Address;
import group13.primitives.EventHandler;
import group13.primitives.EventListener;
import group13.primitives.NetworkMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Network extends Thread {

    public static int MAX_PACKET_SIZE = 65000;

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
            byte[] buffer = new byte[MAX_PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            try {
                inSocket.receive(packet);
                byte[] packetData = packet.getData();


                ByteArrayInputStream inputStream = new ByteArrayInputStream(packetData);
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                NetworkMessage receivedMessage = null;
                // extract message object
                try {
                    receivedMessage = (NetworkMessage) objectInputStream.readObject();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }


                String outProcessId = receivedMessage.getSenderId();
                if (! this.links.containsKey(outProcessId) && receivedMessage.isHandshake()) {

                    /* String payload = new String(receivedMessage.getPayload(), StandardCharsets.UTF_8);
                    String[] parts = payload.split(":"); */

                    /* if (parts.length != 2) {
                        System.err.println("Error : Invalid address");
                    } */
                    Object payload = receivedMessage.getPayload();
                    if (!(payload instanceof Address))
                        System.err.println("Error : Invalid address");

                    Address outAddress = (Address) payload;
                    if (!outAddress.getProcessId().equals(outProcessId)) {
                        System.err.println("Error : process id doesnt match with the received address");
                    }

                    // notify about the new link
                    PerfectLink link = this.createLink(outAddress);
                    NetworkNew new_event = new NetworkNew(link);
                    networkHandler.trigger(new_event);

                } else if (!this.links.containsKey(outProcessId) && receivedMessage.isHandshake()){
                    // process unknown and first message isn't handshake
                    System.err.println("Error : Received message from unknown process id without handshake");
                    this.close();
                    return;
                }

                NetworkMessage finalReceivedMessage = receivedMessage;
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        // deliver contents to respective PerfectLinkIn
                        links.get(outProcessId).receive(finalReceivedMessage);
                    }
                };

                thread.start();
                this.threadPool.add(thread);


                inputStream.close();
                objectInputStream.close();

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
}
