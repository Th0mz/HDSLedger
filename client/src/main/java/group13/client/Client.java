package group13.client;

import group13.member.ServerStruct;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Client {        

    public static void main(String args[]) {
        int nrServers = -1;
        int nrFaulty = -1;
        BufferedReader reader;
        ArrayList<ServerStruct> listOfServers = new ArrayList<ServerStruct>();

        if (args.length != 1) {
            System.out.println("Intended format: mvn exec:java -Dexec.args=\"<config-file>\"");
            return;
        }

		try {
			reader = new BufferedReader(new FileReader(args[0]));
			nrFaulty = Integer.parseInt(reader.readLine());
            nrServers = Integer.parseInt(reader.readLine());

			for (int i = 0; i < nrServers; i++) {
                String line = reader.readLine();
                String[] splited = line.split("\\s+");
				listOfServers.add(new ServerStruct(splited[0], splited[1]));
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

        for(ServerStruct s : listOfServers) {
            System.out.println(s);
        }
    }
}
