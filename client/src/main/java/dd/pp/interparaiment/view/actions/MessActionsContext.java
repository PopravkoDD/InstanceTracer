package dd.pp.interparaiment.view.actions;

import dd.pp.interparaiment.view.controls.UIUpdater;

public class MessActionsContext {
    private final UIUpdater updater;

    public MessActionsContext(UIUpdater updater) {
        this.updater = updater;
    }

    public UIUpdater getUpdater() {
        return this.updater;
    }
}
