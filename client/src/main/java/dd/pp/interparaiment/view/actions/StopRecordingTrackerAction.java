package dd.pp.interparaiment.view.actions;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;

import dd.pp.interparaiment.InspectorViewModel;

public final class StopRecordingTrackerAction extends AnAction {
    private final InspectorViewModel viewModel;
    private final MessActionsContext state;

    public StopRecordingTrackerAction(MessActionsContext state, InspectorViewModel viewModel) {
        super("Stop Recording", "Finish accepted data and keep the target JVM running", AllIcons.Actions.Suspend);
        this.state = state;
        this.viewModel = viewModel;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(viewModel.isInspectionActive() && viewModel.isRecording() && !state.isBusy());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (!state.beginOperation()) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (viewModel.isInspectionActive()) {
                    viewModel.stopRecording();
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (IllegalStateException ended) {
                if (viewModel.isInspectionActive()) {
                    viewModel.getEventManager().notify(new dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest(ended.getMessage()));
                }
            } finally {
                state.finishOperation();
                state.getUpdater().update();
            }
        });
    }
}
