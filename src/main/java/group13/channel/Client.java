package group13.channel;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {
    public static void main(String[] args) throws IOException {
        DatagramSocket clientSocket = new DatagramSocket(); // Create a client socket
        InetAddress serverAddress = InetAddress.getByName("localhost");
        int serverPort = 5000;

        // Send a message to the server
        String message = "Hello from client";
        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
        clientSocket.send(sendPacket);

        byte[] receiveData = new byte[1024];

        // Wait for a response from the server
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        String responseMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
        System.out.println("Received response from server: " + responseMessage);

        clientSocket.close(); // Close the client socket
    }
}