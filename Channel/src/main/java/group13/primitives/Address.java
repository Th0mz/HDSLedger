package group13.primitives;

import group13.channel.bestEffortBroadcast.events.BEBDeliver;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

public class Address implements Serializable {

    private String processId;
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

    public String getProcessId() {
        return processId;
    }

    public static String calculateProcessId (Address address) {
        MessageDigest digest = null;

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        String address_string = address.toString();
        byte[] processIdDigest = digest.digest(address_string.getBytes());

        String processId = Base64.getEncoder().encodeToString(processIdDigest);
        return processId;
    }

    public String toString() {
        return this.hostname + ":" + this.port;
    }

    @Override
    public boolean equals(Object o) {

        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        if (!(o instanceof Address)) {
            return false;
        }

        Address deliver = (Address) o;

        return deliver.getProcessId().equals(this.processId);
    }

}
