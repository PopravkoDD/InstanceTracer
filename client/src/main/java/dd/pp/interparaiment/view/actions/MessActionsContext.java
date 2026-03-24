package dd.pp.interparaiment.view.actions;

import dd.pp.interparaiment.view.controls.UIUpdater;

public class MessActionsContext {
    private boolean isRunning = false;
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

    public UIUpdater getUpdater() {
        return this.updater;
    }
}
