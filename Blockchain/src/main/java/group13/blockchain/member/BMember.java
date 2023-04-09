package group13.blockchain.member;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import group13.blockchain.TES.ClientResponse;
import group13.blockchain.auxiliary.IBFTBlock;
import group13.blockchain.commands.*;
import group13.blockchain.consensus.IBFT;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.primitives.Address;
import group13.blockchain.TES.State;
public class BMember {

    protected ArrayList<Address> _serverList = new ArrayList<Address>();

    protected State tesState = new State();
    protected HashMap<PublicKey, HashSet<Integer>> receivedCommands = new HashMap<PublicKey, HashSet<Integer>>();
    private ReentrantLock receivedCommandsLock = new ReentrantLock();

    private ArrayList<SignedObject> nextCommands = new ArrayList<SignedObject>();
    private ReentrantLock nextCommandsLock = new ReentrantLock();

    protected Integer _nrFaulty;
    protected Integer _nrServers;
    protected Address _myInfo;
    protected boolean _isLeader;

    protected int _nextInstance;
    protected BMemberInterface frontend;

    protected IBFT _consensus;
    protected HashMap<Integer, IBFTBlock> _ledger = new HashMap<>();
    private int lastApplied = -1;
    protected Lock lastAppliedLock = new ReentrantLock();
    protected Lock ledgerLock = new ReentrantLock();
    protected Lock tentativeReadLock = new ReentrantLock();

    protected PublicKey myPubKey;
    protected PrivateKey myPrivKey;

    protected int _snapShotInstance = -1;

    private HashMap<Integer, ArrayList<CheckBalanceCommand>> tentativeReads = new HashMap<>();
    private HashMap<PublicKey, HashMap<Integer, HashMap<PublicKey,byte[]>>> snapShot;

