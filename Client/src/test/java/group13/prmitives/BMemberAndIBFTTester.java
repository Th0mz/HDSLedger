package group13.prmitives;

import group13.blockchain.TES.ClientResponse;
import group13.blockchain.TES.Snapshot;
import group13.blockchain.TES.SnapshotAccount;
import group13.blockchain.commands.BlockchainCommand;
import group13.blockchain.commands.RegisterCommand;
import group13.blockchain.consensus.IBFT;
import group13.blockchain.member.BMember;
import group13.blockchain.member.BMemberInterface;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.primitives.Address;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.util.ArrayList;
import java.util.List;

public class BMemberAndIBFTTester extends BMemberTester {

    public boolean alterClientRequest = false;
    public boolean sendRepeatedCommands = false;
    public boolean dropCommands = false;

    public void setByzantineRepeatedCommands() {((IBFTByzantine)_consensus).setByzantineRepeatedCommands();}
    public void setByzantineSuppressCommands() {((IBFTByzantine)_consensus).setByzantineSuppressCommands();}

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
        snapshot = new Snapshot(myPubKey, myPrivKey, _nrFaulty);

        _consensus = new IBFTByzantine(_nrServers, _nrFaulty, myPubKey, myPrivKey, leaderPK, beb, this);
        frontend = new BMemberInterfaceTester(myPubKey, myPrivKey, interfaceAddress, this);
    }

    @Override
    public void processCommand(Object command) {

        //System.out.println("PROCESS COMMAND CALLED");
        if (!(command instanceof SignedObject))
            return;

        // check if the command received from the client is valid
        SignedObject signedObject = (SignedObject) command;
        BlockchainCommand bcommand = null;
        try {
            if (!(signedObject.getObject() instanceof BlockchainCommand))
                return;

            bcommand = (BlockchainCommand) signedObject.getObject();
            if (!signedObject.verify(bcommand.getPublicKey(), Signature.getInstance("SHA256withRSA")))
                return;
            
        } catch (ClassNotFoundException | IOException | InvalidKeyException | 
                    SignatureException | NoSuchAlgorithmException  e) {
            e.printStackTrace();
        }

        // process command
        if (bcommand == null) {
            return;
        }

        //IF -> READS;   ELSE -> TRANSFERS/REGISTERS 
        if( bcommand.getType().equals("CHECK_BALANCE")){
            //System.out.println("READ COMMAND RECEIVED");
            super.processReads(bcommand);
        } else {
            //System.out.println("Added command of type " + bcommand.getType());
            if(_isLeader && !dropCommands) {
                if (alterClientRequest) {
                    try {
                        Signature signature = Signature.getInstance("SHA256withRSA");
                        SignedObject signed = new SignedObject(bcommand, myPrivKey, signature);
                        super.addCommand(signed);
                        System.out.println("LEADER SENT WRONGFULLY SIGNED");
                    } catch (IOException | InvalidKeyException | SignatureException | 
                            NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (sendRepeatedCommands) {
                        super.addCommand(signedObject);
                    }
                    super.addCommand(signedObject);
                }
                
            } else {
                super._consensus.waitForCommand(bcommand);
            }

        }
            
    }

    public void forceClientResponse(ClientResponse response) {
        // craft 2f + 2 equal responses and send them to the client
        ArrayList<ClientResponse> craftedResponses = new ArrayList<>();
        for (int i = 0; i < 2 * _nrFaulty + 2; i++) {
            craftedResponses.add(response);
        }

        frontend.sendResponses(craftedResponses);
    }
}
