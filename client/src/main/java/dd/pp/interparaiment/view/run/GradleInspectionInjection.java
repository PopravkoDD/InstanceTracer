package dd.pp.interparaiment.view.run;

import java.util.List;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;

/** A task-scoped init script changes JavaExec's JVM, never Gradle daemon VM options. */
final class GradleInspectionInjection {
    private GradleInspectionInjection() {}

    static void validate(GradleRunConfiguration configuration) throws RuntimeConfigurationError {
        var tasks = configuration.getCommandLine().getTasks().getTasks();
        if (tasks.size() != 1 || !tasks.get(0).getName().matches(":?[\\p{L}\\p{N}_.-]+(?::[\\p{L}\\p{N}_.-]+)*")
                || configuration.isRunAsTest()) {
            throw new RuntimeConfigurationError("MessInspection requires one exact Gradle JavaExec task path, such as :run or :app:run");
        }
        String scriptParameters = configuration.getSettings().getScriptParameters();
        for (String option : ParametersListUtil.parse(scriptParameters == null ? "" : scriptParameters)) {
            if (List.of("--continuous", "-t", "--dry-run", "-m", "--configuration-cache").contains(option)) {
                throw new RuntimeConfigurationError("MessInspection does not support Gradle " + option);
            }
        }
        if (configuration.isDebugServerProcess()) {
            throw new RuntimeConfigurationError("Disable Gradle script debugging in the native Gradle settings; MessInspection targets the JavaExec application");
        }
    }

    static String script(GradleRunConfiguration configuration, String option) {
        String task = configuration.getCommandLine().getTasks().getTasks().get(0).getName();
        String exactPath = task.startsWith(":") ? task : ":" + task;
        return """
                if (gradle.startParameter.hasProperty('configurationCacheRequested') && gradle.startParameter.configurationCacheRequested) {
                    throw new GradleException('MessInspection requires --no-configuration-cache for JavaExec inspection')
                }
                gradle.taskGraph.whenReady { graph ->
                    def targetPath = %s
                    def target = graph.allTasks.find { it.path == targetPath }
                    if (!(target instanceof org.gradle.api.tasks.JavaExec)) {
                        throw new GradleException('MessInspection requires a scheduled JavaExec task at ' + targetPath)
                    }
                    target.doFirst {
                        def inspectionAgent = %s
                        if (!(target.jvmArgs ?: []).contains(inspectionAgent)) {
                            target.jvmArgs(inspectionAgent)
                        }
                    }
                }
                """.formatted(groovyLiteral(exactPath), groovyLiteral(option));
    }

    private static String groovyLiteral(String text) {
        return "'" + text.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }
}
