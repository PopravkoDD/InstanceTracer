package dd.pp.interparaiment.view.execution;

import com.intellij.execution.Executor;
import com.intellij.execution.JavaRunConfigurationBase;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.target.TargetEnvironmentAwareRunProfile;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MessInspectionConfiguration extends RunConfigurationBase<RunConfigurationOptions> {
    private String underlyingId = "";
    private String underlyingTypeId = "";
    private String underlyingName = "";
    private boolean underlyingRemoved;
    private List<String> traceTargets = List.of();
    private String host = "127.0.0.1";
    private int port = 35824;
    private boolean recordOnStart;

    public MessInspectionConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @Override
    public @NotNull SettingsEditor<? extends MessInspectionConfiguration> getConfigurationEditor() {
        return new MessInspectionSettingsEditor(getProject());
    }

    @Override
    public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        // MessInspectionProgramRunner delegates to the selected configuration's native runner and state.
        return null;
    }

    public @Nullable RunnerAndConfigurationSettings findUnderlyingConfiguration() {
        if (underlyingRemoved) return null;
        List<RunnerAndConfigurationSettings> settings = RunManager.getInstance(getProject()).getAllSettings();
        for (RunnerAndConfigurationSettings candidate : settings) {
            if (!(candidate.getConfiguration() instanceof MessInspectionConfiguration)
                    && !underlyingId.isBlank() && underlyingId.equals(candidate.getUniqueID())) {
                return candidate;
            }
        }
        for (RunnerAndConfigurationSettings candidate : settings) {
            if (!(candidate.getConfiguration() instanceof MessInspectionConfiguration)
                    && underlyingTypeId.equals(candidate.getType().getId())
                    && underlyingName.equals(candidate.getName())) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        RunnerAndConfigurationSettings selected = findUnderlyingConfiguration();
        if (selected == null) {
            throw new RuntimeConfigurationError(underlyingName.isBlank()
                    ? "Select an existing Run Configuration."
                    : "The selected Run Configuration '" + underlyingName + "' is missing or was deleted. Select another configuration.");
        }
        if (!(selected.getConfiguration() instanceof JavaRunConfigurationBase)) {
            throw new RuntimeConfigurationError("Run Configuration '" + selected.getName()
                    + "' does not support automatic Java agent injection. Select a local Java Run Configuration; Gradle and remote JVM configurations are unsupported.");
        }
        if (selected.getConfiguration() instanceof TargetEnvironmentAwareRunProfile target && target.needPrepareTarget()) {
            throw new RuntimeConfigurationError("InstanceTracer development artifacts support only a local JVM. Select a local Run Configuration.");
        }
        if (host.isBlank() || host.contains(",")) {
            throw new RuntimeConfigurationError("Enter a nonempty receiver host without commas.");
        }
        if (port < 1 || port > 65535) {
            throw new RuntimeConfigurationError("The receiver port must be between 1 and 65535.");
        }
        if (traceTargets.isEmpty()) {
            throw new RuntimeConfigurationError("Add at least one trace target before starting MessInspection.");
        }
    }

    public void selectUnderlyingConfiguration(@NotNull RunnerAndConfigurationSettings settings) {
        if (settings.getConfiguration() instanceof MessInspectionConfiguration) {
            throw new IllegalArgumentException("MessInspection configurations cannot wrap another MessInspection configuration.");
        }
        underlyingId = settings.getUniqueID();
        underlyingTypeId = settings.getType().getId();
        underlyingName = settings.getName();
        underlyingRemoved = false;
    }

    public void markUnderlyingConfigurationRemoved() { underlyingRemoved = true; }
    public boolean isUnderlyingConfigurationRemoved() { return underlyingRemoved; }

    public String getUnderlyingId() { return underlyingId; }
    public String getUnderlyingName() { return underlyingName; }
    public String getUnderlyingTypeId() { return underlyingTypeId; }
    public List<String> getTraceTargets() { return traceTargets; }
    public void setTraceTargets(List<String> targets) { traceTargets = List.copyOf(targets); }
    public String getHost() { return host; }
    public void setHost(String value) { host = value.trim(); }
    public int getPort() { return port; }
    public void setPort(int value) { port = value; }
    public boolean isRecordOnStart() { return recordOnStart; }
    public void setRecordOnStart(boolean value) { recordOnStart = value; }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        Element inspection = element.getChild("inspection");
        if (inspection == null) return;
        underlyingId = inspection.getAttributeValue("configurationId", "");
        underlyingTypeId = inspection.getAttributeValue("configurationType", "");
        underlyingName = inspection.getAttributeValue("configurationName", "");
        underlyingRemoved = Boolean.parseBoolean(inspection.getAttributeValue("configurationRemoved", "false"));
        host = inspection.getAttributeValue("host", "127.0.0.1");
        try {
            port = Integer.parseInt(inspection.getAttributeValue("port", "35824"));
        } catch (NumberFormatException exception) {
            throw new InvalidDataException("Invalid MessInspection receiver port.", exception);
        }
        recordOnStart = Boolean.parseBoolean(inspection.getAttributeValue("recordOnStart", "false"));
        traceTargets = inspection.getChildren("target").stream().map(target -> target.getAttributeValue("class", "")).toList();
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);
        element.removeChildren("inspection");
        Element inspection = new Element("inspection");
        inspection.setAttribute("configurationId", underlyingId);
        inspection.setAttribute("configurationType", underlyingTypeId);
        inspection.setAttribute("configurationName", underlyingName);
        inspection.setAttribute("configurationRemoved", Boolean.toString(underlyingRemoved));
        inspection.setAttribute("host", host);
        inspection.setAttribute("port", Integer.toString(port));
        inspection.setAttribute("recordOnStart", Boolean.toString(recordOnStart));
        traceTargets.forEach(target -> inspection.addContent(new Element("target").setAttribute("class", target)));
        element.addContent(inspection);
    }
}
