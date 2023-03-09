package group13.channel.primitives;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Address {
    private String hostname;
    private int port;
    InetAddress inet_address;

    public Address(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;

        try {
            this.inet_address = InetAddress.getByName(hostname);
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

    public InetAddress getInet_address() {
        return inet_address;
    }
}
