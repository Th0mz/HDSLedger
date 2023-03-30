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
import group13.blockchain.TES.State;
public class BMember {

    protected ArrayList<Address> _serverList = new ArrayList<Address>();

    protected State tesState = new State();
    protected HashMap<PublicKey, HashSet<Integer>> receivedCommands = new HashMap<PublicKey, HashSet<Integer>>();
    private ReentrantLock receivedCommandsLock = new ReentrantLock();

    private ArrayList<BlockchainCommand> nextCommands = new ArrayList<BlockchainCommand>();
    private ReentrantLock nextCommandsLock = new ReentrantLock();

    protected Integer _nrFaulty;
    protected Integer _nrServers;
    protected Address _myInfo;
    protected boolean _isLeader;

    private int _nextInstance;
    protected BMemberInterface frontend;

    protected IBFT _consensus;
    protected HashMap<Integer, IBFTBlock> _ledger = new HashMap<>();
    private int lastApplied = -1;
    protected Lock lastAppliedLock = new ReentrantLock();
    protected Lock ledgerLock = new ReentrantLock();

    private PublicKey myPubKey;


    public void createBMember(ArrayList<Address> serverList, Integer nrFaulty, Integer nrServers,
                    Address interfaceAddress, Address myInfo, String leaderId) {

        _serverList = serverList;
        _nrFaulty = nrFaulty;
        _nrServers = nrServers;
        _myInfo = myInfo;
        _isLeader = myInfo.getProcessId().equals(leaderId);
        _nextInstance = 0;

        BEBroadcast beb = new BEBroadcast(myInfo);
        

        try {
            String consensus_folder = new File("./src/main/java/group13/blockchain/consensus").getCanonicalPath();
            myPubKey = getPubKey(consensus_folder + "/" + _myInfo.getProcessId().substring(0, 5) + ".pub");
            for (Address serverAddress : serverList) {
                beb.addServer(serverAddress);
                String outProcessId = serverAddress.getProcessId();
                PublicKey key = getPubKey(consensus_folder + "/" + outProcessId.substring(0, 5) + ".pub");

                // TODO : hard code for now, but best practice is probably include
                // their registration on the first block of the blockchain
                RegisterCommand command = new RegisterCommand(-1, key);
                tesState.applyRegister(command);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        _consensus = new IBFT(_nrServers, _nrFaulty, leaderId, beb, this);
        frontend = new BMemberInterface(interfaceAddress, this);
    }

    public void processCommand(Object command, String clientId) {
        if (!_isLeader)
            return;

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

        addCommand(bcommand);
    }

    // add command to the received list and initiate a new consensus instance if
    // there are sufficient commands to complete the block
    public void addCommand(BlockchainCommand command) {
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
        nextCommands.add(command);

        if (nextCommands.size() >= IBFTBlock.BLOCK_SIZE) {
            ledgerLock.lock();
            int nextInstance = _nextInstance;
            _nextInstance += 1;
            ledgerLock.unlock();

            ArrayList<BlockchainCommand> commandsToSend = nextCommands;
            IBFTBlock block = new IBFTBlock(this.myPubKey, commandsToSend, nextInstance);
            nextCommands = new ArrayList<BlockchainCommand>();

            _consensus.start(nextInstance, block);
        }
        nextCommandsLock.unlock();

        System.out.println("Called consensus for create_account");
        //TODO: add to next consensus block
    }

    public void deliver(Integer instance, IBFTBlock block) {
        //TODO: Save operations. If in order: 1) validate operation; 2)apply
        
        System.out.println("DELIVERED BLOCK: " + block);
        ledgerLock.lock();
        _ledger.put(instance, block);
        lastAppliedLock.lock();
        int next = lastApplied + 1;
        IBFTBlock nextBlock = _ledger.get(next);
        while (nextBlock != null) {
            this.tesState.applyBlock(nextBlock);
            next++;
            nextBlock = _ledger.get(next);  
        }

        lastApplied = next - 1;
        lastAppliedLock.unlock();
        ledgerLock.unlock();
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

}
