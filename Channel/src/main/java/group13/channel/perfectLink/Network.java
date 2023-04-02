package group13.channel.perfectLink;
import group13.channel.perfectLink.events.NetworkNew;
import group13.primitives.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Network extends Thread {

    public static int MAX_PACKET_SIZE = 65000;

    protected HashMap<PublicKey, PerfectLink> links;
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

                    Object receivedObject = objectInputStream.readObject();
                    if (!(receivedObject instanceof NetworkMessage)) {
                        System.err.println("Error : received message that isn't a NetworkMessage");
                        continue;
                    }

                    receivedMessage = (NetworkMessage) receivedObject;
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }


                PublicKey outPublicKey = receivedMessage.getSenderPK();
                if (! this.links.containsKey(outPublicKey) && receivedMessage.isHandshake()) {

                    Object object = null;
                    try { object = receivedMessage.getPayload().getObject(); }
                    catch (ClassNotFoundException e) { throw new RuntimeException(e); }

                    if (!(object instanceof FreshObject)) {
                        System.err.println("Error : Network message with wrong structure (no signed FreshObject)");
                        continue;
                    }

                    FreshObject freshObject = (FreshObject) object;
                    Object payload = freshObject.getObject();
                    if (!(payload instanceof Address)) {
                        System.err.println("Error : Invalid address");
                        continue;
                    }

                    Address outAddress = (Address) payload;
                    // notify about the new link
                    NetworkNew new_event = new NetworkNew(outAddress, outPublicKey);
                    networkHandler.trigger(new_event, EventHandler.Mode.SYNC);


                } else if (!this.links.containsKey(outPublicKey) && receivedMessage.isHandshake()){
                    // process unknown and first message isn't handshake
                    System.err.println("Error : Received message from unknown process id without handshake");
                    this.close();
                    return;
                }

                NetworkMessage finalReceivedMessage = receivedMessage;
                if (links.containsKey(outPublicKey)) {
                    PerfectLink link = links.get(outPublicKey);
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            // deliver contents to respective PerfectLinkIn
                            link.receive(finalReceivedMessage);
                        }
                    };

                    thread.start();
                    this.threadPool.add(thread);
                } else {
                    System.err.println("Error : Unknown link");
                }

                inputStream.close();
                objectInputStream.close();

            } catch (SocketException e) {
                // socket closed
            } catch (IOException e) {
                System.err.println("Error : Couldn't read or write to socket");
                throw new RuntimeException(e);
            }
        }
    }

    synchronized public PerfectLink createAuthenticatedLink(Address outAddress, PublicKey inPublicKey, PrivateKey inPrivateKey, PublicKey outPublicKey) {
        PerfectLink link = new PerfectLink(this.inAddress, outAddress, inPublicKey, inPrivateKey, outPublicKey);
        this.links.put(outPublicKey, link);

        return link;
    }

    public PerfectLink getLink (PublicKey processPK) {
        return this.links.get(processPK);
    }

    public void subscribeNew(EventListener listener) {
        networkHandler.subscribe(NetworkNew.EVENT_NAME, listener);
    }

    public void unsubscribeNew(EventListener listener) {
        networkHandler.unsubscribe(NetworkNew.EVENT_NAME, listener);
    }

    public void close () {
        this.inSocket.close();

        for (PublicKey processPK : this.links.keySet()) {
            PerfectLink link = this.links.get(processPK);
            link.close();
        }

        for (Thread thread : this.threadPool) {
            thread.interrupt();
        }
        this.interrupt();
    }
}
