package group13.channel.perfectLink;

import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.primitives.Address;
import group13.primitives.EventHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;

public class PerfectLinkIn extends Thread {

    // Process definition
    private int processId;
    private Address address;

    private DatagramSocket recv_socket;

    private HashMap<Integer, PerfectLinkOut> out_links;
    private EventHandler plEventHandler;

    private HashMap<Integer, Integer> delivered_messages;

    public PerfectLinkIn(int processId, Address address, EventHandler plEventHandler, HashMap<Integer, PerfectLinkOut> out_links) {
        this.processId = processId;
        this.address = address;
        this.delivered_messages = new HashMap<>();
        this.out_links = out_links;

        System.out.println("Listening at " + address.getPort());

        try {
            recv_socket = new DatagramSocket(address.getPort(), address.getInet_address());
        } catch (SocketException e) {
            System.out.println("Error : Couldn't bind " + address.getHostname() + ":"
                    + address.getPort() + " to the perfect link socket" );
            throw new RuntimeException(e);
        }

        this.plEventHandler = plEventHandler;
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
                System.out.println("PLin received message: ");
                byte[] packet_data = packet.getData();
                int message_type = this.getMessageType(packet_data);
                int sequence_number = this.getSeqNum(packet_data);
                int process_id = this.getProcessId(packet_data);

                // send message received, it is needed to :
                //   - send ack
                //   - deliver the message if it wasn't already
                if (! this.delivered_messages.containsKey(process_id)) {
                    this.addSender(process_id);
                    this.out_links.put(process_id, new PerfectLinkOut(processId, new Address(9876)));
                    //TODO: MARTELADA ADD SENDER SO IT CAN RECEIVE MSG FROM CLIENT(PID UNKNOWN)
                    System.out.println("Error : Received message from unknown process id");
                }

                if (message_type == 0 ) {
                    // DEBUG :
                    System.out.println("message received [pid = " + this.processId + "] : ");
                    System.out.println(" - process id : " + process_id);
                    System.out.println(" - sequence number : " + sequence_number);

                    int current_sequence_number = this.delivered_messages.get(process_id);

                    // deliver message
                    if (current_sequence_number == sequence_number) {
                        String message = new String(packet_data, PerfectLink.HEADER_SIZE, packet.getLength() - PerfectLink.HEADER_SIZE);
                        Pp2pDeliver deliver_event = new Pp2pDeliver(process_id, message, packet.getPort());
                        plEventHandler.trigger(deliver_event);
                        this.delivered_messages.put(process_id, current_sequence_number + 1);
                        System.out.println("sequence number is increased");
                    }

                    // ack all messages that were already received
                    if (current_sequence_number >= sequence_number) {
                        // send ack
                        this.out_links.get(process_id).send_ack(sequence_number);
                        System.out.println("Ack is sent");
                    }

                } else if (message_type == 1) {
                    // must notify the respective perfect link out that the message was
                    // already received
                    // DEBUG :
                    System.out.println("ACK received [pid = " + this.processId + "] :" );
                    System.out.println(" - process id : " + process_id);
                    System.out.println(" - sequence number : " + sequence_number);
                    this.out_links.get(process_id).acked_packet(sequence_number);
                }

            } catch (SocketException e) {
                // socket closed
            } catch (IOException e) {
                System.out.println("Error : Couldn't read or write to socket");
                throw new RuntimeException(e);
            }
        }
    }

    public void addSender (int processId) {
        if (this.delivered_messages.containsKey(processId)) {
            System.out.println("Error : Multiple senders with the same process id");
            System.exit(-1);
        }

        this.delivered_messages.put(processId, 0);
    }

    public int getSeqNum(byte[] ackData) {
        return (ackData[1] << 24) | (ackData[2] << 16) | (ackData[3] << 8) | ackData[4];
    }

    public int getProcessId(byte[] ackData) {
        return (ackData[5] << 24) | (ackData[6] << 16) | (ackData[7] << 8) | ackData[8];
    }

    public int getMessageType(byte[] data) {

        if(data[0] == 0x00)
            return 0; // it's a 'send' message
        else if (data[0] == 0x01)
            return 1; // it's an 'ack' message

        return -1; //unknown type
    }

    public void close () {
        this.recv_socket.close();
    }
}
