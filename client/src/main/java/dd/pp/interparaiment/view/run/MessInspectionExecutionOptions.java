package dd.pp.interparaiment.view.run;

import java.util.List;
import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaCommandLineState;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.target.TargetEnvironmentAwareRunProfile;
import com.intellij.execution.target.TargetEnvironmentConfigurations;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunnableState;
import com.intellij.openapi.util.Key;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager;

/** Changes only an unregistered execution copy. Native states keep their normal implementation. */
public final class MessInspectionExecutionOptions {
    private static final Key<Runnable> RESTORE_OPTIONS = Key.create("dd.pp.interparaiment.restoreInspectionOptions");

    private MessInspectionExecutionOptions() {}

    public static void validate(RunConfiguration configuration) throws RuntimeConfigurationError {
        if (TargetEnvironmentConfigurations.getEffectiveTargetName(configuration, configuration.getProject()) != null
                || (configuration instanceof TargetEnvironmentAwareRunProfile target && target.needPrepareTarget())) {
            throw new RuntimeConfigurationError("MessInspection currently supports local execution targets only");
        }
        if (configuration instanceof GradleRunConfiguration gradle) {
            GradleInspectionInjection.validate(gradle);
        } else if (!(configuration instanceof CommonJavaRunConfigurationParameters)) {
            throw new RuntimeConfigurationError("Run Configuration '" + configuration.getName() + "' ("
                    + configuration.getType().getDisplayName()
                    + ") does not expose target Java VM options required by MessInspection");
        }
    }

    public static void apply(ExecutionEnvironment environment, String option) throws ExecutionException {
        RunConfiguration configuration = (RunConfiguration) environment.getRunProfile();
        try {
            validate(configuration);
        } catch (RuntimeConfigurationError e) {
            throw new ExecutionException(e.getMessage(), e);
        }
        if (environment.getUserData(RESTORE_OPTIONS) != null) {
            throw new ExecutionException("Inspection options have already been applied to this execution");
        }
        if (configuration instanceof GradleRunConfiguration gradle) {
            String existing = gradle.getUserData(GradleTaskManager.INIT_SCRIPT_KEY);
            String inspection = GradleInspectionInjection.script(gradle, option);
            environment.putUserData(RESTORE_OPTIONS, () -> gradle.putUserData(GradleTaskManager.INIT_SCRIPT_KEY, existing));
            // ExternalSystemRunConfiguration copies its user data to EVERY newly requested native state.
            gradle.putUserData(GradleTaskManager.INIT_SCRIPT_KEY,
                    existing == null || existing.isBlank() ? inspection : existing + "\n" + inspection);
        } else {
            CommonJavaRunConfigurationParameters java = (CommonJavaRunConfigurationParameters) configuration;
            String existing = java.getVMParameters();
            environment.putUserData(RESTORE_OPTIONS, () -> java.setVMParameters(existing));
            if (!ParametersListUtil.parse(existing == null ? "" : existing).contains(option)) {
                String prefix = existing == null || existing.isBlank() ? "" : existing + " ";
                java.setVMParameters(prefix + ParametersListUtil.join(List.of(option)));
            }
        }
    }

    public static void validateState(ExecutionEnvironment environment) throws ExecutionException {
        RunProfileState state = environment.getState();
        if (environment.getRunProfile() instanceof GradleRunConfiguration) {
            if (state instanceof ExternalSystemRunnableState) {
                return;
            }
        } else if (state instanceof JavaCommandLineState) {
            return;
        }
        throw new ExecutionException("Run Configuration '" + environment.getRunProfile().getName()
                + "' does not provide a supported target JVM execution state; it cannot be inspected safely");
    }

    public static void clear(ExecutionEnvironment environment) {
        synchronized (environment) {
            Runnable restore = environment.getUserData(RESTORE_OPTIONS);
            if (restore != null) {
                restore.run();
                environment.putUserData(RESTORE_OPTIONS, null);
            }
        }
    }
}
