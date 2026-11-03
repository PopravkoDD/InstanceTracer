package dd.pp.interparaiment.view.execution;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** The platform's configuration IDs contain names in this SDK, so preserve references on rename. */
public final class InspectionRunManagerListener implements RunManagerListener {
    private final Project project;

    public InspectionRunManagerListener(Project project) {
        this.project = project;
    }

    @Override
    public void runConfigurationChanged(@NotNull RunnerAndConfigurationSettings settings, @Nullable String existingId) {
        if (existingId == null || settings.getConfiguration() instanceof MessInspectionConfiguration) return;
        for (RunnerAndConfigurationSettings candidate : RunManager.getInstance(project).getAllSettings()) {
            if (candidate.getConfiguration() instanceof MessInspectionConfiguration inspection
                    && !inspection.isUnderlyingConfigurationRemoved() && existingId.equals(inspection.getUnderlyingId())) {
                inspection.selectUnderlyingConfiguration(settings);
            }
        }
    }

    @Override
    public void runConfigurationRemoved(@NotNull RunnerAndConfigurationSettings settings) {
        if (settings.getConfiguration() instanceof MessInspectionConfiguration) return;
        for (RunnerAndConfigurationSettings candidate : RunManager.getInstance(project).getAllSettings()) {
            if (candidate.getConfiguration() instanceof MessInspectionConfiguration inspection
                    && settings.getUniqueID().equals(inspection.getUnderlyingId())) {
                inspection.markUnderlyingConfigurationRemoved();
            }
        }
    }
}
