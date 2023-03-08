package group13.channel.primitives;

import java.net.InetAddress;

public class Address {
    private InetAddress address;
    private int port;

    public Address(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
