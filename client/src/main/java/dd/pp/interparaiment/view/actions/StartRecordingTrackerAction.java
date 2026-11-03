package dd.pp.interparaiment.view.actions;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;

import dd.pp.interparaiment.InspectorViewModel;

public final class StartRecordingTrackerAction extends AnAction {
    private final InspectorViewModel viewModel;
    private final MessActionsContext state;

    public StartRecordingTrackerAction(MessActionsContext state, InspectorViewModel viewModel) {
        super("Start Recording", "Record data from this inspection session", AllIcons.Debugger.ThreadRunning);
        this.state = state;
        this.viewModel = viewModel;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(viewModel.isInspectionActive() && !viewModel.isRecording() && !state.isBusy());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (!state.beginOperation()) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                viewModel.startRecording();
            } catch (IllegalStateException failure) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Messages.showErrorDialog(e.getProject(), failure.getMessage(), "Cannot Start Recording"));
            } finally {
                state.finishOperation();
                state.getUpdater().update();
            }
        });
    }
}
