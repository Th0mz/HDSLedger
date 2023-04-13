package group13.blockchain.TES;

import group13.blockchain.auxiliary.IBFTBlock;
import group13.blockchain.commands.BlockchainCommand;
import group13.blockchain.commands.CheckBalanceCommand;
import group13.blockchain.commands.RegisterCommand;
import group13.blockchain.commands.TransferCommand;

import java.io.IOException;
import java.security.PublicKey;
import java.security.SignedObject;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class State {

    public final static int FEE = 5;
    public final static int INITIAL_BALANCE = 100;

    private HashMap<PublicKey, Account> accounts = new HashMap<>();

    public List<ClientResponse> applyBlock(IBFTBlock block) {
        PublicKey minerPK = block.getMiner();
        List<ClientResponse> responses = new ArrayList<>();
        ClientResponse response = null;

        for(SignedObject obj : block.getCommandsList()) {
            try {
                BlockchainCommand command = (BlockchainCommand) obj.getObject();
                String commandType = command.getType();

                if(RegisterCommand.constType.equals(commandType))
                    response = applyRegister((RegisterCommand) command);
                else if (TransferCommand.constType.equals(commandType))
                    response = applyTransfer((TransferCommand) command, minerPK);
                else if (CheckBalanceCommand.constType.equals(commandType))
                    response = applyCheckBalance((CheckBalanceCommand) command);
                else {
                    System.out.println("Error : Unknown command");
                    response = null;
                }

                if (response != null) {
                    responses.add(response);
                }

            } catch (ClassNotFoundException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return responses;
    }

    public ClientResponse applyRegister(RegisterCommand command) {
        boolean applied = false;
        PublicKey accountPK = command.getPublicKey();
        if(!accounts.containsKey(accountPK)) {
            Account newAccount = new Account(accountPK, INITIAL_BALANCE);
            accounts.put(accountPK, newAccount);
            applied = true;
        }
        

        ClientResponse response = new ClientResponse(command, applied);
        return response;
    }

    public ClientResponse applyTransfer(TransferCommand command, PublicKey minerPK) {
        boolean applied = false;
        PublicKey senderPK = command.getPublicKey();
        PublicKey destinationPK = command.getDestPublicKey();

        // check if all accounts involved in the transaction exist
        // (sender, receiver and miner)
        if(accounts.containsKey(senderPK) &&
            accounts.containsKey(destinationPK) &&
            accounts.containsKey(minerPK)) {

            Account sender = accounts.get(senderPK);
            float amount = command.getAmount();

            applied = sender.send(amount, FEE);
            if (applied) {
                Account destination = accounts.get(destinationPK);
                Account miner = accounts.get(minerPK);

                destination.receive(amount);
                miner.receive(FEE);
            }
        }
        //System.out.println("\n========================\n========================\n========================");

       // System.out.println("->APPLIED TRANSFER");
       // System.out.println("\n========================\n========================\n========================");

        ClientResponse response = new ClientResponse(command, applied);
        return response;
    }

    public ClientResponse applyCheckBalance(CheckBalanceCommand command) {
        boolean applied = false;
        float balance = -1;

        PublicKey accountPK = command.getPublicKey();
        if(accounts.containsKey(accountPK)){
            Account account = accounts.get(accountPK);
            balance = account.getBalance();
            applied = true;

            //TODO: Send to client his balance
            //System.out.println("->CHECK BALANCE    | Amount: " + balance);
        }

        ClientResponse response = new ClientResponse(command, balance, applied);
        return response;
    }

    public HashMap<PublicKey, Account> getAccounts() {
        return accounts;
    }

    public void print() {

        System.out.println("==========================================");
        System.out.println("                TES STATE                 ");
        System.out.println("==========================================");
        for (PublicKey key : accounts.keySet()) {
            Account account = accounts.get(key);
            RSAPublicKey rsaKey = (RSAPublicKey) key;
            System.out.println("Id : " + rsaKey.getPublicExponent() + "   Amount = " + account.getBalance());
        }

        System.out.println("==========================================");
    }
}
