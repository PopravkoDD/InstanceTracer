package dd.pp.interparaiment.view.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.messages.MessageBusConnection;
import dd.pp.interparaiment.InspectorConfiguration;
import dd.pp.interparaiment.InspectorViewModel;
import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.IListener;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;
import dd.pp.interparaiment.event.requests.StartInspectionRequest;
import dd.pp.interparaiment.event.requests.StopInspectionRequest;
import dd.pp.interparaiment.view.actions.MessActionsContext;
import org.jetbrains.annotations.NotNull;

/** Owns one prepared receiver and the corresponding IDE execution lifecycle. */
public final class TrackerExecution implements Disposable {
    private final Project project;
    private final InspectorViewModel viewModel;
    private final MessActionsContext state;
    private final ExecutionEnvironment environment;
    private final EventManager eventManager;
    private final String originalVmOptions;
    private final AtomicBoolean optionInjected = new AtomicBoolean();
    private volatile long executionId;
    private final AtomicBoolean finished = new AtomicBoolean();
    private final MessageBusConnection connection;
    private volatile InspectorViewModel.PreparedInspection inspection;
    private final IListener<StopInspectionRequest> stopListener = request -> finish(false);

    public TrackerExecution(Project project, InspectorViewModel viewModel, MessActionsContext state,
                            ExecutionEnvironment environment, EventManager eventManager) {
        this.project = project;
        this.viewModel = viewModel;
        this.state = state;
        this.environment = environment;
        this.eventManager = eventManager;
        originalVmOptions = parameters().getVMParameters();
        Disposer.register(project.getService(TracingService.class), this);
        connection = project.getMessageBus().connect(this);
        connection.subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            @Override
            public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env,
                                       @NotNull ProcessHandler handler) {
                if (isOurExecution(env) && !finished.get()) {
                    restoreVmOptions();
                    state.setStarting(false);
                    state.setRunning(true);
                    eventManager.notify(new StartInspectionRequest(inspection));
                }
            }

            @Override
            public void processNotStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
                if (isOurExecution(env)) {
                    fail("The IDE did not start the target process. Check the Run/Debug console.");
                }
            }

            @Override
            public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env,
                                          @NotNull ProcessHandler handler, int exitCode) {
                if (isOurExecution(env)) {
                    // Let the reader consume buffered frames through EOF before it closes itself.
                    if (inspection != null) {
                        inspection.targetTerminated();
                    }
                    finish(false);
                }
            }
        });
        eventManager.subscribe(StopInspectionRequest.class, stopListener);
    }

    private boolean isOurExecution(ExecutionEnvironment other) {
        return executionId != 0 && other.getExecutionId() == executionId;
    }

    /** Resolve filesystem artifacts and bind the listener without blocking the IDE thread. */
    public void prepareAndLaunch(InspectorConfiguration snapshot, String agentArgs,
                                 ClientConfiguration client, IJavaAgentResolver resolver) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                IJavaAgentResolver.AgentArtifacts artifacts = resolver.resolve(client);
                String option = javaAgentOption(artifacts, agentArgs);
                if (finished.get()) {
                    return;
                }
                inspection = viewModel.prepareInspection(snapshot);
                if (finished.get()) {
                    inspection.close();
                    return;
                }
                ApplicationManager.getApplication().invokeLater(() -> launch(option), ModalityState.nonModal());
            } catch (IOException | IllegalArgumentException e) {
                fail(e.getMessage());
            }
        });
    }

    private static String javaAgentOption(IJavaAgentResolver.AgentArtifacts artifacts, String agentArgs) {
        Path agent = artifacts.agent();
        String hooks = artifacts.hooks().toString();
        if (agent.toString().contains("=") || agent.toString().chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("The agent JAR path contains characters unsupported by -javaagent: " + agent);
        }
        if (hooks.contains(",") || hooks.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("The hooks JAR path contains characters unsupported by the agent arguments: " + hooks);
        }
        return "-javaagent:" + agent + "=BridgePath=" + hooks + "," + agentArgs;
    }

    private void launch(String option) {
        if (project.isDisposed() || finished.get()) {
            finish(true);
            return;
        }
        try {
            // This profile is an unregistered deep copy, never the RunManager's stored configuration.
            injectVmOption(option);
            environment.getRunnerAndConfigurationSettings().checkSettings(environment.getExecutor());
            if (environment.getState() == null) {
                throw new com.intellij.execution.ExecutionException("The selected configuration did not provide an executable state");
            }
            environment.assignNewExecutionId();
            executionId = environment.getExecutionId();
            environment.getRunner().execute(environment);
        } catch (com.intellij.execution.ExecutionException
                 | com.intellij.execution.configurations.RuntimeConfigurationException e) {
            fail(e.getMessage());
        }
    }

    private CommonJavaRunConfigurationParameters parameters() {
        return (CommonJavaRunConfigurationParameters) environment.getRunProfile();
    }

    private void injectVmOption(String option) {
        var options = new ArrayList<>(ParametersListUtil.parse(originalVmOptions == null ? "" : originalVmOptions));
        options.add(option);
        parameters().setVMParameters(ParametersListUtil.join(options));
        optionInjected.set(true);
    }

    private void restoreVmOptions() {
        if (optionInjected.compareAndSet(true, false)) {
            parameters().setVMParameters(originalVmOptions);
        }
    }

    private void fail(String message) {
        String diagnostic = message == null ? "The IDE could not launch tracking" : message;
        if (finish(true)) {
            eventManager.notify(new ShowMessageInConsoleRequest("Tracking launch failed: " + diagnostic));
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!project.isDisposed()) {
                    Messages.showErrorDialog(project, diagnostic, "InstanceTracer");
                }
            }, ModalityState.nonModal());
        }
    }

    private boolean finish(boolean stopReader) {
        if (!closeResources(stopReader)) {
            return false;
        }
        Disposer.dispose(this);
        return true;
    }

    private boolean closeResources(boolean stopReader) {
        if (!finished.compareAndSet(false, true)) {
            return false;
        }
        state.setStarting(false);
        state.setRunning(false);
        restoreVmOptions();
        eventManager.unsubscribe(StopInspectionRequest.class, stopListener);
        if (stopReader && inspection != null) {
            inspection.close();
        }
        connection.disconnect();
        return true;
    }

    @Override
    public void dispose() {
        closeResources(true);
    }
}
