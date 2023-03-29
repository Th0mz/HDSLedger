package group13.blockchain.member;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import group13.primitives.Address;

public class StartServer {

    private static BMember server = new BMember();
    
    public static void main(String[] args){
        BufferedReader reader;

        ArrayList<Address> listOfServers = new ArrayList<Address>();
        ArrayList<Integer> portsForBlockchain = new ArrayList<Integer>();
        Integer nrFaulty = -1;
        Integer nrServers = -1;
        Address interfaceAddress = null;
        Address myInfo = null;

        if (args.length != 2) {
            System.out.println("Intended format: mvn exec:java -Dexec.args=\"<config-file> <processId>\"");
            return;
        }

        Integer serverId = Integer.parseInt(args[1]);
        boolean isLeader = false;
        if (serverId == 0)
            isLeader = true;

		try {
			reader = new BufferedReader(new FileReader(args[0]));
			nrFaulty = Integer.parseInt(reader.readLine());
            nrServers = Integer.parseInt(reader.readLine());

			for (int i = 0; i < nrServers; i++) {
                String line = reader.readLine();
                String[] splited = line.split("\\s+");
				listOfServers.add(new Address(splited[0], Integer.parseInt(splited[1])));
                portsForBlockchain.add(Integer.parseInt(splited[2]));
                if(i == serverId){
                    myInfo = new Address(splited[0], Integer.parseInt(splited[1]));
                    interfaceAddress = new Address(Integer.parseInt(splited[2]));
                }
                    
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

        String leaderId = listOfServers.get(0).getProcessId();
        server.createBMember(listOfServers, nrFaulty, nrServers, interfaceAddress,
                            myInfo, leaderId);
        
    }
}
