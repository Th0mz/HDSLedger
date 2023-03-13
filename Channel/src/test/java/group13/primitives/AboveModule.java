package group13.primitives;

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
