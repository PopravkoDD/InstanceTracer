package dd.pp.interparaiment.view.run;

import javax.swing.Icon;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class MessInspectionConfigurationType implements ConfigurationType {
    private final ConfigurationFactory factory = new ConfigurationFactory(this) {
        @Override
        public @NotNull String getId() {
            return "MessInspection";
        }

        @Override
        public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return new MessInspectionConfiguration(project, this, "MessInspection");
        }
    };

    @Override
    public @NotNull String getDisplayName() { return "MessInspection"; }

    @Override
    public @NotNull String getConfigurationTypeDescription() { return "Inspect a JVM using an existing Run Configuration"; }

    @Override
    public Icon getIcon() { return AllIcons.Debugger.ThreadRunning; }

    @Override
    public @NotNull String getId() { return "MessInspection"; }

    @Override
    public ConfigurationFactory @NotNull [] getConfigurationFactories() {
        return new ConfigurationFactory[]{factory};
    }
}
