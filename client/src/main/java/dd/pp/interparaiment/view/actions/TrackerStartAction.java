package dd.pp.interparaiment.view.actions;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.execution.DefaultExecutionTarget;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionTargetManager;
import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.impl.RunDialog;
import com.intellij.execution.jar.JarApplicationConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.target.TargetEnvironmentConfigurations;
import com.intellij.execution.target.TargetEnvironmentAwareRunProfile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import dd.pp.interparaiment.InspectorConfiguration;
import dd.pp.interparaiment.InspectorViewModel;
import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.view.service.ClientConfiguration;
import dd.pp.interparaiment.view.service.IJavaAgentResolver;
import dd.pp.interparaiment.view.service.TrackerExecution;
import dd.pp.interparaiment.view.service.TrackerSettingsService;
import dd.pp.interparaiment.view.service.TracingService;

public class TrackerStartAction extends AnAction {
    private final EventManager eventManager;
    private final MessActionsContext state;

    public TrackerStartAction(final MessActionsContext state, final EventManager eventManager) {
        super("Start Tracking", "Starts connection with tracking agent", AllIcons.Debugger.ThreadRunning);
        this.state = state;
        this.eventManager = eventManager;
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(e.getProject() != null && !state.isRunning() && !state.isStarting());
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        if (project == null || state.isRunning() || state.isStarting()) {
            return;
        }
        try {
            RunnerAndConfigurationSettings selected = RunManager.getInstance(project).getSelectedConfiguration();
            validateSelection(project, selected);
            ClientConfiguration client = project.getService(TrackerSettingsService.class).getClientConfiguration();
            Executor executor = client.debugMode() ? DefaultDebugExecutor.getDebugExecutorInstance()
                    : DefaultRunExecutor.getRunExecutorInstance();
            // Honor edit-before-run before creating or injecting the copy.
            if (selected.isEditBeforeRun()
                    && !RunDialog.editConfiguration(project, selected, "Edit Run Configuration", executor)) {
                return;
            }
            validateSelection(project, selected);
            InspectorViewModel viewModel = project.getService(TracingService.class).getViewModel();
            final InspectorConfiguration snapshot;
            final String agentArgs;
            synchronized (viewModel) {
                snapshot = viewModel.getConfiguration();
                agentArgs = viewModel.getAgentArgs();
            }
            IJavaAgentResolver resolver = project.getService(IJavaAgentResolver.class);
            // The platform implementation clones the profile and both kinds of runner-specific settings.
            RunnerAndConfigurationSettings copy = ((RunnerAndConfigurationSettingsImpl) selected).clone();
            ExecutionEnvironment environment = ExecutionEnvironmentBuilder.create(executor, copy)
                    .activeTarget().dataContext(anActionEvent.getDataContext()).build();
            state.setStarting(true);
            new TrackerExecution(project, viewModel, state, environment, eventManager)
                    .prepareAndLaunch(snapshot, agentArgs, client, resolver);
        } catch (ExecutionException | IllegalArgumentException | IllegalStateException e) {
            state.setStarting(false);
            Messages.showErrorDialog(project, e.getMessage(), "InstanceTracer");
        }
    }

    private static void validateSelection(Project project, RunnerAndConfigurationSettings selected) {
        if (selected == null) {
            throw new IllegalArgumentException("Select a Java Application or JAR Application configuration in the Run/Debug toolbar");
        }
        var configuration = selected.getConfiguration();
        if ((configuration.getClass() != ApplicationConfiguration.class
                && configuration.getClass() != JarApplicationConfiguration.class)
                || !(selected instanceof RunnerAndConfigurationSettingsImpl)) {
            throw new IllegalArgumentException("InstanceTracer supports local Java Application and JAR Application configurations. "
                    + "The selected configuration type is " + selected.getType().getDisplayName());
        }
        if (!DefaultExecutionTarget.INSTANCE.equals(ExecutionTargetManager.getActiveTarget(project))
                || TargetEnvironmentConfigurations.getEffectiveTargetName(configuration, project) != null
                || ((TargetEnvironmentAwareRunProfile) configuration).needPrepareTarget()) {
            throw new IllegalArgumentException("Development agent artifacts support local JVM execution only; select a local target and JDK");
        }
    }
}
