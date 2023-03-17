package group13.blockchain.consensus;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import group13.blockchain.member.BMember;
import group13.blockchain.member.BMemberInterface;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.primitives.Address;

public class BMemberByzantine extends BMember {

    @Override
    public void createBMember(Integer id, ArrayList<Address> list, Integer nrFaulty, Integer nrServers,
                    Integer myIPort, Address myInfo, boolean isLeader) {
        _id = id;
        _listOfServers = list;
        _nrFaulty = nrFaulty;
        _nrServers = nrServers;
        _myIPort = myIPort;
        _myInfo = myInfo;
        _isLeader = isLeader;
        for (int i = 0; i < 1000; i++) {
            _ledger.add("");
        }

        BEBroadcast beb = new BEBroadcast(new Address(_myInfo.getPort()));
        for (int index = 0; index < _listOfServers.size(); index++ ) {
            beb.addServer(new Address(_listOfServers.get(index).getPort()));
        }
        _consensus = new IBFTByzantine(_id, _nrServers, _nrFaulty, 0, beb, this);
        frontend = new BMemberInterface(_id, _myIPort, this);
    }

    public String getConsensusResult(int instance) {
        return _ledger.get(instance);
    }

    public IBFTByzantine getConsensusObject(){
        return (IBFTByzantine) _consensus;
    }
    
}
