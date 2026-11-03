package dd.pp.interparaiment.view.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunnableState;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager;

/** Applies only to freshly created execution state, never the persisted VM settings. */
public final class MessInspectionExecutionOptions {
    private MessInspectionExecutionOptions() {}

    public static void apply(ExecutionEnvironment environment, String option) throws ExecutionException {
        synchronized (environment) {
            // IntelliJ may reuse an environment on rerun. State and options are fresh for each execution.
            clear(environment);
            environment.putUserData(MessInspectionProgramRunner.AGENT_OPTION, option);
            if (((MessInspectionConfiguration) environment.getRunProfile()).getPeer() instanceof GradleRunConfiguration gradle) {
                var state = environment.getState();
                if (!(state instanceof ExternalSystemRunnableState externalState)) {
                    throw new ExecutionException("The Gradle runner did not provide an external-system execution state");
                }
                String existing = externalState.getUserData(GradleTaskManager.INIT_SCRIPT_KEY);
                String inspection = GradleInspectionInjection.script(gradle, option);
                externalState.putUserData(GradleTaskManager.INIT_SCRIPT_KEY,
                        existing == null || existing.isBlank() ? inspection : existing + "\n" + inspection);
            }
        }
    }

    public static void clear(ExecutionEnvironment environment) {
        synchronized (environment) {
            environment.putUserData(MessInspectionProgramRunner.EXECUTION_STATE, null);
            environment.putUserData(MessInspectionProgramRunner.AGENT_OPTION, null);
        }
    }
}
