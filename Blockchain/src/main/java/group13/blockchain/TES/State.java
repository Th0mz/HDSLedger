package group13.blockchain.TES;

import group13.blockchain.auxiliary.IBFTBlock;
import group13.blockchain.commands.BlockchainCommand;
import group13.blockchain.commands.CheckBalanceCommand;
import group13.blockchain.commands.RegisterCommand;
import group13.blockchain.commands.TransferCommand;

import java.security.PublicKey;
import java.util.HashMap;

public class State {

    public final static int FEE = 5;
    public final static int INITIAL_BALANCE = 100;

    public HashMap<PublicKey, Account> accounts = new HashMap<>();

    public void applyBlock(IBFTBlock block) {
        PublicKey minerPK = block.getMiner();

        for(BlockchainCommand command : block.getCommandsList()) {
            String commandType = command.getType();
            if(RegisterCommand.constType.equals(commandType))
                applyRegister((RegisterCommand) command);
            else if (TransferCommand.constType.equals(commandType))
                applyTransfer((TransferCommand) command, minerPK);
            else if (CheckBalanceCommand.constType.equals(commandType))
                applyCheckBalance((CheckBalanceCommand) command);
            else {
                System.out.println("Error : Unknown command");
            }
        }
    }

    public void applyRegister(RegisterCommand command) {
        PublicKey accountPK = command.getPublicKey();
        if(!accounts.containsKey(accountPK)) {
            Account newAccount = new Account(accountPK, INITIAL_BALANCE);
            accounts.put(accountPK, newAccount);
        }

        System.out.println("->REGISTERED CLIENT");
    }

    public void applyTransfer(TransferCommand command, PublicKey minerPK) {
        PublicKey senderPK = command.getPublicKey();
        PublicKey destinationPK = command.getDestPublicKey();

        // check if all accounts involved in the transaction exist
        // (sender, receiver and miner)
        if(accounts.containsKey(senderPK) &&
            accounts.containsKey(destinationPK) &&
            accounts.containsKey(minerPK)) {

            Account sender = accounts.get(senderPK);
            float amount = command.getAmount();

            boolean applied = sender.send(amount, FEE);
            if (applied) {
                Account destination = accounts.get(destinationPK);
                Account miner = accounts.get(minerPK);

                destination.receive(amount);
                miner.receive(FEE);
            }
        }

        System.out.println("->APPLIED TRANSFER");
    }

    public void applyCheckBalance(CheckBalanceCommand command) {

        PublicKey accountPK = command.getPublicKey();
        if(accounts.containsKey(accountPK)){
            Account account = accounts.get(accountPK);
            float balance = account.getBalance();

            //TODO: Send to client his balance
            System.out.println("->CHECK BALANCE Client pubKey: "+ command.getPublicKey()
                    + "    | Amount: " + balance);
        }
    }
}
