package group13.blockchain.auxiliary;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.UUID;

import group13.blockchain.commands.BlockchainCommand;

public class IBFTBlock implements Serializable {

    public static int BLOCK_SIZE = 10;
    private ArrayList<BlockchainCommand> listOfCommands = new ArrayList<BlockchainCommand>();
    private int instance;

    private PublicKey miner;
    private UUID uniqueID = UUID.randomUUID();

    public IBFTBlock(PublicKey miner, ArrayList<BlockchainCommand> commands, int inst) {
        listOfCommands = commands;
        instance = inst;
        this.miner = miner;
    }

    public ArrayList<BlockchainCommand> getCommandsList() { return listOfCommands; }

    public int getInstance() { return instance; }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof IBFTBlock))
            return false;
        IBFTBlock blk = (IBFTBlock) o;

        if(instance != blk.getInstance())
            return false;
        
        if(listOfCommands.size() != blk.getCommandsList().size())
            return false;
        
        for(int i = 0; i < listOfCommands.size(); i++)
            if(!listOfCommands.get(i).equals(blk.getCommandsList().get(i)))
                return false;

        // check if the block was mined by the same entity
        return this.miner.equals(blk.getMiner());
    }

    public String getId() {
        return uniqueID.toString();
    }

    public PublicKey getMiner() {
        return miner;
    }

    @Override
    public int hashCode() {
        return uniqueID.hashCode();
    }
}
