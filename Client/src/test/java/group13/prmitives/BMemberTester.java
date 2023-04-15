package group13.prmitives;

import group13.blockchain.TES.Account;
import group13.blockchain.TES.ClientResponse;
import group13.blockchain.TES.Snapshot;
import group13.blockchain.TES.SnapshotAccount;
import group13.blockchain.commands.BlockchainCommand;
import group13.blockchain.commands.CheckBalanceCommand;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class BMemberTester extends BMember {

    public boolean alterClientRequest = false;
    public boolean sendRepeatedCommands = false;
    public boolean dropCommands = false;
    public boolean fakeRead = false;
    public boolean read_awaited = false;
    public boolean respondWeakRead = true;
    public boolean falsify_signatures = false;

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

        _consensus = new IBFT(_nrServers, _nrFaulty, myPubKey, myPrivKey, leaderPK, beb, this);
        frontend = new BMemberInterfaceTester(myPubKey, myPrivKey, interfaceAddress, this);
    }

    public int getLedgerSize(){return _ledger.size();}

    public float getClientBalance(PublicKey pKey) { return tesState.getAccounts().get(pKey).getBalance();}

    public int getNumberClients() { return tesState.getAccounts().size();}

    public void falsify_signatures(boolean b){
        falsify_signatures = b;
    }

    public void decreaseLastApplied() {
        lastAppliedLock.lock();
        lastApplied--;
        lastAppliedLock.unlock();
    }
    public void increaseLastApplied() {
        lastAppliedLock.lock();
        lastApplied++;
        lastAppliedLock.unlock();
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
           if(fakeRead) processReads(bcommand);
           else super.processReads(bcommand);
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

    public void processReads(BlockchainCommand bcommand){
        CheckBalanceCommand readCommand = (CheckBalanceCommand)bcommand;
        int lastSeenClient = readCommand.getLastViewSeen();
        PublicKey client = readCommand.getPublicKey();
        // STRONG READ & WEAK READ
        if (readCommand.getIsConsistent()) {
           // System.out.println("\n\n\n STRONG \n\n\n client " + client + "\n\n\n " + tesState.accounts.get(client) + "\n\n\n\n");
            lastAppliedLock.lock();
            int lastInstance = _consensus.getMaxSeenValidInstance(); //_nextInstance - 1;
            

            if (lastInstance >= lastSeenClient && lastInstance == lastApplied) {
                // check if client exists
                HashMap<PublicKey, Account> accounts = tesState.getAccounts();
                List<ClientResponse> responses = new ArrayList<>();
                ClientResponse response = null;
                float balance = -1;
                if (!accounts.containsKey(client)) {
                    response = new ClientResponse(bcommand, balance, false, lastApplied);
                } else {
                    balance = accounts.get(client).getBalance();
                    response = new ClientResponse(bcommand, 100*Math.random(), true, lastApplied);
                }

                responses.add(response);
                System.out.println("\nxxxxxxxxxxxxxxxxxxxxxxx\nFAKRE READ SENT\n" + responses.get(0)  +"\n@node "+ myPrivKey.hashCode() +"\nxxxxxxxxxxxxxxxxxxxxxxx\n" );
                frontend.sendResponses(responses);

            } else if (lastInstance >= lastSeenClient && lastInstance > lastApplied) {
                //wait for tentative operations to apply then respond
                //tentativeReadLock.lock();
                read_awaited = true;
                ArrayList<CheckBalanceCommand> waitinReads = tentativeReads.get(lastInstance);
                if(waitinReads == null) {
                    tentativeReads.put(lastInstance, new ArrayList<CheckBalanceCommand>());
                }
                waitinReads = tentativeReads.get(lastInstance);
                //add read to waiting list
                waitinReads.add(readCommand);

                //tentativeReadLock.unlock();
            } else {
                List<ClientResponse> responses = new ArrayList<>();
                float balance = -1;
                responses.add(new ClientResponse(bcommand, balance, false));
                frontend.sendResponses(responses);
            }

            lastAppliedLock.unlock();

        } else {
            //TODO - weak read; snapshots;
            if(respondWeakRead){

                List<ClientResponse> responses = new ArrayList<>();
                snapLock.lock();
                System.out.println("[WEAK READ]");
                System.out.println(snapshot.isValidSnapshot());
                System.out.println(snapshot.getValidVersion());
                if (snapshot.isValidSnapshot() && (lastSeenClient <= snapshot.getValidVersion())) {
                    System.out.println("WEAK READ BEING PROCESSED @node: " + Base64.getEncoder().encodeToString(myPubKey.getEncoded()));
                    SnapshotAccount accBalance = snapshot.getBalance(client);
                    
                    if(falsify_signatures) {

                        for (PublicKey k :accBalance.getSignatures().keySet() ) {
                            try {
                                accBalance.getSignatures().put(k, new SignedObject(1000f, myPrivKey, Signature.getInstance("SHA256withRSA")));
                            } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException
                                    | IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                    int version = snapshot.getValidVersion();
                    snapLock.unlock();
                    
                    responses.add(new ClientResponse(bcommand, accBalance, true, version));
                    System.out.println("WR - nr of signatures:" + accBalance.getSignatures().size());
                } else {
                    snapLock.unlock();
                    float balance = -1;
                    responses.add(new ClientResponse(bcommand, balance, false));
    
                }
    
                System.out.println("WEAK READ SENT BACK TO CLIENT");
                frontend.sendResponses(responses);
            }

        }
    }

    public void respondWeakRead(boolean b){
        respondWeakRead = b;
    }

    public void deliver(Integer instance) {
        //TODO: Save operations. If in order: 1) validate operation; 2)apply
        

            if(tentativeReads.get(instance) != null) {
                ArrayList<CheckBalanceCommand> waitinReads = tentativeReads.get(instance);
                tentativeReads.remove(lastApplied);
                super.sendWaitingReads(waitinReads);
            }

             
        
    }

    public boolean getReadAwaited() {
        return read_awaited;
    }
    public void setRead(boolean v){
        fakeRead = v;
    }

    public int getLastApplied(){
        return lastApplied;
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
