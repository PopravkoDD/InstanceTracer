package dd.pp.interparaiment.view.actions;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import dd.pp.interparaiment.InspectorConfiguration;
import dd.pp.interparaiment.InspectorViewModel;
import dd.pp.interparaiment.view.dialogs.TrackerConfigurationDialog;
import dd.pp.interparaiment.view.execution.MessInspectionConfiguration;
import dd.pp.interparaiment.view.service.TracingService;

/** Edits trace targets for the selected inspection's future executions. */
public final class ConfigureTrackerAction extends AnAction {
    private final Project project;
    private final MessInspectionConfiguration owner;

    public ConfigureTrackerAction(Project project, MessInspectionConfiguration owner) {
        super("Configure Trace Targets", "Edit trace targets for future inspection executions", AllIcons.General.Settings);
        this.project = project;
        this.owner = owner;
    }

    private MessInspectionConfiguration selectedConfiguration() {
        if (owner != null) {
            return owner;
        }
        RunnerAndConfigurationSettings selected = RunManager.getInstance(project).getSelectedConfiguration();
        return selected != null && selected.getConfiguration() instanceof MessInspectionConfiguration configuration
                ? configuration : null;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(selectedConfiguration() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        MessInspectionConfiguration configuration = selectedConfiguration();
        if (configuration == null) {
            return;
        }
        TrackerConfigurationDialog dialog = new TrackerConfigurationDialog(project, configuration.getTraceTargets());
        if (!dialog.showAndGet()) {
            return;
        }
        try {
            InspectorConfiguration before = project.getService(TracingService.class).resolveConfiguration(configuration);
            final InspectorConfiguration newConfiguration = new InspectorConfiguration(dialog.getTraceTargets(), before.host(), before.port(),
                    before.agentPath(), before.bridgePath(), before.recordOnStart());
            configuration.setTraceTargets(newConfiguration.traceTargets());
        } catch (IOException | IllegalArgumentException | IllegalStateException failure) {
            Messages.showErrorDialog(project, failure.getMessage(), "Cannot Configure InstanceTracer");
        }
    }
}
