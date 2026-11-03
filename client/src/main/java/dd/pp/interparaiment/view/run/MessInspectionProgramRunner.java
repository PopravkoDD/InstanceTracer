package dd.pp.interparaiment.view.run;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.DefaultExecutionTarget;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.util.Key;
import dd.pp.interparaiment.view.service.TrackerExecution;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager;

/** Delegates execution to the selected profile's native runner, with a fresh copy on every launch/rerun. */
public final class MessInspectionProgramRunner implements ProgramRunner<RunnerSettings> {
    private static final Key<MessInspectionConfiguration> ORIGIN = Key.create("dd.pp.interparaiment.inspectionOrigin");

    @Override
    public @NotNull String getRunnerId() { return "MessInspection"; }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return (DefaultRunExecutor.EXECUTOR_ID.equals(executorId) || DefaultDebugExecutor.EXECUTOR_ID.equals(executorId))
                && getInspection(profile) != null;
    }

    private static MessInspectionConfiguration getInspection(RunProfile profile) {
        if (profile instanceof MessInspectionConfiguration inspection) {
            return inspection;
        }
        return profile instanceof RunConfigurationBase<?> copy ? copy.getUserData(ORIGIN) : null;
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment requested) throws ExecutionException {
        MessInspectionConfiguration inspection = getInspection(requested.getRunProfile());
        if (inspection == null) {
            throw new ExecutionException("The MessInspection configuration is unavailable");
        }
        final RunnerAndConfigurationSettings selected;
        try {
            selected = inspection.requireUnderlyingConfiguration();
            MessInspectionExecutionOptions.validate(selected.getConfiguration());
        } catch (RuntimeConfigurationException e) {
            throw new ExecutionException(e.getMessage(), e);
        }
        if (!DefaultExecutionTarget.INSTANCE.equals(requested.getExecutionTarget())) {
            throw new ExecutionException("MessInspection currently supports local execution targets only");
        }
        RunnerAndConfigurationSettings copy = copySettings(selected);
        if (!(copy.getConfiguration() instanceof RunConfigurationBase<?> profile)) {
            throw new ExecutionException("The selected configuration cannot provide an independent JVM execution profile");
        }
        // Resolve the native runner BEFORE marking the copy as an inspection rerun.
        ProgramRunner<?> nativeRunner = ProgramRunner.getRunner(requested.getExecutor().getId(), profile);
        if (nativeRunner == null || nativeRunner == this) {
            throw new ExecutionException("No native " + requested.getExecutor().getActionName()
                    + " runner is available for '" + selected.getName() + "'");
        }
        profile.putUserData(ORIGIN, inspection);
        ExecutionEnvironment environment = createEnvironment(requested, copy, nativeRunner);
        new TrackerExecution(inspection, environment, () -> nativeRunner.execute(environment)).prepareAndLaunch();
    }

    private ExecutionEnvironment createEnvironment(ExecutionEnvironment requested, RunnerAndConfigurationSettings copy,
                                                   ProgramRunner<?> nativeRunner) throws ExecutionException {
        ExecutionEnvironmentBuilder builder = new ExecutionEnvironmentBuilder(requested.getProject(), requested.getExecutor())
                .runnerAndSettings(nativeRunner, copy)
                // Keep only dispatch/rerun ownership here; native runner settings and profile belong to the copy.
                .runner(this)
                .target(requested.getExecutionTarget())
                .contentToReuse(requested.getContentToReuse())
                .dataContext(requested.getDataContext());
        if (requested.getModulePath() != null) {
            builder.modulePath(requested.getModulePath());
        }
        ExecutionEnvironment environment = builder.build(requested.getCallback());
        if (environment.getRunProfile() != copy.getConfiguration() || environment.getRunner() != this) {
            throw new ExecutionException("The selected Run Configuration delegates to another execution profile. "
                    + "MessInspection requires direct target JVM options or a Gradle JavaExec configuration.");
        }
        if (requested.isHeadless()) {
            environment.setHeadless();
        }
        if (requested.getExecutionId() == 0) {
            environment.assignNewExecutionId();
        } else {
            environment.setExecutionId(requested.getExecutionId());
        }
        return environment;
    }

    private static RunnerAndConfigurationSettings copySettings(RunnerAndConfigurationSettings selected)
            throws ExecutionException {
        if (!(selected instanceof RunnerAndConfigurationSettingsImpl platformSettings)) {
            throw new ExecutionException("This Run Configuration does not support the platform's independent settings copy");
        }
        RunnerAndConfigurationSettings copy = platformSettings.clone();
        // An automatic native editor could register/persist the injected copy on console rerun.
        copy.setEditBeforeRun(false);
        if (copy.getConfiguration() == selected.getConfiguration()) {
            throw new ExecutionException("The selected Run Configuration did not create an independent execution copy");
        }
        copy.getConfiguration().setBeforeRunTasks(selected.getConfiguration().getBeforeRunTasks().stream()
                .<BeforeRunTask<?>>map(task -> task.clone()).toList());
        if (selected.getConfiguration() instanceof GradleRunConfiguration original
                && copy.getConfiguration() instanceof GradleRunConfiguration gradle) {
            // Gradle's XML-based clone omits runtime init-script user data.
            gradle.putUserData(GradleTaskManager.INIT_SCRIPT_KEY, original.getUserData(GradleTaskManager.INIT_SCRIPT_KEY));
        }
        // Do not add this copy to RunManager or mark it as a user-visible temporary configuration.
        return copy;
    }
}
