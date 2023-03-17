package group13.blockchain.consensus;

import java.util.ArrayList;

import group13.blockchain.member.BMember;
import group13.blockchain.member.BMemberInterface;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.primitives.Address;

public class BMemberByzantine extends BMember {

    @Override
    public void createBMember(ArrayList<Address> serverList, Integer nrFaulty, Integer nrServers,
                    Address interfaceAddress, Address myInfo, String leaderId) {
        _serverList = serverList;
        _nrFaulty = nrFaulty;
        _nrServers = nrServers;
        _myInfo = myInfo;
        _isLeader = myInfo.getProcessId().equals(leaderId);

        BEBroadcast beb = new BEBroadcast(myInfo);
        for (Address serverAddress : serverList) {
            beb.addServer(serverAddress);
        }


        _consensus = new IBFTByzantine(_nrServers, _nrFaulty, leaderId, beb, this);
        frontend = new BMemberInterface(interfaceAddress, this);
    }

    public String getConsensusResult(int instance) {
        return _ledger.get(instance);
    }

    public IBFTByzantine getConsensusObject(){
        return (IBFTByzantine) _consensus;
    }
    
}
