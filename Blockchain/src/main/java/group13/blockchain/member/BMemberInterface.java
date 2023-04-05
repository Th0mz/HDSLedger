package group13.blockchain.member;

import group13.blockchain.TES.ClientResponse;
import group13.channel.bestEffortBroadcast.BEBroadcast;
import group13.channel.bestEffortBroadcast.events.BEBDeliver;
import group13.channel.bestEffortBroadcast.events.BEBSend;
import group13.primitives.Address;
import group13.primitives.Event;
import group13.primitives.EventListener;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

public class BMemberInterface implements EventListener {

    private BEBroadcast beb;
    private BMember _server;

    public BMemberInterface(PublicKey publicKey, PrivateKey privateKey, Address memberAddress, BMember server) {
        _server = server;
        beb = new BEBroadcast(memberAddress, publicKey, privateKey);
        beb.subscribeDelivery(this);

        System.out.println("Started process " +  memberAddress.getPort() );
    }

    @Override
    public void update(Event event) {
        String eventType = event.getEventName();
        if (!eventType.equals(BEBDeliver.EVENT_NAME)) {
            System.out.println("Should only receive deliver events (??)");
        }


        BEBDeliver ev = (BEBDeliver) event;
        PublicKey clientPK = ev.getProcessPK();

        Object payload = ev.getPayload();
        _server.processCommand(payload);
    }

    public void sendResponses(List<ClientResponse> responses) {
        for (ClientResponse response : responses) {
            PublicKey issuerPK = response.getIssuer();
            BEBSend sendEvent = new BEBSend(response);

            boolean sent = beb.unicast(issuerPK, sendEvent);
            if (!sent) {
                System.err.println("Error : Link wasn't found for unicast response");
            }
        }
    }

    public void close() {
        beb.close();
    }
}
