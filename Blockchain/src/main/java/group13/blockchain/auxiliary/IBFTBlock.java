package group13.blockchain.auxiliary;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

import group13.blockchain.commands.BlockchainCommand;

public class IBFTBlock implements Serializable {
    private ArrayList<BlockchainCommand> listOfCommands = new ArrayList<BlockchainCommand>();
    private int instance;
    private UUID uniqueID = UUID.randomUUID();

    public IBFTBlock(ArrayList<BlockchainCommand> commands, int inst) {
        listOfCommands = commands;
        instance = inst;
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
        
        return true;
    }

    public String getId() {
        return uniqueID.toString();
    }

    @Override
    public int hashCode() {
        return uniqueID.hashCode();
    }
}
