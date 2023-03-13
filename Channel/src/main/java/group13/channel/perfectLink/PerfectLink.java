package group13.channel.perfectLink;

import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.primitives.Address;
import group13.primitives.EventHandler;
import group13.primitives.EventListener;

import java.util.HashMap;

public class PerfectLink {
    // packet definitions
    public static int HEADER_SIZE = 9;

    private int processId;
    private PerfectLinkIn in_link;
    private HashMap<Integer, PerfectLinkOut> out_links;

    private EventHandler plEventHandler;

    public PerfectLink (int processId, Address address) {
        this.plEventHandler = new EventHandler();

        this.out_links = new HashMap<>();
        this.processId = processId;

        this.in_link = new PerfectLinkIn(processId, address, this.plEventHandler, this.out_links);
        this.in_link.start();

    }

    public PerfectLinkOut createLink (int processId, Address destination) {
        PerfectLinkOut out_link = new PerfectLinkOut(this.processId, destination);
        out_links.put(processId, out_link);
        this.in_link.addSender(processId);

        return out_link;
    }

    public PerfectLinkOut removeLink (Address destination) {

        PerfectLinkOut out_link = new PerfectLinkOut(this.processId, destination);
        out_links.remove(out_link);

        return out_link;
    }

    public HashMap<Integer, PerfectLinkOut> getOutLinks() {
        return out_links;
    }

    public void subscribeDelivery(EventListener listener) {
        plEventHandler.subscribe(Pp2pDeliver.EVENT_NAME, listener);
    }

    public void close () {
        this.in_link.interrupt();
        this.in_link.close();
        for (int out_link_id : this.out_links.keySet()) {
            this.out_links.get(out_link_id).close();
        }
    }

}
