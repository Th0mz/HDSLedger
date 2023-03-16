package group13.primitives;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Address {

    private byte[] processId;
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

        this.processId = calculateProcessId(this);
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

    public static byte[] calculateProcessId (Address address) {
        MessageDigest digest = null;

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        String address_string = address.toString();
        byte[] processId = digest.digest(address_string.getBytes());

        return processId;
    }

    public String toString() {
        return "address:" + this.hostname + " | port:" + this.port;
    }

}
