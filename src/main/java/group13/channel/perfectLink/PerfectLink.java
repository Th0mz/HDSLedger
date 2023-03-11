package group13.channel.perfectLink;

import group13.channel.perfectLink.events.Pp2pDeliver;
import group13.primitives.Address;
import group13.primitives.EventHandler;
import group13.primitives.EventListener;

import java.util.ArrayList;
import java.util.List;

public class PerfectLink {

    private int processId;
    private PerfectLinkIn in_link;
    private List<PerfectLinkOut> out_links;

    private EventHandler plEventHandler;

    public PerfectLink (int processId, Address address) {
        this.plEventHandler = new EventHandler();

        this.in_link = new PerfectLinkIn(address, this.plEventHandler);
        this.out_links = new ArrayList<>();
        this.processId = processId;
    }

    public PerfectLinkOut createLink (Address destination) {
        PerfectLinkOut out_link = new PerfectLinkOut(this.processId, destination);
        out_links.add(out_link);

        return out_link;
    }

    public PerfectLinkOut removeLink (Address destination) {

        PerfectLinkOut out_link = new PerfectLinkOut(this.processId, destination);
        out_links.remove(out_link);

        return out_link;
    }

    public void subscribeDelivery(EventListener listener) {
        plEventHandler.subscribe(Pp2pDeliver.EVENT_NAME, listener);
    }

    public void close () {
        this.in_link.interrupt();
        this.in_link.close();
        for (PerfectLinkOut out_link : this.out_links) {
            out_link.close();
        }
    }

}