    public void createBMember(ArrayList<Address> serverList, Integer nrFaulty, Integer nrServers,
                    Address interfaceAddress, Address myInfo, Address leaderAddress) {

        _serverList = serverList;
        _nrFaulty = nrFaulty;
        _nrServers = nrServers;
        _myInfo = myInfo;
        _isLeader = leaderAddress.equals(_myInfo);
        _nextInstance = 0;

        PublicKey leaderPK = null;
        BEBroadcast beb = null;
        try {
            String inProcessId = _myInfo.getProcessId();
            String consensus_folder = new File("./src/main/java/group13/blockchain/consensus").getCanonicalPath();

            myPubKey = getPubKey(consensus_folder + "/" + inProcessId.substring(0, 5) + ".pub");
            myPrivKey = getPrivateKey(consensus_folder + "/" + inProcessId.substring(0, 5) + ".key");
            beb = new BEBroadcast(myInfo, myPubKey, myPrivKey);

            for (Address serverAddress : serverList) {
                String outProcessId = serverAddress.getProcessId();
                PublicKey outPublicKey = getPubKey(consensus_folder + "/" + outProcessId.substring(0, 5) + ".pub");

                // set leader public key
                if (leaderAddress.equals(serverAddress)) {
                    leaderPK = outPublicKey;
                }

                beb.addServer(serverAddress, outPublicKey);

                // TODO : hard code for now, but best practice is probably include
                // their registration on the first block of the blockchain
                RegisterCommand command = new RegisterCommand(-1, outPublicKey);
                tesState.applyRegister(command);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        _consensus = new IBFT(_nrServers, _nrFaulty, myPubKey, myPrivKey, leaderPK, beb, this);
        frontend = new BMemberInterface(myPubKey, myPrivKey, interfaceAddress, this);
    }

    public void processCommand(Object command) {
        //if (!_isLeader)
        //    return;

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
            CheckBalanceCommand readCommand = (CheckBalanceCommand)bcommand;
            int lastSeenClient = readCommand.getLastViewSeen();
            PublicKey client = readCommand.getPublicKey();
            // STRONG READ & WEAK READ
            if (readCommand.getIsConsistent()) {
                
                lastAppliedLock.lock();
                int lastInstance = _consensus.getMaxSeenValidInstance(); //_nextInstance - 1;
                

                if (lastInstance >= lastSeenClient && lastInstance == lastApplied) {
                    float balance = tesState.accounts.get(client).getBalance();

                    System.out.println("STRONG READ @instance " + lastInstance + "client Balance = " + balance );
                    
                    List<ClientResponse> responses = new ArrayList<>();
                    responses.add(new ClientResponse(bcommand, balance, true, lastApplied));
                    frontend.sendResponses(responses);

                } else if (lastInstance >= lastSeenClient && lastInstance > lastApplied) {
                    //wait for tentative operations to apply then respond
                    //tentativeReadLock.lock();
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
                    responses.add(new ClientResponse(bcommand, -1, false));
                    frontend.sendResponses(responses);
                }

                lastAppliedLock.unlock();

            } else {
                //TODO - weak read; snapshots;
            }

        } else {

            System.out.println("Added command of type " + bcommand.getType());
            if(_isLeader) {
                addCommand(signedObject);
            } else {
                _consensus.waitForCommand(bcommand);
            }
        }

            
    }

    // add command to the received list and initiate a new consensus instance if
    // there are sufficient commands to complete the block
    public void addCommand(SignedObject obj) {
        BlockchainCommand command;
        try {
            if (!(obj.getObject() instanceof BlockchainCommand))
                return;
            command = (BlockchainCommand) obj.getObject();
        } catch (ClassNotFoundException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        PublicKey commandPubKey = command.getPublicKey();
        Integer commandId = command.getSequenceNumber();

        receivedCommandsLock.lock();

        //Guarantee all commands have a unique id+pubKey 'tuple'
        if(receivedCommands.containsKey(commandPubKey) &&
                receivedCommands.get(commandPubKey).contains(commandId)) {
            receivedCommandsLock.unlock();
            return;
        }

        if (!receivedCommands.containsKey(commandPubKey))
            receivedCommands.put(commandPubKey, new HashSet<Integer>());

        receivedCommands.get(commandPubKey).add(commandId);
        receivedCommandsLock.unlock();

        nextCommandsLock.lock();
        nextCommands.add(obj);

        if (nextCommands.size() >= IBFTBlock.BLOCK_SIZE) {
            ledgerLock.lock();
            int nextInstance = _nextInstance;
            _nextInstance += 1;
            ledgerLock.unlock();

            ArrayList<SignedObject> commandsToSend = nextCommands;
            IBFTBlock block = new IBFTBlock(this.myPubKey, commandsToSend, nextInstance);
            nextCommands = new ArrayList<SignedObject>();

            System.out.println("Calling consensus for block : " + block);
            _consensus.start(nextInstance, block);
        }

        nextCommandsLock.unlock();
        //TODO: add to next consensus block
    }

    public void deliver(Integer instance, IBFTBlock block) {
        //TODO: Save operations. If in order: 1) validate operation; 2)apply
        
        System.out.println("DELIVERED BLOCK: " + block);
        ledgerLock.lock();
        _ledger.put(instance, block);
        //lastAppliedLock.lock();
        int next = lastApplied + 1;
        IBFTBlock nextBlock = _ledger.get(next);
        while (nextBlock != null) {

            lastAppliedLock.lock();
            List<ClientResponse> responses = this.tesState.applyBlock(nextBlock);
            lastAppliedLock.unlock();
            frontend.sendResponses(responses);

            lastAppliedLock.lock();
            lastApplied = next;

            if(tentativeReads.get(nextBlock.getInstance()) != null) {
                ArrayList<CheckBalanceCommand> waitinReads = tentativeReads.get(nextBlock.getInstance());
                tentativeReads.remove(lastApplied);
                sendWaitingReads(waitinReads);
            }

            lastAppliedLock.unlock();

            
            next++;
            nextBlock = _ledger.get(next);  
        }

        //lastApplied = next - 1;
        //lastAppliedLock.unlock();
        ledgerLock.unlock();
    }

    private void sendWaitingReads(List<CheckBalanceCommand> reads) {
        List<ClientResponse> responses = new ArrayList<>();
        for (CheckBalanceCommand cmd : reads) {
            float balance = tesState.accounts.get(cmd.getPublicKey()).getBalance();
            System.out.println("STRONG READ (waited) @instance " + lastApplied + "client Balance = " + balance );
            
            responses.add(new ClientResponse(cmd, balance, true, lastApplied));
        }
        frontend.sendResponses(responses);
    }

    public IBFTBlock getConsensusResult(int instance) {
        return _ledger.get(instance);
    }

    public IBFT getConsensusObject(){
        return _consensus;
    }

    private static PublicKey getPubKey(String file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);
            fis.close();
            X509EncodedKeySpec ks = new X509EncodedKeySpec(bytes, "RSA");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey pub = kf.generatePublic(ks);
            return pub;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        //hack
        PublicKey k = null;
        return k;
    }

    private static PrivateKey getPrivateKey(String file) {

        try {
            //Encoder enc = Base64.getEncoder();
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);
            fis.close();
            //System.out.println(new String(bytes));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        //hack
        PrivateKey k = null;
        return k;
    }

    public void close() {
        _consensus.close();
        frontend.close();
    }

}
