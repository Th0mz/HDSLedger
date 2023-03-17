package group13.prmitives;

import group13.primitives.EventHandler;

public class AboveModule {
    private EventHandler eventHandler;
    private AboveModuleListener eventListner;

    public AboveModule () {
        this.eventListner = new AboveModuleListener();
    }

    public AboveModuleListener getEventListner() {
        return eventListner;
    }
}
