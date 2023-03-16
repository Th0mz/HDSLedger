package group13.blockchain.consensus;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import group13.blockchain.member.BMember;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.channel.bestEffortBroadcast.events.BEBSend;

public class IBFTByzantine extends IBFT {

    private int nrProcesses, byzantineP, quorum;
    private int pId, preparedRound, instance;
    private String input, preparedValue;
    private BEBroadcast broadcast;
    private BMember _server;
    private int round = 1;

    private Lock lock = new ReentrantLock();

    private int prepared, commited;
    private HashMap<String, Set<Integer>> prepares = new HashMap<>();
    private HashMap<String, Set<Integer>> commits = new HashMap<>();
    private int leader;

    private boolean isStartByzantine = false;
    private boolean isPrePrepareByzantine = false;
    private boolean isPrepareByzantine = false;
    private boolean isCommitByzantine = false;

    public IBFTByzantine(int id, int n, int f, int leader, BEBroadcast beb, BMember server) {
        super(id, n, f, leader, beb, server);
        pId = id;
        nrProcesses = n;
        byzantineP = f;
        quorum = (nrProcesses + byzantineP)/2 + 1;
        this.leader = leader;
        _server = server;
        broadcast = beb;
        broadcast.subscribeDelivery(this);
    }

    @Override
    public void start(int instance, String value) {

        this.instance = instance;
        input = value;
        round = 1;
        preparedRound = -1;
        preparedValue = null;

        if(isStartByzantine) {
            broadcast.send(new BEBSend("PRE_PREPARE\n" + instance + "\n" + round + "\n" + input));
        } else {
            if ( leader == pId ) {
                //broadcast
                ///Message -> PRE_PREPARE, instance, round, input
                broadcast.send(new BEBSend("PRE_PREPARE\n" + instance + "\n" + round + "\n" + input));
                //add signature ???
            }
        }
        
    }


    public void setStartByzantine() {isStartByzantine = true;}
    public void setPrePrepareByzantine() {isPrePrepareByzantine = true;}
    public void setPrepareByzantine() {isPrepareByzantine = true;}
    public void setCommitByzantine() {isCommitByzantine = true;}
    public void clearAllByzantine() {isStartByzantine = false; isPrePrepareByzantine=false; isPrepareByzantine=false;isCommitByzantine=false;}
    
}
