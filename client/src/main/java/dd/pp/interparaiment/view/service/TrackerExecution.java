package dd.pp.interparaiment.view.service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ThrowableRunnable;
import dd.pp.interparaiment.InspectorConfiguration;
import dd.pp.interparaiment.InspectorViewModel;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;
import dd.pp.interparaiment.event.requests.StartInspectionRequest;
import dd.pp.interparaiment.view.run.MessInspectionConfiguration;
import dd.pp.interparaiment.view.run.MessInspectionExecutionOptions;
import org.jetbrains.annotations.NotNull;

/** Execution ownership only. Recording toggles never finish this lifecycle. */
public final class TrackerExecution implements Disposable {
    private static final Key<TrackerExecution> OWNER = Key.create("dd.pp.interparaiment.executionOwner");
    private final Project project;
    private final ExecutionEnvironment environment;
    private final MessInspectionConfiguration configuration;
    private final ThrowableRunnable<ExecutionException> nativeLaunch;
    private final MessageBusConnection connection;
    private final AtomicBoolean finished = new AtomicBoolean();
    private volatile InspectorViewModel.PreparedInspection inspection;
    private volatile InspectorViewModel viewModel;

    public TrackerExecution(MessInspectionConfiguration configuration, ExecutionEnvironment environment,
                            ThrowableRunnable<ExecutionException> nativeLaunch) {
        this.environment = environment;
        this.configuration = configuration;
        this.nativeLaunch = nativeLaunch;
        if (environment.getExecutionId() == 0) {
            environment.assignNewExecutionId();
        }
        project = environment.getProject();
        connection = project.getMessageBus().connect(this);
        Disposer.register(project.getService(TracingService.class), this);
        connection.subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            @Override
            public void processNotStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
                if (isOurExecution(env)) {
                    // The native runner reports execution errors and cancellations in the usual IDE UI.
                    finish();
                }
            }

            @Override
            public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env,
                                          @NotNull ProcessHandler handler, int exitCode) {
                if (isOurExecution(env)) {
                    finish();
                }
            }
        });
    }

    private boolean isOurExecution(ExecutionEnvironment other) {
        return other == environment;
    }

    public void prepareAndLaunch() {
        // Capture editable execution settings and the recording preference before asynchronous preparation.
        String host = configuration.getHost();
        int port = configuration.getPort();
        boolean recordOnStart = project.getService(TrackerSettingsService.class).getClientConfiguration().recordOnStart();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                TracingService tracing = project.getService(TracingService.class);
                InspectorConfiguration snapshot = tracing.resolveConfiguration(host, port);
                viewModel = tracing.getViewModel(snapshot);
                String option = viewModel.getAgentArgs(snapshot);
                if (finished.get()) {
                    return;
                }
                synchronized (environment) {
                    if (finished.get()) {
                        return;
                    }
                    environment.putUserData(OWNER, this);
                    MessInspectionExecutionOptions.apply(environment, option);
                }
                environment.getRunnerAndConfigurationSettings().checkSettings(environment.getExecutor());
                if (finished.get()) {
                    return;
                }
                inspection = viewModel.prepareInspection(snapshot, recordOnStart);
                if (finished.get()) {
                    inspection.close();
                    return;
                }
                synchronized (viewModel) {
                    // A launch snapshot must not overwrite targets edited while it was being prepared.
                    viewModel.updateOptions(snapshot.withTraceTargets(viewModel.getConfiguration().traceTargets()));
                }
                synchronized (environment) {
                    if (finished.get()) {
                        inspection.close();
                        return;
                    }
                }
                viewModel.getEventManager().notify(new StartInspectionRequest(inspection));
                ApplicationManager.getApplication().invokeLater(this::launch, ModalityState.nonModal());
            } catch (ProcessCanceledException e) {
                finish();
            } catch (IOException | ExecutionException | RuntimeConfigurationException | RuntimeException e) {
                fail(e.getMessage());
            }
        });
    }

    private void launch() {
        if (finished.get() || project.isDisposed()) {
            finish();
            return;
        }
        try {
            MessInspectionExecutionOptions.validateState(environment);
            nativeLaunch.run();
        } catch (ProcessCanceledException e) {
            finish();
        } catch (ExecutionException | RuntimeException e) {
            fail(e.getMessage());
        }
    }

    private void fail(String message) {
        if (finish()) {
            String diagnostic = message == null ? "MessInspection launch failed" : message;
            if (viewModel != null) {
                viewModel.getEventManager().notify(new ShowMessageInConsoleRequest(diagnostic));
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!project.isDisposed()) {
                    Messages.showErrorDialog(project, diagnostic, "MessInspection");
                }
            }, ModalityState.nonModal());
        }
    }

    private boolean finish() {
        if (!closeResources()) {
            return false;
        }
        Disposer.dispose(this);
        return true;
    }

    private boolean closeResources() {
        if (!finished.compareAndSet(false, true)) {
            return false;
        }
        if (inspection != null) {
            // Close reader/socket immediately, then asynchronously drain and terminate the resolver.
            inspection.close();
        }
        synchronized (environment) {
            if (environment.getUserData(OWNER) == this) {
                MessInspectionExecutionOptions.clear(environment);
                environment.putUserData(OWNER, null);
            }
        }
        connection.disconnect();
        return true;
    }

    @Override
    public void dispose() {
        closeResources();
    }
}
