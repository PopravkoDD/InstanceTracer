package dd.pp.interparaiment.view.run;

import java.util.ArrayList;
import java.util.List;
import com.intellij.execution.Executor;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManager;
import com.intellij.execution.DefaultExecutionTarget;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.diagnostic.logging.LogConsole;
import com.intellij.execution.target.TargetEnvironmentConfigurations;
import com.intellij.execution.target.TargetEnvironmentAwareRunProfile;
import com.intellij.execution.target.TargetEnvironmentConfiguration;
import com.intellij.execution.target.LanguageRuntimeType;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.module.Module;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.openapi.util.InvalidDataException;
import dd.pp.interparaiment.InspectorConfiguration;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;

/** Independently persisted wrapper; the embedded native profile is owned, never selected/copied at launch. */
public final class MessInspectionConfiguration extends RunConfigurationBase<RunConfigurationOptions>
        implements RunProfileWithCompileBeforeLaunchOption, TargetEnvironmentAwareRunProfile, SearchScopeProvidingRunProfile {
    public enum Kind { Application, Gradle }

    private Kind kind = Kind.Application;
    private RunConfigurationBase<?> peer;
    private String host = InspectorConfiguration.DEFAULT_HOST;
    private int port = InspectorConfiguration.DEFAULT_PORT;

    MessInspectionConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
        peer = createPeer(kind);
    }

    RunConfigurationBase<?> createPeer(Kind type) {
        if (type == Kind.Application) {
            ConfigurationFactory nativeFactory = ApplicationConfigurationType.getInstance().getConfigurationFactories()[0];
            var application = new InspectionApplicationConfiguration(getProject(), nativeFactory, getName());
            application.setBeforeRunTasks(RunManager.getInstance(getProject()).getConfigurationTemplate(nativeFactory)
                    .getConfiguration().getBeforeRunTasks().stream().<BeforeRunTask<?>>map(task -> task.clone()).toList());
            return application;
        }
        ConfigurationFactory nativeFactory = GradleExternalTaskConfigurationType.getInstance().getConfigurationFactories()[0];
        GradleRunConfiguration gradle = (GradleRunConfiguration) nativeFactory.createTemplateConfiguration(getProject());
        gradle.setName(getName());
        gradle.setDebugServerProcess(false);
        return gradle;
    }

    public @NotNull RunConfigurationBase<?> getPeer() { return peer; }

    @Override public @NotNull List<LogFileOptions> getLogFiles() { return peer.getLogFiles(); }
    @Override public @NotNull List<PredefinedLogFile> getPredefinedLogFiles() { return peer.getPredefinedLogFiles(); }
    @Override public @NotNull ArrayList<LogFileOptions> getAllLogFiles() { return peer.getAllLogFiles(); }
    @Override public LogFileOptions getOptionsForPredefinedLogFile(PredefinedLogFile file) { return peer.getOptionsForPredefinedLogFile(file); }
    @Override public boolean isSaveOutputToFile() { return peer.isSaveOutputToFile(); }
    @Override public void setSaveOutputToFile(boolean value) { peer.setSaveOutputToFile(value); }
    @Override public String getOutputFilePath() { return peer.getOutputFilePath(); }
    @Override public void setFileOutputPath(String value) { peer.setFileOutputPath(value); }
    @Override public boolean isShowConsoleOnStdOut() { return peer.isShowConsoleOnStdOut(); }
    @Override public void setShowConsoleOnStdOut(boolean value) { peer.setShowConsoleOnStdOut(value); }
    @Override public boolean isShowConsoleOnStdErr() { return peer.isShowConsoleOnStdErr(); }
    @Override public void setShowConsoleOnStdErr(boolean value) { peer.setShowConsoleOnStdErr(value); }
    @Override public boolean collectOutputFromProcessHandler() { return peer.collectOutputFromProcessHandler(); }
    @Override public void createAdditionalTabComponents(AdditionalTabComponentManager manager, ProcessHandler process) {
        peer.createAdditionalTabComponents(manager, process);
    }
    @Override public void customizeLogConsole(LogConsole console) { peer.customizeLogConsole(console); }

    @Override
    public @NotNull List<BeforeRunTask<?>> getBeforeRunTasks() {
        return peer == null ? super.getBeforeRunTasks() : peer.getBeforeRunTasks();
    }

    @Override
    public void setBeforeRunTasks(@NotNull List<BeforeRunTask<?>> tasks) {
        if (peer == null) {
            super.setBeforeRunTasks(tasks);
        } else {
            peer.setBeforeRunTasks(tasks);
        }
    }

    @Override
    public Module @NotNull [] getModules() {
        return peer instanceof RunProfileWithCompileBeforeLaunchOption nativeProfile
                ? nativeProfile.getModules() : Module.EMPTY_ARRAY;
    }

    @Override
    public boolean isBuildBeforeLaunchAddedByDefault() {
        return kind == Kind.Application;
    }

    @Override
    public boolean isExcludeCompileBeforeLaunchOption() { return kind == Kind.Gradle; }

    @Override
    public GlobalSearchScope getSearchScope() {
        return peer instanceof SearchScopeProvidingRunProfile nativeProfile ? nativeProfile.getSearchScope()
                : GlobalSearchScope.projectScope(getProject());
    }

    @Override
    public boolean canRunOn(@NotNull TargetEnvironmentConfiguration target) { return false; }

    @Override
    public boolean canRunOn(@NotNull ExecutionTarget target) { return DefaultExecutionTarget.INSTANCE.equals(target); }

    @Override
    public LanguageRuntimeType<?> getDefaultLanguageRuntimeType() { return null; }

    @Override
    public String getDefaultTargetName() {
        return ((TargetEnvironmentAwareRunProfile) peer).getDefaultTargetName();
    }

    @Override
    public void setDefaultTargetName(String name) {
        ((TargetEnvironmentAwareRunProfile) peer).setDefaultTargetName(name);
    }

    @Override
    public boolean needPrepareTarget() {
        return ((TargetEnvironmentAwareRunProfile) peer).needPrepareTarget();
    }

    public Kind getKind() { return kind; }

    public void setKind(Kind value) {
        if (kind != value) {
            kind = value;
            peer = createPeer(value);
        }
    }

    public String getHost() { return host; }
    public void setHost(String value) { host = value; }
    public int getPort() { return port; }
    public void setPort(int value) { port = value; }

    @Override
    public @NotNull SettingsEditor<MessInspectionConfiguration> getConfigurationEditor() {
        return new MessInspectionSettingsEditor();
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment)
            throws com.intellij.execution.ExecutionException {
        synchronized (environment) {
            RunProfileState state = environment.getUserData(MessInspectionProgramRunner.EXECUTION_STATE);
            if (state == null) {
                state = peer.getState(executor, environment);
                environment.putUserData(MessInspectionProgramRunner.EXECUTION_STATE, state);
            }
            return state;
        }
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        try {
            InspectorConfiguration.validateEndpoint(host, port);
        } catch (IllegalArgumentException e) {
            throw new RuntimeConfigurationError(e.getMessage());
        }
        if (TargetEnvironmentConfigurations.getEffectiveTargetName(peer, getProject()) != null
                || (peer instanceof TargetEnvironmentAwareRunProfile target && target.needPrepareTarget())) {
            throw new RuntimeConfigurationError("MessInspection currently supports local execution targets only");
        }
        peer.checkConfiguration();
        if (peer instanceof GradleRunConfiguration gradle) {
            GradleInspectionInjection.validate(gradle);
        }
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        Element inspection = element.getChild("inspection");
        if (inspection == null) {
            return;
        }
        try {
            setKind(Kind.valueOf(inspection.getAttributeValue("kind", "Application")));
            host = inspection.getAttributeValue("host", InspectorConfiguration.DEFAULT_HOST);
            port = Integer.parseInt(inspection.getAttributeValue("port", Integer.toString(InspectorConfiguration.DEFAULT_PORT)));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Invalid MessInspection settings", e);
        }
        Element nativeSettings = inspection.getChild("underlying");
        if (nativeSettings != null) {
            peer.readExternal(nativeSettings);
            peer.setName(getName());
        }
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);
        Element inspection = new Element("inspection").setAttribute("kind", kind.name())
                .setAttribute("host", host).setAttribute("port", Integer.toString(port));
        Element nativeSettings = new Element("underlying");
        peer.writeExternal(nativeSettings);
        inspection.addContent(nativeSettings);
        element.addContent(inspection);
    }

    /** IntelliJ's configuration editor/templates require independent snapshots, never used by execution. */
    @Override
    public MessInspectionConfiguration clone() {
        MessInspectionConfiguration copy = (MessInspectionConfiguration) super.clone();
        copy.peer = (RunConfigurationBase<?>) peer.clone();
        copy.peer.setBeforeRunTasks(peer.getBeforeRunTasks().stream().<BeforeRunTask<?>>map(task -> task.clone()).toList());
        return copy;
    }
}
