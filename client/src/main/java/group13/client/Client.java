package group13.client;

import group13.member.ServerStruct;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Scanner;


public class Client {        

    public static void main(String args[]) {
        int nrServers = -1;
        int nrFaulty = -1;
        int portOfBlockChain = -1;
        BufferedReader reader;
        ArrayList<ServerStruct> listOfServers = new ArrayList<ServerStruct>();
        ArrayList<Integer> portsForBlockchain = new ArrayList<Integer>();
        DatagramSocket socket = null;
        Scanner scanner = new Scanner(System.in);

        if (args.length != 1) {
            System.out.println("Intended format: mvn exec:java -Dexec.args=\"<config-file>\"");
            return;
        }

		try {
			reader = new BufferedReader(new FileReader(args[0]));
			nrFaulty = Integer.parseInt(reader.readLine());
            nrServers = Integer.parseInt(reader.readLine());
            socket = new DatagramSocket();

			for (int i = 0; i < nrServers; i++) {
                String line = reader.readLine();
                String[] splited = line.split("\\s+");
				listOfServers.add(new ServerStruct(splited[0], splited[1]));
                portsForBlockchain.add(Integer.parseInt(splited[2]));
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

        for(ServerStruct s : listOfServers) {
            System.out.println(s);
        }

        String command = "";
        while(true) {
            command = scanner.nextLine();
            if (command.equals("q")) {
                System.out.println("Killed");
                return;
            }
            
            System.out.println("About to send the message: " + command);
            int i = 0;
            for(Integer p : portsForBlockchain) {
                try {
                    DatagramPacket pcktToSend = new DatagramPacket(command.getBytes(), command.getBytes().length, 
                                                            InetAddress.getByName("localhost"), p);
                    socket.send(pcktToSend);
                    /*new byte[] buffer = byte[500];
                    ack = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);*/
                } catch(IOException e) {
                    e.printStackTrace();
                }
                                
            }
        }
    }
}
