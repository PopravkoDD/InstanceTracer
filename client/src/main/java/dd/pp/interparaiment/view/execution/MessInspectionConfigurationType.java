package dd.pp.interparaiment.view.execution;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class MessInspectionConfigurationType extends ConfigurationTypeBase {
    public static final String ID = "MessInspection";

    public MessInspectionConfigurationType() {
        super(ID, "MessInspection", "Run an existing configuration with InstanceTracer", AllIcons.Debugger.ThreadRunning);
        addFactory(new ConfigurationFactory(this) {
            @Override
            public @NotNull String getId() {
                return ID;
            }

            @Override
            public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
                return new MessInspectionConfiguration(project, this, "MessInspection");
            }
        });
    }
}
