package group13.member;

public class ServerStruct {
    public String _address, _port;
    public ServerStruct(String address, String port) {
        _address = address;
        _port = port;
    }

    @Override
    public String toString() {
        return "address:" + _address + " | port:" + _port;
    }
}
