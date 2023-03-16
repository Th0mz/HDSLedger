package group13.blockchain.consensus;

import group13.blockchain.member.BMember;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.channel.bestEffortBroadcast.events.BEBDeliver;

import group13.channel.bestEffortBroadcast.events.BEBSend;
import group13.primitives.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class IBFT implements EventListener{
    
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
    
    //Timer (eventually)

    private int leader;

    public IBFT(int id, int n, int f, int leader, BEBroadcast beb, BMember server) {
        pId = id;
        nrProcesses = n;
        byzantineP = f;
        quorum = (nrProcesses + byzantineP)/2 + 1;
        this.leader = leader;
        _server = server;
        broadcast = beb;
        broadcast.subscribeDelivery(this);
    }

    private int leader(int instance, int round) {
        return round % nrProcesses;
    }

    public void start(int instance, String value) {

        this.instance = instance;
        input = value;
        round = 1;
        preparedRound = -1;
        preparedValue = null;

        if ( leader == pId ) {
            //broadcast
            ///Message -> PRE_PREPARE, instance, round, input
            broadcast.send(new BEBSend("PRE_PREPARE\n" + instance + "\n" + round + "\n" + input));
            //add signature ???
        }

        //no timer for now
    }


    public void update(Event event) {
        if (event.getEventName() == BEBDeliver.EVENT_NAME) {
            BEBDeliver typed_event = (BEBDeliver) event;
            String payload = typed_event.getPayload();
            int src = typed_event.getProcessID();

            //problema aqui se o input acabar com espaço
            String[] params = payload.split("\n");

            String msgType = params[0];
            System.out.println("-------------------------");
            System.out.println("RECEIVED UPDATE WITH MESSAGE: " + params[0]);
            System.out.println("-------------------------");
            
            //verify signature
            //check round matches
            //here verify leader using params[2] or round??
            if( msgType.equals("PRE_PREPARE") && leader == src && round == Integer.parseInt(params[2])) {
                //what else to verify if valid besides signature??
                prePrepare(params, src);
            } else if (msgType.equals("PREPARE") && round == Integer.parseInt(params[2])) {
                prepare(params, src);
            } else if (msgType.equals("COMMIT")) {
                commit(params, src);
            }
        }
    }

    public void prePrepare(String[] params, int src){
        //timer -- maybe not for now
        System.out.println("-----------------------");
        System.out.println("-----------------------");
        System.out.println("PRE PREPARE");
        System.out.println("Value: " + params[3]);
        System.out.println("-----------------------");
        System.out.println("-----------------------");
        String payload = "PREPARE\n" + params[1] + "\n" + params[2] + "\n" + params[3];
        BEBSend send_event = new BEBSend(payload);

        this.broadcast.send(send_event);
    }

    public void prepare(String[] params, int src) {

        String key = params[1]+params[2]+params[3];
        Set<Integer> setPrepares;
        if(! prepares.containsKey(key) ) {
            setPrepares = new HashSet<Integer>();
            prepares.put(key, setPrepares);
            System.out.println("ADDED KEY: " + key);
        }
        
        setPrepares = prepares.get(key);
        int prepareCount = setPrepares.size();
        
        if (prepareCount < quorum) {

            System.out.println("SIZE BEFORE: " + prepareCount);
            setPrepares.add(src);
            prepareCount = setPrepares.size();
            prepares.put(key, setPrepares);
            System.out.println("SIZE AFTER: " + prepareCount);

            if( prepareCount >= quorum ) {
    
                preparedRound = Integer.parseInt(params[2]);
                preparedValue = params[3];
                broadcast.send(new BEBSend("COMMIT\n" + params[1] + "\n" + params[2] + "\n" + params[3]));
                System.out.println("SENT BROADCAST OF COMMIT");
            }
        }
    }

    public void commit(String[] params, int src) {

        Set<Integer> setCommits;
        String key = params[1]+params[2]+params[3];

        if(! commits.containsKey(key) ) {
            setCommits = new HashSet<Integer>();
            commits.put(key, setCommits);
        }
        setCommits = commits.get(key);
        int commitCount = setCommits.size();

        if (commitCount < quorum) {
            
            setCommits.add(src);
            commitCount = setCommits.size();
            commits.put(key, setCommits);
    
            if( commitCount >= quorum) {
                //timer stuff
                System.out.println("-----------------------");
                System.out.println("-----------------------");
                System.out.println("DECIDE");
                System.out.println("Value decided: " + params[3]);
                System.out.println("-----------------------");
                System.out.println("-----------------------");
                _server.deliver(Integer.parseInt(params[1]), params[3]);
                // DECIDE( params[1], params[3], commits.get(key));
            }
        }
    }

}
