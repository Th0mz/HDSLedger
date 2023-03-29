package group13.blockchain.TES;

import java.security.PublicKey;

public class Account {

    private PublicKey key;
    private float balance;

    public Account(PublicKey key, int balance) {
        this.key = key;
        this.balance = balance;
    }

    public PublicKey getKey() {
        return key;
    }

    public synchronized float getBalance() {
        return balance;
    }

    public synchronized boolean send(float amount, float fee) {
        if (this.balance > amount + fee) {
            this.balance -= (amount + fee);
            return true;
        }

        return false;
    }

    public synchronized void receive(float amount) {
        this.balance += amount;
    }
}
