package group13.channel.perfectLInk;

import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.perfectLInk.events.Pp2pDeliver;
import group13.channel.perfectLInk.events.Pp2pSend;
import group13.channel.primitives.Address;
import group13.channel.primitives.Event;
import group13.channel.primitives.EventListener;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * PerfectLink implements both the functionality for client and server
 * of a perfect link.
 * Constructor takes the port at which it should listen for incoming connections.
 */
public class PerfectLink implements EventListener {

    private DatagramSocket socket;
    private Random random;
    private int _port;
    private int _address;
    private DatagramSocket _socket;
    private int _processId;
    private ReentrantLock seqNumLock = new ReentrantLock();

    private HashSet<Integer> usedSeqNum;
    private Map<Integer, DatagramPacket> toBeSentPackets;
    private HashSet<Integer> deliveredRecPkts;

    private PLEventHandler eventHandler = new PLEventHandler();


    public PerfectLink(int port, int processId) throws Exception {
        _port = port;
        _processId = processId;
        try {
            _socket = new DatagramSocket(port); 
        } catch (SocketException e) {
            System.out.println("Error creating socket");
			System.exit(-1);
        } 

        this.toBeSentPackets = new HashMap<Integer, DatagramPacket>();
        this.deliveredRecPkts = new HashSet<Integer>();
        // ?? this.sentTimes = new HashMap<Integer, Long>();
        // ?? this.ackReceived = new HashMap<Boolean>();

        class ScheduledTask extends TimerTask {
            public void run() {
                for (DatagramPacket pckt : toBeSentPackets.values()) {
                    //Go over and resend all the packets for which we have not received an ack
                    try {
                        _socket.send(pckt);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                
            }
        }
    
        Timer time = new Timer();
        ScheduledTask st = new ScheduledTask();
        time.scheduleAtFixedRate(st, 100, 100);
    }


    // handle received events
    public void update(Event event) {
        String eventName = event.getEventName();
        if (eventName == Pp2pSend.EVENT_NAME) {
            Pp2pSend typed_event = (Pp2pSend) event;
            Address destination = typed_event.getDestination();
            byte[] payload = typed_event.getPayload().getBytes();

            try {
                this.send(destination.getAddress().getHostAddress(), destination.getPort(), payload);
            } catch (Exception e) {
                // TODO : what to do if the send method raises error?
                //   - what must happen for the send method to raise an error?
                throw new RuntimeException(e);
            }
        }
    }

    public void subscribeDelivery(EventListener listener) {
        eventHandler.subscribe(BEBDeliver.EVENT_NAME, listener);
    }

    public void send(String destHost, int destPort, byte[] data) throws Exception {

        byte[] packetData = new byte[data.length + 5];
        int seqNum;
        int r = random.nextInt(214748364);
        seqNumLock.lock();
        try {
            seqNum = r * 10 + _processId;
            while (usedSeqNum.contains(seqNum)) {
                r = random.nextInt(214748364);
                seqNum = r * 10 + _processId;
            }
            usedSeqNum.add(seqNum);
        } finally {
            seqNumLock.unlock();
        }
                
        // prepend type_of_message + sequence_number to message
        packetData[0] = (byte) 0x00; //0x0 indicates that the message is original send
        packetData[1] = (byte) (seqNum >> 24);
        packetData[2] = (byte) (seqNum >> 16);
        packetData[3] = (byte) (seqNum >> 8);
        packetData[4] = (byte) seqNum;

        System.arraycopy(data, 0, packetData, 4, data.length);

        InetAddress destAddr = InetAddress.getByName(destHost);
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, destAddr, destPort);

        toBeSentPackets.put(seqNum, packet);
        socket.send(packet);

    }

    public void runServer() {
        while (true) {
            byte[] buffer = new byte[500];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            try {
                _socket.receive(packet);
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
                        int port = packet.getPort();
                        Address source = new Address(address, port);

                        // TODO : need to make a function that extracts the payload
                        //        from the packet byte stream
                        Pp2pDeliver deliver_event = new Pp2pDeliver(source, "teste");
                        eventHandler.trigger(deliver_event);

                        // send 'ack' message
                        buffer[0] = (byte) 0xff; //simply change type to ack and send
                        packet = new DatagramPacket(buffer, buffer.length, address, port);
                        _socket.send(packet);
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

    public void close() {
        socket.close();
    }
}

