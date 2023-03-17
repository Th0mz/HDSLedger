package group13.blockchain.member;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import group13.blockchain.consensus.IBFT;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.primitives.Address;

public class BMember {
    protected ArrayList<Address> _serverList = new ArrayList<Address>();
    protected Integer _nrFaulty;
    protected Integer _nrServers;
    protected Address _myInfo;
    protected boolean _isLeader;

    protected BMemberInterface frontend;

    protected IBFT _consensus;
    protected ArrayList<String> _ledger = new ArrayList<String>();
    protected Lock ledgerLock = new ReentrantLock();

    public BMember(){}

    public void createBMember(ArrayList<Address> serverList, Integer nrFaulty, Integer nrServers,
                    Address interfaceAddress, Address myInfo, String leaderId) {

        _serverList = serverList;
        _nrFaulty = nrFaulty;
        _nrServers = nrServers;
        _myInfo = myInfo;
        _isLeader = myInfo.getProcessId().equals(leaderId);

        for (int i = 0; i < 1000; i++) {
            _ledger.add("");
        }

        BEBroadcast beb = new BEBroadcast(myInfo);
        for (Address serverAddress : serverList) {
            beb.addServer(serverAddress);
        }

        _consensus = new IBFT(_nrServers, _nrFaulty, leaderId, beb, this);
        frontend = new BMemberInterface(interfaceAddress, this);
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

    public String getConsensusResult(int instance) {
        return _ledger.get(instance);
    }

    public IBFT getConsensusObject(){
        return _consensus;
    }

    public void printLedger(){
        for(String i : _ledger)
            System.out.println(i);
    }

}
