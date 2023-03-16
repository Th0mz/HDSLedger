package group13.blockchain.consensus;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import group13.blockchain.member.BMember;
import group13.blockchain.member.BMemberInterface;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.primitives.Address;

public class BMemberByzantine extends BMember {

    private ArrayList<Address> _listOfServers = new ArrayList<Address>();
    private Integer _id;
    private Integer _nrFaulty;
    private Integer _nrServers;
    private Integer _myIPort;
    private Address _myInfo;
    private boolean _isLeader;

    private BMemberInterface frontend;

    private IBFTByzantine _consensus;
    private ArrayList<String> _ledger = new ArrayList<String>();
    private Lock ledgerLock = new ReentrantLock();

  
    public void createBMemberByzantine(Integer id, ArrayList<Address> list, Integer nrFaulty, Integer nrServers,
                    Integer myIPort, Address myInfo, boolean isLeader) {
        _id = id;
        _listOfServers = list;
        _nrFaulty = nrFaulty;
        _nrServers = nrServers;
        _myIPort = myIPort;
        _myInfo = myInfo;
        _isLeader = isLeader;

        BEBroadcast beb = new BEBroadcast(_id, new Address(_myInfo.getPort()));
        for (int index = 0; index < _listOfServers.size(); index++ ) {
            beb.addServer(index, new Address(_listOfServers.get(index).getPort()));
        }
        _consensus = new IBFTByzantine(_id, _nrServers, _nrFaulty, 0, beb, this);

        frontend = new BMemberInterface(_id, _myIPort, this);
    }

    public String getConsensusResult(int instance) {
        return _ledger.get(instance);
    }

    public IBFTByzantine getConsensusObject(){
        return _consensus;
    }
    
}
