package group13.member;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MemberInterface extends Thread{

    int _port;
    private DatagramSocket _socket;

    public MemberInterface(int port) {
        _port = port;
        try{
            _socket = new DatagramSocket(port);  
        } catch(IOException e){
            e.printStackTrace();
        }
        System.out.println("Started process " + _port );
    }

    public void run(){
        while (true) {
            try {
                byte[] data = new byte[500];
                InetAddress srcAddress;
                int srcPort;
                DatagramPacket pcktReceived = new DatagramPacket(data, data.length);
                
                _socket.receive(pcktReceived);
                srcPort = pcktReceived.getPort();
                srcAddress = pcktReceived.getAddress();

                String msg = new String(pcktReceived.getData(), 0, pcktReceived.getLength());
                System.out.println(msg);
                
                String response = "MESSAGE RECEIVED";
                DatagramPacket pcktToSend = new DatagramPacket(response.getBytes(), response.getBytes().length, srcAddress, srcPort);
                _socket.send(pcktToSend);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
