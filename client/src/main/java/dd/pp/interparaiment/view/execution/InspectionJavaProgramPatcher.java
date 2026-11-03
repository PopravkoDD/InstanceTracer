package dd.pp.interparaiment.view.execution;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.JavaProgramPatcher;

/** Applies only to the unregistered execution copy owned by MessInspection. */
public final class InspectionJavaProgramPatcher extends JavaProgramPatcher {
    @Override
    public void patchJavaParameters(Executor executor, RunProfile profile, JavaParameters parameters) {
        if (profile instanceof RunConfigurationBase<?> configuration) {
            String option = configuration.getUserData(MessInspectionProgramRunner.AGENT_OPTION);
            if (option != null && !parameters.getVMParametersList().getList().contains(option)) {
                parameters.getVMParametersList().add(option);
            }
        }
    }
}
