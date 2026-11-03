package dd.pp.interparaiment.view.execution;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.JavaRunConfigurationBase;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.runners.JvmPatchableProgramRunner;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.Key;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.messages.MessageBusConnection;
import dd.pp.interparaiment.PreparedInspection;
import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.requests.InspectionClosedEvent;
import dd.pp.interparaiment.view.service.TracingService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class MessInspectionProgramRunner implements ProgramRunner<RunnerSettings> {
    static final Key<PreparedInspection> INSPECTION = Key.create("InstanceTracer.inspection");
    static final Key<String> AGENT_OPTION = Key.create("InstanceTracer.agentOption");

    @Override
    public @NotNull String getRunnerId() {
        return "MessInspectionRunner";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return profile instanceof MessInspectionConfiguration
                && (DefaultRunExecutor.EXECUTOR_ID.equals(executorId) || DefaultDebugExecutor.EXECUTOR_ID.equals(executorId));
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        MessInspectionConfiguration configuration = (MessInspectionConfiguration)environment.getRunProfile();
        try {
            configuration.checkConfiguration();
        } catch (com.intellij.execution.configurations.RuntimeConfigurationException exception) {
            notifyNotStarted(environment);
            throw new ExecutionException(exception.getMessage(), exception);
        }
        RunnerAndConfigurationSettings selected = configuration.findUnderlyingConfiguration();
        if (!(selected instanceof RunnerAndConfigurationSettingsImpl original)) {
            notifyNotStarted(environment);
            throw new ExecutionException("The selected configuration cannot be copied with its native runner settings.");
        }
        // Neither the copied settings nor the copied configuration are registered with RunManager.
        RunnerAndConfigurationSettingsImpl temporary = original.clone();
        ProgramRunner<?> nativeRunner = ProgramRunner.getRunner(environment.getExecutor().getId(), temporary.getConfiguration());
        if (nativeRunner == null) {
            notifyNotStarted(environment);
            throw new ExecutionException("The selected Run Configuration does not support " + environment.getExecutor().getActionName() + ".");
        }
        if (!(nativeRunner instanceof JvmPatchableProgramRunner<?>)) {
            notifyNotStarted(environment);
            throw new ExecutionException("The selected runner does not support automatic Java agent injection.");
        }
        MessInspectionConfiguration snapshot = (MessInspectionConfiguration)configuration.clone();
        TracingService service = environment.getProject().getService(TracingService.class);
        new Task.Backgroundable(environment.getProject(), "Preparing InstanceTracer", true) {
            private volatile PreparedInspection prepared;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.checkCanceled();
                    checkUnderlyingSettings(temporary, environment.getExecutor());
                    prepared = service.prepareInspection(configuration, snapshot);
                    indicator.checkCanceled();
                    injectAgentOption((JavaRunConfigurationBase)temporary.getConfiguration(), prepared.getAgentArgs());
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (indicator.isCanceled() || environment.getProject().isDisposed() || prepared.isClosed()) {
                            service.cancelInspection(prepared);
                            notifyNotStarted(environment);
                            return;
                        }
                        launch(environment, temporary, nativeRunner, prepared, service);
                    });
                } catch (ProcessCanceledException exception) {
                    if (prepared != null) service.cancelInspection(prepared);
                    notifyNotStarted(environment);
                    throw exception;
                } catch (ExecutionException | RuntimeException exception) {
                    if (prepared != null) service.cancelInspection(prepared);
                    reportFailure(environment, exception);
                }
            }

            @Override
            public void onCancel() {
                if (prepared != null) service.cancelInspection(prepared);
                notifyNotStarted(environment);
            }
        }.queue();
    }

    private static void checkUnderlyingSettings(RunnerAndConfigurationSettings settings, Executor executor) throws ExecutionException {
        try {
            ReadAction.run(() -> settings.checkSettings(executor));
        } catch (com.intellij.execution.configurations.RuntimeConfigurationException exception) {
            throw new ExecutionException("Fix the underlying Run Configuration '" + settings.getName()
                    + "' separately before starting MessInspection: " + exception.getMessage(), exception);
        }
    }

    private static void injectAgentOption(JavaRunConfigurationBase configuration, String agentOption) {
        List<String> parameters = new ArrayList<>(ParametersListUtil.parse(configuration.getVMParameters() == null
                ? "" : configuration.getVMParameters()));
        parameters.add(agentOption);
        configuration.setVMParameters(ParametersListUtil.join(parameters));
        configuration.putUserData(AGENT_OPTION, agentOption);
    }

    private void launch(ExecutionEnvironment source, RunnerAndConfigurationSettings temporary, ProgramRunner<?> nativeRunner,
                        PreparedInspection prepared, TracingService service) {
        MessageBusConnection connection = source.getProject().getMessageBus().connect(source.getProject());
        AtomicBoolean finished = new AtomicBoolean();
        AtomicBoolean processStarted = new AtomicBoolean();
        AtomicReference<EventManager.Subscription> closeSubscription = new AtomicReference<>();
        Runnable finish = () -> {
            if (finished.compareAndSet(false, true)) {
                connection.disconnect();
                EventManager.Subscription subscription = closeSubscription.getAndSet(null);
                if (subscription != null) subscription.close();
                service.cancelInspection(prepared);
                if (!processStarted.get()) notifyNotStarted(source);
            }
        };
        try {
            ProgramRunner<?> reroutingRunner = new ReroutingRunner(nativeRunner, source, this, finish);
            ExecutionEnvironment delegated = new ExecutionEnvironmentBuilder(source)
                    .runnerAndSettings(nativeRunner, temporary)
                    .runner(reroutingRunner)
                    .build(new Callback() {
                        @Override
                        public void processStarted(com.intellij.execution.ui.RunContentDescriptor descriptor) {
                            Callback callback = source.getCallback();
                            source.setCallback(null);
                            if (callback != null) callback.processStarted(descriptor);
                        }

                        @Override
                        public void processNotStarted() {
                            finish.run();
                        }
                    });
            delegated.putUserData(INSPECTION, prepared);
            connection.subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
                private boolean owns(ExecutionEnvironment environment) {
                    return environment.getUserData(INSPECTION) == prepared
                            && environment.getRunProfile() == temporary.getConfiguration();
                }

                @Override
                public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment environment,
                                           @NotNull ProcessHandler handler) {
                    if (owns(environment)) {
                        service.associateProcess(prepared, environment, handler);
                        processStarted.set(true);
                        if (prepared.isClosed()) finish.run();
                    }
                }

                @Override
                public void processNotStarted(@NotNull String executorId, @NotNull ExecutionEnvironment environment) {
                    if (owns(environment)) finish.run();
                }

                @Override
                public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment environment,
                                              @NotNull ProcessHandler handler, int exitCode) {
                    if (owns(environment)) finish.run();
                }
            });
            closeSubscription.set(prepared.getViewModel().getEventManager().subscribe(InspectionClosedEvent.class, event -> {
                // A fast agent may disconnect before the native process-start notification.
                if (processStarted.get()) finish.run();
            }));
            if (prepared.isClosed() || source.getProject().isDisposed()) {
                finish.run();
                return;
            }
            if (ExecutionManager.getInstance(source.getProject()).isStarting(delegated)) {
                finish.run();
                reportFailure(source, new ExecutionException("Another Java execution is starting. Start MessInspection again after that launch finishes."));
                return;
            }
            delegated.assignNewExecutionId();
            // Native runner.startRunProfile performs the copied configuration's before-run tasks.
            reroutingRunner.execute(delegated);
        } catch (ExecutionException | RuntimeException exception) {
            finish.run();
            reportFailure(source, exception);
        }
    }

    private static void reportFailure(ExecutionEnvironment environment, Exception failure) {
        ExecutionException exception = failure instanceof ExecutionException
                ? (ExecutionException)failure : new ExecutionException("Cannot start MessInspection: " + failure.getMessage(), failure);
        ApplicationManager.getApplication().invokeLater(() -> {
            completeNotStarted(environment);
            if (!environment.getProject().isDisposed()) ExecutionUtil.handleExecutionError(environment, exception);
        });
    }

    private static void notifyNotStarted(ExecutionEnvironment environment) {
        ApplicationManager.getApplication().invokeLater(() -> completeNotStarted(environment));
    }

    private static void completeNotStarted(ExecutionEnvironment environment) {
        Callback callback = environment.getCallback();
        environment.setCallback(null);
        if (callback != null) callback.processNotStarted();
    }

    /** Keeps native execution and debugger settings, while standard Rerun prepares a fresh inspection. */
    private static final class ReroutingRunner implements JvmPatchableProgramRunner<RunnerSettings> {
        private final ProgramRunner<?> delegate;
        private final ExecutionEnvironment source;
        private final MessInspectionProgramRunner inspectionRunner;
        private final AtomicBoolean initialExecution = new AtomicBoolean(true);
        private final Runnable finish;

        private ReroutingRunner(ProgramRunner<?> delegate, ExecutionEnvironment source, MessInspectionProgramRunner inspectionRunner,
                               Runnable finish) {
            this.delegate = delegate;
            this.source = source;
            this.inspectionRunner = inspectionRunner;
            this.finish = finish;
        }

        @Override
        public @NotNull String getRunnerId() { return delegate.getRunnerId(); }

        @Override
        public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
            return delegate.canRun(executorId, profile);
        }

        @Override
        public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
            if (initialExecution.compareAndSet(true, false)) {
                try {
                    delegate.execute(environment);
                } catch (ExecutionException | RuntimeException exception) {
                    finish.run();
                    throw exception;
                }
            } else {
                ExecutionEnvironment rerun = new ExecutionEnvironmentBuilder(source)
                        .executor(environment.getExecutor())
                        .target(environment.getExecutionTarget())
                        .contentToReuse(environment.getContentToReuse())
                        .build();
                inspectionRunner.execute(rerun);
            }
        }

        @Override
        public void patch(@NotNull JavaParameters parameters, RunnerSettings settings, @NotNull RunProfile profile,
                          boolean beforeExecution) throws ExecutionException {
            if (delegate instanceof JvmPatchableProgramRunner<?> patchable) {
                patchable.patch(parameters, settings, profile, beforeExecution);
            } else {
                throw new ExecutionException("The selected runner does not support Java parameter patching.");
            }
        }
    }
}
