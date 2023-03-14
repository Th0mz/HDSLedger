package group13.primitives;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Address {
    private String hostname;
    private int port;
    InetAddress inetAddress;

    public Address(int port) {
        this("localhost", port);
    }

    public Address(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;

        try {
            this.inetAddress = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            System.out.println("Error : hostname '" + hostname + "' is not valid");
            throw new RuntimeException(e);
        }
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }
}
