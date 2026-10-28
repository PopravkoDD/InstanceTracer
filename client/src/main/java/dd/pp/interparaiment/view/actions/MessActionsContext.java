package dd.pp.interparaiment.view.actions;

import dd.pp.interparaiment.view.controls.UIUpdater;

public class MessActionsContext {
    private volatile boolean isRunning = false;
    private volatile boolean isStarting = false;
    private final UIUpdater updater;

    public MessActionsContext(UIUpdater updater) {
        this.updater = updater;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(final boolean isRunning) {
        this.isRunning = isRunning;
    }

    public boolean isStarting() {
        return isStarting;
    }

    public void setStarting(boolean starting) {
        isStarting = starting;
    }

    public UIUpdater getUpdater() {
        return this.updater;
    }
}
