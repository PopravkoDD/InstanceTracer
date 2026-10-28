package dd.pp.interparaiment.view.actions;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import dd.pp.interparaiment.view.dialogs.TrackerConfigurationDialog;
import dd.pp.interparaiment.view.service.TracingService;
import dd.pp.interparaiment.view.service.TrackerSettingsService;

public class ConfigureTrackerAction extends AnAction {

    private final MessActionsContext state;

    public ConfigureTrackerAction(final MessActionsContext state) {
        super("Configure Tracking", "Opens InstanceTracer configuration", AllIcons.General.Settings);
        this.state = state;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        if (project != null) {
            new TrackerConfigurationDialog(project, project.getService(TracingService.class).getViewModel(),
                    project.getService(TrackerSettingsService.class)).show();
        }
    }
}
