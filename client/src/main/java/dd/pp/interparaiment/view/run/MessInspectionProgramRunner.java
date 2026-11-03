package dd.pp.interparaiment.view.run;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemTaskDebugRunner;
import com.intellij.openapi.util.Key;
import dd.pp.interparaiment.view.service.TrackerExecution;
import org.jetbrains.annotations.NotNull;

/** Native runner adapters keep MessInspection in the environment, including console reruns. */
public final class MessInspectionProgramRunner {
    static final Key<String> AGENT_OPTION = Key.create("dd.pp.interparaiment.inspectionAgent");
    static final Key<RunProfileState> EXECUTION_STATE = Key.create("dd.pp.interparaiment.inspectionState");

    private MessInspectionProgramRunner() {}

    private static boolean supports(String executorId, RunProfile profile, String expectedExecutor,
                                    MessInspectionConfiguration.Kind kind) {
        return expectedExecutor.equals(executorId) && profile instanceof MessInspectionConfiguration inspection
                && inspection.getKind() == kind;
    }

    public static final class ApplicationRun extends DefaultJavaProgramRunner {
        @Override public @NotNull String getRunnerId() { return "MessInspection.Application.Run"; }
        @Override public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
            return supports(executorId, profile, DefaultRunExecutor.EXECUTOR_ID, MessInspectionConfiguration.Kind.Application);
        }
        @Override public void execute(@NotNull ExecutionEnvironment environment) {
            new TrackerExecution(environment, () -> super.execute(environment)).prepareAndLaunch();
        }
    }

    public static final class ApplicationDebug extends GenericDebuggerRunner {
        @Override public @NotNull String getRunnerId() { return "MessInspection.Application.Debug"; }
        @Override public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
            return supports(executorId, profile, DefaultDebugExecutor.EXECUTOR_ID, MessInspectionConfiguration.Kind.Application);
        }
        @Override public void execute(@NotNull ExecutionEnvironment environment) {
            new TrackerExecution(environment, () -> super.execute(environment)).prepareAndLaunch();
        }
    }

    public static final class GradleRun implements ProgramRunner<RunnerSettings> {
        @Override public @NotNull String getRunnerId() { return "MessInspection.Gradle.Run"; }
        @Override public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
            return supports(executorId, profile, DefaultRunExecutor.EXECUTOR_ID, MessInspectionConfiguration.Kind.Gradle);
        }
        @Override public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
            var configuration = (MessInspectionConfiguration) environment.getRunProfile();
            ProgramRunner<?> nativeRunner = ProgramRunner.getRunner(DefaultRunExecutor.EXECUTOR_ID, configuration.getPeer());
            if (nativeRunner == null) {
                throw new ExecutionException("The native Gradle runner is unavailable");
            }
            // The platform's Gradle Run runner is final; delegate its implementation with our unchanged environment.
            new TrackerExecution(environment, () -> nativeRunner.execute(environment)).prepareAndLaunch();
        }
    }

    public static final class GradleDebug extends ExternalSystemTaskDebugRunner {
        @Override public @NotNull String getRunnerId() { return "MessInspection.Gradle.Debug"; }
        @Override public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
            return supports(executorId, profile, DefaultDebugExecutor.EXECUTOR_ID, MessInspectionConfiguration.Kind.Gradle);
        }
        @Override public void execute(@NotNull ExecutionEnvironment environment) {
            new TrackerExecution(environment, () -> super.execute(environment)).prepareAndLaunch();
        }
    }
}
