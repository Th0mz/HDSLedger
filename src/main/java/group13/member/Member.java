package group13.member;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Member {

    private static MemberInterface frontend;
    public static void main(String[] args){
        BufferedReader reader;
        ArrayList<ServerStruct> listOfServers = new ArrayList<ServerStruct>();
        ArrayList<Integer> portsForBlockchain = new ArrayList<Integer>();
        Integer nrFaulty = -1;
        Integer nrServers = -1;
        int port = -1;
        ServerStruct myInfo = null;

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
				listOfServers.add(new ServerStruct(splited[0], splited[1]));
                portsForBlockchain.add(Integer.parseInt(splited[2]));
                if(i == serverId)
                    myInfo = new ServerStruct(splited[0], splited[1]);
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

        System.out.println(nrFaulty);
        System.out.println(nrServers);
        for(ServerStruct s : listOfServers) {
            System.out.println(s);
        }
        System.out.println(myInfo);

        port = Integer.parseInt(args[1]);
        for(Integer i : portsForBlockchain) {
            frontend = new MemberInterface(i);
            frontend.start();
        }
        
    }
}
