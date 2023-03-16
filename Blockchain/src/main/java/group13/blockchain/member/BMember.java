package group13.blockchain.member;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import group13.blockchain.consensus.IBFT;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.primitives.Address;

public class BMember {
    private ArrayList<Address> _listOfServers = new ArrayList<Address>();
    private ArrayList<Integer> _portsForBlockchain = new ArrayList<Integer>();
    private Integer _id;
    private Integer _nrFaulty;
    private Integer _nrServers;
    private Integer _myIPort;
    private Integer _myIBFTPort;
    private Address _myInfo;
    private boolean _isLeader;

    private BMemberInterface frontend;

    private IBFT _consensus;
    private ArrayList<String> _ledger = new ArrayList<String>();
    private Lock ledgerLock = new ReentrantLock();

    public BMember(){}

    public void CreateBMember(Integer id, ArrayList<Address> list, ArrayList<Integer> portsBlockchain, Integer nrFaulty, Integer nrServers,
                    Integer myIPort, Integer myIBFTPort, Address myInfo, IBFT consensus, boolean isLeader) {
        _id = id;
        _listOfServers = list;
        _portsForBlockchain = portsBlockchain;
        _nrFaulty = nrFaulty;
        _nrServers = nrServers;
        _myIPort = myIPort;
        _myIBFTPort = myIBFTPort;
        _myInfo = myInfo;
        _isLeader = isLeader;

        BEBroadcast beb = new BEBroadcast(_id, new Address(_myInfo.getPort()));
        for (int index = 0; index < _listOfServers.size(); index++ ) {
            beb.addServer(new Address(_listOfServers.get(index).getPort()));
        }
        _consensus = new IBFT(_id, _nrServers, _nrFaulty, 0, _myIBFTPort, beb, this);

        frontend = new BMemberInterface(_id, _myIPort, this);
    }

    public void tryConsensus(String msg) {
        if (!_isLeader)
            return;

        ledgerLock.lock();
        int nextInstance = _ledger.size();
        _consensus.start(nextInstance, msg);
        System.out.println("============================");
        System.out.println("============================");
        System.out.println("CONSENSUS STARTED");
        ledgerLock.unlock();
    }

    public void deliver(Integer instance, String message) {
        // TODO: CHANGE PID AND PORT FOR REAL VALUES BASED ON WHO ASKED FOR OPERATION
        System.out.println("CONSENSUS FINISHED");
        System.out.println("============================");
        System.out.println("============================");
        ledgerLock.lock();
        _ledger.add(instance, message); //TODO: DONT ALLOW BYZANTINE TO FORCE A MSG
        ledgerLock.unlock();
        frontend.ackClient(instance, message, 9999, 9876);
    }

}
