package group13.channel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Server {
    public static void main(String[] args) throws IOException {
        DatagramSocket serverSocket = new DatagramSocket(5000); // Create a server socket on port 5000
        System.out.println("Server is listening on port 5000");

        byte[] receiveData = new byte[1024];

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket); // Wait for a client to send a message
            String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Received message from client: " + message);

            // Do something with the message here

            // Send a response back to the client
            String responseMessage = "Response from server";
            byte[] sendData = responseMessage.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
            serverSocket.send(sendPacket);
        }
    }
}

