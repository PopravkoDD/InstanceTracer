package dd.pp.interparaiment.view.run;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Keep persisted references in step with RunManager's name-based IDs on rename. */
public final class MessInspectionRunManagerListener implements RunManagerListener {
    private final Project project;

    public MessInspectionRunManagerListener(Project project) {
        this.project = project;
    }

    @Override
    public void runConfigurationChanged(@NotNull RunnerAndConfigurationSettings settings, @Nullable String oldId) {
        if (settings.getConfiguration() instanceof MessInspectionConfiguration || oldId == null) {
            return;
        }
        RunManager manager = RunManager.getInstance(project);
        for (RunnerAndConfigurationSettings candidate : manager.getAllSettings()) {
            if (candidate.getConfiguration() instanceof MessInspectionConfiguration inspection
                    && oldId.equals(inspection.getConfigurationId())
                    && (!settings.getUniqueID().equals(inspection.getConfigurationId())
                        || !settings.getName().equals(inspection.getConfigurationName()))) {
                inspection.setUnderlyingConfiguration(settings);
                // Notify the standard settings/scheme machinery; nested wrapper events are ignored above.
                manager.addConfiguration(candidate);
            }
        }
    }
}
