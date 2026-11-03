package dd.pp.interparaiment.view.actions;

import java.util.concurrent.atomic.AtomicBoolean;

import dd.pp.interparaiment.view.controls.UIUpdater;

public final class MessActionsContext {
    private final AtomicBoolean busy = new AtomicBoolean();
    private final UIUpdater updater;

    public MessActionsContext(UIUpdater updater) {
        this.updater = updater;
    }

    public boolean isBusy() {
        return busy.get();
    }

    public boolean beginOperation() {
        return busy.compareAndSet(false, true);
    }

    public void finishOperation() {
        busy.set(false);
    }

    public UIUpdater getUpdater() {
        return updater;
    }
}
