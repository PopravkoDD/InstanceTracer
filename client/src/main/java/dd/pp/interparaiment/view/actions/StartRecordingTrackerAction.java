package dd.pp.interparaiment.view.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import dd.pp.interparaiment.InspectorViewModel;
import dd.pp.interparaiment.event.requests.RecordingStartedEvent;
import dd.pp.interparaiment.p2p.DataResolvingWorker.RecordingState;
import org.jetbrains.annotations.NotNull;

public final class StartRecordingTrackerAction extends AnAction {
    private final InspectorViewModel viewModel;

    public StartRecordingTrackerAction(InspectorViewModel viewModel) {
        super("Start Recording", "Start or resume recording in the connected inspection", AllIcons.Debugger.ThreadRunning);
        this.viewModel = viewModel;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        var inspection = viewModel.getActiveInspection();
        event.getPresentation().setEnabled(inspection != null && inspection.isConnected()
                && inspection.getRecordingState() == RecordingState.PAUSED);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        var inspection = viewModel.getActiveInspection();
        if (inspection == null) {
            return;
        }
        try {
            inspection.startRecording(); // Prepare the resolver before unmuting through its event subscriber.
            viewModel.getEventManager().notify(new RecordingStartedEvent(inspection));
        } catch (IllegalStateException e) {
            Messages.showErrorDialog(event.getProject(), e.getMessage(), "InstanceTracer");
        }
    }
}
