package group13.blockchain.member;

import java.util.ArrayList;
import java.util.HashMap;
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

    private HashMap<Integer, String> instances = new HashMap<>();
    private int _nextInstance;

    protected BMemberInterface frontend;

    protected IBFT _consensus;
    protected HashMap<Integer, String> _ledger = new HashMap<>();
    protected Lock ledgerLock = new ReentrantLock();

    public BMember(){}

    public void createBMember(ArrayList<Address> serverList, Integer nrFaulty, Integer nrServers,
                    Address interfaceAddress, Address myInfo, String leaderId) {

        _serverList = serverList;
        _nrFaulty = nrFaulty;
        _nrServers = nrServers;
        _myInfo = myInfo;
        _isLeader = myInfo.getProcessId().equals(leaderId);
        _nextInstance = 0;

        BEBroadcast beb = new BEBroadcast(myInfo);
        for (Address serverAddress : serverList) {
            beb.addServer(serverAddress);
        }

        _consensus = new IBFT(_nrServers, _nrFaulty, leaderId, beb, this);
        frontend = new BMemberInterface(interfaceAddress, this);
    }

    public void tryConsensus(String msg, String clientId) {
        if (!_isLeader)
            return;

        ledgerLock.lock();
        int nextInstance = _nextInstance;
        _nextInstance += 1;

        _consensus.start(nextInstance, msg);
        System.out.println("============================");
        System.out.println("============================");
        System.out.println("CONSENSUS STARTED");
        ledgerLock.unlock();
        this.instances.put(nextInstance, clientId);
    }

    public void deliver(Integer instance, String message) {
        // TODO: CHANGE PID AND PORT FOR REAL VALUES BASED ON WHO ASKED FOR OPERATION
        System.out.println("CONSENSUS FINISHED");
        System.out.println("============================");
        System.out.println("============================");

        ledgerLock.lock();
        _ledger.put(instance, message); //TODO: DONT ALLOW BYZANTINE TO FORCE A MSG
        ledgerLock.unlock();

        if (_isLeader) {
            String clientId = instances.get(instance);
            frontend.ackClient(instance, message, clientId);
        }
    }

    public String getConsensusResult(int instance) {
        return _ledger.get(instance);
    }

    public IBFT getConsensusObject(){
        return _consensus;
    }

    public void printLedger() {
        System.out.println("Ledger of " + this._myInfo.getProcessId());
        int i = 0;
        String next = this._ledger.get(i);
        while (next != null) {
            System.out.println(i + " : " + next);

            next = this._ledger.get(++i);
        }
    }
}
