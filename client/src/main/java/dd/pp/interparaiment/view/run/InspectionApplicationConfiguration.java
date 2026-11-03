package dd.pp.interparaiment.view.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/** Native Application state with one execution-local parameter hook; no VM settings mutation. */
final class InspectionApplicationConfiguration extends ApplicationConfiguration {
    InspectionApplicationConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(name, project, factory);
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        var state = new JavaApplicationCommandLineState<InspectionApplicationConfiguration>(this, environment) {
            @Override
            protected JavaParameters createJavaParameters() throws ExecutionException {
                JavaParameters parameters = super.createJavaParameters();
                String option = environment.getUserData(MessInspectionProgramRunner.AGENT_OPTION);
                if (option == null) {
                    throw new ExecutionException("Launch this Application through its owning MessInspection configuration");
                }
                if (!parameters.getVMParametersList().getList().contains(option)) {
                    parameters.getVMParametersList().add(option);
                }
                return parameters;
            }
        };
        state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance()
                .createBuilder(getProject(), getConfigurationModule().getSearchScope()));
        return state;
    }
}
