package dd.pp.interparaiment.view.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import dd.pp.interparaiment.InspectorViewModel;
import dd.pp.interparaiment.event.requests.RecordingStoppedEvent;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;
import dd.pp.interparaiment.p2p.DataResolvingWorker.RecordingState;
import org.jetbrains.annotations.NotNull;

public final class StopRecordingTrackerAction extends AnAction {
    private final InspectorViewModel viewModel;

    public StopRecordingTrackerAction(InspectorViewModel viewModel) {
        super("Stop Recording", "Drain and pause recording without stopping the target", AllIcons.Actions.Suspend);
        this.viewModel = viewModel;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        var inspection = viewModel.getActiveInspection();
        event.getPresentation().setEnabled(inspection != null && inspection.isConnected()
                && inspection.getRecordingState() == RecordingState.RECORDING);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        var inspection = viewModel.getActiveInspection();
        if (inspection == null) {
            return;
        }
        var drained = inspection.stopRecording(); // Closes admission before publishing the mute event.
        viewModel.getEventManager().notify(new RecordingStoppedEvent(inspection));
        // No wait/join on the IDE event thread. Completion includes the last active message.
        drained.whenComplete((ignored, error) -> {
            if (error != null) {
                viewModel.getEventManager().notify(new ShowMessageInConsoleRequest("Cannot drain recording: " + error.getMessage()));
            }
        });
    }
}
