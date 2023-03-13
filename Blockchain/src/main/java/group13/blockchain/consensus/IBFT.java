package group13.blockchain.consensus;

import group13.channel.bestEffortBroadcast;
import group13.primitives;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class IBFT implements EventListener{
    
    private int nrProcesses, byzantineP, quorum;
    private int pId, round, preparedRound, instance;
    private string input, preparedValue;
    private BEBroadcast broadcast;

    private Lock lock = new ReentrantLock();

    private int prepared, commited;
    private HashMap<String, Integer> prepares = new HashMap<>();
    private HashMap<String, Integer> commits = new HashMap<>();
    
    //Timer (eventually)

    public IBFT(int id, int n, int f) {
        pId = id;
        nrProcesses = n;
        byzantineP = f;
        quorum = (nrProcesses + byzantineP)/2 + 1;
        broadcast = new BEBroadcast(pId, new Address(5000 + pId));
    }

    private int leader(int instance, int round) {
        return round % nrProcesses;
    }

    public void start(int instance, string value) {

        this.instance = instance;
        input = value;
        round = 1;
        preparedRound = -1;
        preparedValue = -1;

        if ( leader(instance, round) == pId ) {
            //broadcast
            ///Message -> PRE_PREPARE, instance, round, input
            broadcast.update(new BEBSend("PRE_PREPARE\n" + instance + "\n" + round + "\n" + input));
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

            //verify signature

            if( msgType.equals("PRE_PREPARE") && leader(params[1], params[2]) == src) {
                //what else to verify if valid besides signature??
                prePrepare(params, src);
            } else if (msgType.equals("PREPARE")) {
                prepare(params, src);
            } else if (msgType.equals("COMMIT")) {
                commit(params, src);
            }
        }
    }

    private void prePrepare(String[] params, int src){
        //timer -- maybe not for now
        broadcast.update(new BEBSend("PREPARE\n" + instance + "\n" + round + "\n" + input));

    }

    private void prepare(String[] params, int src) {

        String key = params[1]+params[2]+params[3];

        if(! prepares.containsKey(key) ) {
            prepares.put(key, new valueOf(0));
        }
        
        int prepareCount = prepares.get(key);
        
        if (prepareCount < quorum) {

            prepareCount += 1;
            prepares.put(key, prepareCount);
            

            if( prepareCount >= quorum ) {
    
                preparedRound = params[2];
                preparedValue = params[3];
                broadcast.update(new BEBSend("COMMIT\n" + params[1] + "\n" + params[2] + "\n" + params[3]));
            }
        }
    }

    private void commit(String[] params, int src) {

        String key = params[1]+params[2]+params[3];

        if(! commits.containsKey(key) ) {
            commits.put(key, new valueOf(0));
        }

        int commitCount = commits.get(key);

        if (commitCount < quorum) {
            commitCount += 1;
            commits.put(key, commitCount);
    
            if( commitCount >= quorum) {
                //timer stuff
                // DECIDE( params[1], params[3], Qcommit?? );
            }
        }
    }

}
