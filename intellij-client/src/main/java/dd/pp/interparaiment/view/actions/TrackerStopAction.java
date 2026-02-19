package dd.pp.interparaiment.view.actions;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class TrackerStopAction extends AnAction {

    private final TrackingProcessState state;

    public TrackerStopAction(final TrackingProcessState state) {
        super("Stop Tracking", "Closes connection with tracking agent", AllIcons.Actions.Suspend);
        this.state = state;
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(state.isRunning());
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent anActionEvent) {
        state.setRunning(false);
    }
}
