package dd.pp.interparaiment.view.run;

import com.intellij.execution.DefaultExecutionTarget;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.configurations.WithoutOwnBeforeRunSteps;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import dd.pp.interparaiment.InspectorConfiguration;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A persisted reference to an existing profile, plus the inspection endpoint. */
public final class MessInspectionConfiguration extends RunConfigurationBase<RunConfigurationOptions>
        implements WithoutOwnBeforeRunSteps {
    private String configurationId = "";
    private String configurationName = "";
    private String host = InspectorConfiguration.DEFAULT_HOST;
    private int port = InspectorConfiguration.DEFAULT_PORT;

    MessInspectionConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    public String getConfigurationId() { return configurationId; }
    public String getConfigurationName() { return configurationName; }

    public void setUnderlyingConfiguration(@NotNull RunnerAndConfigurationSettings settings) {
        configurationId = settings.getUniqueID();
        configurationName = settings.getName();
    }

    public @Nullable RunnerAndConfigurationSettings findUnderlyingConfiguration() {
        if (configurationId.isEmpty()) {
            return null;
        }
        return RunManager.getInstance(getProject()).getAllSettings().stream()
                .filter(settings -> configurationId.equals(settings.getUniqueID()))
                .filter(settings -> !(settings.getConfiguration() instanceof MessInspectionConfiguration))
                .findFirst().orElse(null);
    }

    public @NotNull RunnerAndConfigurationSettings requireUnderlyingConfiguration() throws RuntimeConfigurationError {
        RunnerAndConfigurationSettings settings = findUnderlyingConfiguration();
        if (settings == null) {
            throw new RuntimeConfigurationError(configurationId.isEmpty()
                    ? "Select an existing Run Configuration in the MessInspection editor"
                    : "The selected Run Configuration '" + configurationName
                            + "' no longer exists. Select another configuration in the MessInspection editor.");
        }
        return settings;
    }

    public String getHost() { return host; }
    public void setHost(String value) { host = value; }
    public int getPort() { return port; }
    public void setPort(int value) { port = value; }

    @Override
    public boolean canRunOn(@NotNull ExecutionTarget target) {
        return DefaultExecutionTarget.INSTANCE.equals(target);
    }

    @Override
    public @NotNull SettingsEditor<MessInspectionConfiguration> getConfigurationEditor() {
        return new MessInspectionSettingsEditor(getProject());
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment)
            throws ExecutionException {
        // The runner delegates to the independently copied native profile, never this wrapper's state.
        throw new ExecutionException("MessInspection must be launched through its Run/Debug runner");
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        try {
            InspectorConfiguration.validateEndpoint(host, port);
        } catch (IllegalArgumentException e) {
            throw new RuntimeConfigurationError(e.getMessage());
        }
        MessInspectionExecutionOptions.validate(requireUnderlyingConfiguration().getConfiguration());
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        Element inspection = element.getChild("inspection");
        if (inspection == null) {
            return;
        }
        configurationId = inspection.getAttributeValue("configurationId", "");
        configurationName = inspection.getAttributeValue("configurationName", "");
        host = inspection.getAttributeValue("host", InspectorConfiguration.DEFAULT_HOST);
        try {
            port = Integer.parseInt(inspection.getAttributeValue("port", Integer.toString(InspectorConfiguration.DEFAULT_PORT)));
        } catch (NumberFormatException e) {
            throw new InvalidDataException("Invalid MessInspection port", e);
        }
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);
        element.addContent(new Element("inspection")
                .setAttribute("configurationId", configurationId)
                .setAttribute("configurationName", configurationName)
                .setAttribute("host", host).setAttribute("port", Integer.toString(port)));
    }
}
