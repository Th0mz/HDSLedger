package group13.primitives;

public class AboveModule {
    private EventHandler eventHandler;
    private AboveModuleListener eventListner;

    public AboveModule () {
        this.eventHandler = new EventHandler();
        this.eventListner = new AboveModuleListener();
    }

    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public AboveModuleListener getEventListner() {
        return eventListner;
    }
}
