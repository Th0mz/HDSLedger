package group13.prmitives;

import group13.blockchain.commands.RegisterCommand;
import group13.blockchain.consensus.IBFT;
import group13.blockchain.member.BMember;
import group13.blockchain.member.BMemberInterface;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.primitives.Address;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class BMemberTester extends BMember {

    public void createBMemberTester(ArrayList<Address> serverList, List<PublicKey> serverPKs, Integer nrFaulty, Integer nrServers,
                              Address interfaceAddress, Address myInfo, KeyPair myKeys, Address leaderAddress) {

        _serverList = serverList;
        _nrFaulty = nrFaulty;
        _nrServers = nrServers;
        _myInfo = myInfo;
        _isLeader = leaderAddress.equals(_myInfo);
        _nextInstance = 0;

        PublicKey leaderPK = null;
        myPubKey = myKeys.getPublic();
        myPrivKey = myKeys.getPrivate();

        BEBroadcast beb = new BEBroadcast(myInfo, myPubKey, myPrivKey);
        for (int i = 0; i < serverList.size(); i++) {
            Address outAddress = serverList.get(i);
            PublicKey outPublicKey = serverPKs.get(i);

            beb.addServer(outAddress, outPublicKey);

            if (leaderAddress.equals(outAddress)) {
                leaderPK = outPublicKey;
            }

            RegisterCommand command = new RegisterCommand(-1, outPublicKey);
            tesState.applyRegister(command);
        }

        _consensus = new IBFT(_nrServers, _nrFaulty, myPubKey, myPrivKey, leaderPK, beb, this);
        frontend = new BMemberInterface(myPubKey, myPrivKey, interfaceAddress, this);
    }
}
