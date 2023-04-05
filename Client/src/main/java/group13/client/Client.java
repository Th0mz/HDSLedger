package group13.client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Scanner;

import group13.primitives.Address;


public class Client {

    public static String REGISTER_CMD = "1";
    public static String TRANSFER_CMD = "2";
    public static String CHECK_CMD = "3";
    public static String QUIT_CMD = "q";

    public static void main(String args[]) {
        int nrServers = -1;
        int nrFaulty = -1;
        int portOfBlockChain = -1;
        BufferedReader reader;
        ArrayList<Address> listOfServers = new ArrayList<Address>();
        ArrayList<Integer> portsForBlockchain = new ArrayList<Integer>();
        ClientFrontend frontend = null;

        if (args.length != 2) {
            System.out.println("Intended format: mvn exec:java -Dexec.args=\"<config-file> <public-key-file>\"");
            return;
        }

		try {
			reader = new BufferedReader(new FileReader(args[0]));
			nrFaulty = Integer.parseInt(reader.readLine());
            nrServers = Integer.parseInt(reader.readLine());

            ArrayList<Integer> pids = new ArrayList<Integer>();
			for (int i = 0; i < nrServers; i++) {
                String line = reader.readLine();
                String[] splited = line.split("\\s+");
				listOfServers.add(new Address(Integer.parseInt(splited[2])));
                portsForBlockchain.add(Integer.parseInt(splited[2]));
                pids.add(i);
			}


            frontend = new ClientFrontend(new Address(9876), listOfServers, args[1], nrFaulty);
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

        for(Address s : listOfServers) {
            System.out.println(s.getPort());
        }

        
        Scanner scanner = new Scanner(System.in);
        String command = "";
        while(true) {
            System.out.println("Possible commands: ");
            System.out.println(" 1) REGISTER to blockchain");
            System.out.println(" 2) TRANSFER tokens ");
            System.out.println(" 3) CHECK balance ");
            System.out.println(" q) To close client ");
            command = scanner.nextLine();

            if (QUIT_CMD.equals(command)) {
                System.out.println("closing...");
                scanner.close();
                System.exit(0);

            } else if (REGISTER_CMD.equals(command)) {
                frontend.register();
            }
            else if (TRANSFER_CMD.equals(command)) {
                System.out.println("Name of file of publicKey:");
                String pubFile = scanner.nextLine();
                System.out.println("Amount to transfer:");
                Integer amount = Integer.parseInt(scanner.nextLine());
                PublicKey pKeyDest = getPubKey(pubFile);

                frontend.transfer(pKeyDest, amount);
            }
            else if (CHECK_CMD.equals(command)) {
                frontend.checkBalance();
            }
        }
    }

    private static PublicKey getPubKey(String file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);
            fis.close();
            X509EncodedKeySpec ks = new X509EncodedKeySpec(bytes, "RSA");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey pub = kf.generatePublic(ks);
            return pub;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        //hack
        PublicKey k = null;
        return k;
    }
}
