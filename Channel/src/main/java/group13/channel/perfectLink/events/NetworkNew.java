package group13.channel.perfectLink.events;

import group13.channel.perfectLink.PerfectLink;
import group13.primitives.Event;

public class NetworkNew extends Event {

    public static final String EVENT_NAME = "networkNew";
    private PerfectLink link;

    public NetworkNew (PerfectLink link) {
        super(EVENT_NAME);
        this.link = link;
    }

    public PerfectLink getLink() {
        return link;
    }
}
