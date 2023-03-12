package group13.client;

public class BCServerStruct {
    public String _address, _port;
    public BCServerStruct(String address, String port) {
        _address = address;
        _port = port;
    }

    @Override
    public String toString() {
        return "address:" + _address + " | port:" + _port;
    }
}
