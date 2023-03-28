package group13.blockchain.member;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import group13.blockchain.auxiliary.IBFTBlock;
import group13.blockchain.commands.*;
import group13.blockchain.consensus.IBFT;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.primitives.Address;

public class BMember {

    public final static int FEE = 5;
    protected ArrayList<Address> _serverList = new ArrayList<Address>();
    protected HashMap<PublicKey, Integer> clients = new HashMap<PublicKey, Integer>();
    protected HashSet<PublicKey> pendingClients = new HashSet<PublicKey>();

    protected HashMap<PublicKey, HashSet<Integer>> receivedCommands = new HashMap<PublicKey, HashSet<Integer>>();
    private ReentrantLock receivedCommandsLock = new ReentrantLock();

    private ArrayList<BlockchainCommand> nextCommands = new ArrayList<BlockchainCommand>();
    private ReentrantLock nextCommandsLock = new ReentrantLock();

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

    private PublicKey myPubKey;

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

        try {
            String consensus_folder = new File("./src/main/java/group13/blockchain/consensus").getCanonicalPath();
            myPubKey = getPubKey(consensus_folder + "/" + _myInfo.getProcessId().substring(0, 5) + ".pub");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        _consensus = new IBFT(_nrServers, _nrFaulty, leaderId, beb, this);
        frontend = new BMemberInterface(interfaceAddress, this);
    }

    public void processCommand(Object command, String clientId) {
        //System.out.println("tryConsensus");
        if (!_isLeader)
            return;

        if (!(command instanceof SignedObject))
            return;

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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (bcommand != null) {
            String type = bcommand.getType();
            if(type.equals(RegisterCommand.constType)) {
                create_account((RegisterCommand) bcommand);
            } else if(type.equals(CheckBalanceCommand.constType)) {
                check_balance((CheckBalanceCommand) bcommand);
            } else if(type.equals(TransferCommand.constType)) {
                transfer((TransferCommand) bcommand);
            } else {
                return;
            }
        }

        //this.instances.put(nextInstance, clientId); ???
    }

    public void deliver(Integer instance, String message) {
        // TODO: CHANGE PID AND PORT FOR REAL VALUES BASED ON WHO ASKED FOR OPERATION
        /*System.out.println("CONSENSUS FINISHED");
        System.out.println("============================");
        System.out.println("============================");*/

        ledgerLock.lock();
        _ledger.put(instance, message); //TODO: DONT ALLOW BYZANTINE TO FORCE A MSG
        ledgerLock.unlock();

        if (_isLeader) {
            String clientId = instances.get(instance);
            frontend.ackClient(instance, message, clientId);
        }
    }

    public synchronized void create_account(RegisterCommand command) {
        receivedCommandsLock.lock();
        PublicKey commandPubKey = command.getPublicKey();
        Integer commandId = command.getSequenceNumber();

        if(clients.containsKey(commandPubKey) || pendingClients.contains(commandPubKey))
            return;

        //Guarantee all commands have a unique id+pubKey 'tuple'
        if(receivedCommands.containsKey(commandPubKey) && 
            receivedCommands.get(commandPubKey).contains(commandId))
            return;

        pendingClients.add(commandPubKey);
        if (!receivedCommands.containsKey(commandPubKey))
            receivedCommands.put(commandPubKey, new HashSet<Integer>());
        
        receivedCommands.get(commandPubKey).add(commandId);
        receivedCommandsLock.unlock();

        nextCommandsLock.lock();
        nextCommands.add(command);
        TransferCommand fee = new TransferCommand(-1, commandPubKey, myPubKey, FEE);
        nextCommands.add(fee);

        if (nextCommands.size() >= 10) {
            ledgerLock.lock();
            int nextInstance = _nextInstance;
            _nextInstance += 1;
            ledgerLock.unlock();
            
            ArrayList<BlockchainCommand> commandsToSend = nextCommands;
            IBFTBlock block = new IBFTBlock(commandsToSend, nextInstance);
            nextCommands = new ArrayList<BlockchainCommand>();

            _consensus.start(nextInstance, block);
        }
        nextCommandsLock.unlock();

        System.out.println("Called consensus for create_account");
        //TODO: add to next consensus block
    }

    public synchronized void check_balance(CheckBalanceCommand command) {

        receivedCommandsLock.lock();
        PublicKey commandPubKey = command.getPublicKey();
        Integer commandId = command.getSequenceNumber();

        //Guarantee all commands have a unique id+pubKey 'tuple'
        if(receivedCommands.containsKey(commandPubKey) && 
            receivedCommands.get(commandPubKey).contains(commandId))
            return;

        if (!receivedCommands.containsKey(commandPubKey))
            receivedCommands.put(commandPubKey, new HashSet<Integer>());
        
        receivedCommands.get(commandPubKey).add(commandId);
        receivedCommandsLock.unlock();

        nextCommandsLock.lock();
        nextCommands.add(command);
        if (nextCommands.size() >= 10) {
            ledgerLock.lock();
            int nextInstance = _nextInstance;
            _nextInstance += 1;
            ledgerLock.unlock();
            
            ArrayList<BlockchainCommand> commandsToSend = nextCommands;
            IBFTBlock block = new IBFTBlock(commandsToSend, nextInstance);
            nextCommands = new ArrayList<BlockchainCommand>();

            _consensus.start(nextInstance, block);
        }
        nextCommandsLock.unlock();
        System.out.println("Called consensus for check_balance");
        //TODO: DO CONSENSUS FOR STRONGLY CONSISTENT READ

    }

    public synchronized void transfer(TransferCommand command) {

        receivedCommandsLock.lock();
        PublicKey commandPubKey = command.getPublicKey();
        Integer commandId = command.getSequenceNumber();

        //Guarantee all commands have a unique id+pubKey 'tuple'
        if(receivedCommands.containsKey(commandPubKey) && 
            receivedCommands.get(commandPubKey).contains(commandId))
            return;

        if (!receivedCommands.containsKey(commandPubKey))
            receivedCommands.put(commandPubKey, new HashSet<Integer>());
        
        receivedCommands.get(commandPubKey).add(commandId);
        receivedCommandsLock.unlock();

        nextCommandsLock.lock();
        nextCommands.add(command);
        TransferCommand fee = new TransferCommand(-1, commandPubKey, myPubKey, FEE);
        nextCommands.add(fee);

        if (nextCommands.size() >= 10) {
            ledgerLock.lock();
            int nextInstance = _nextInstance;
            _nextInstance += 1;
            ledgerLock.unlock();
            
            ArrayList<BlockchainCommand> commandsToSend = nextCommands;
            IBFTBlock block = new IBFTBlock(commandsToSend, nextInstance);
            nextCommands = new ArrayList<BlockchainCommand>();

            _consensus.start(nextInstance, block);
        }
        nextCommandsLock.unlock();
        System.out.println("Called consensus for transfer");
        //TODO: DO CONSENSUS AND CHECK VALIDITY ONLY AFTER DELIVER
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

}
