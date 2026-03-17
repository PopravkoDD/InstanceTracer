package dd.pp.interparaiment.view.actions;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.requests.StopInspectionRequest;

public class TrackerStopAction extends AnAction {
    private final EventManager eventManager;
    private final MessActionsContext state;

    public TrackerStopAction(final MessActionsContext state, final EventManager eventManager) {
        super("Stop Tracking", "Closes connection with tracking agent", AllIcons.Actions.Suspend);
        this.state = state;
        this.eventManager = eventManager;
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(state.isRunning());
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent anActionEvent) {
        state.setRunning(false);
        eventManager.notify(new StopInspectionRequest());
    }
}
