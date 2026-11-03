package dd.pp.interparaiment.view.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import dd.pp.interparaiment.InspectorConfiguration;
import dd.pp.interparaiment.InspectorViewModel;
import dd.pp.interparaiment.PreparedInspection;
import dd.pp.interparaiment.event.requests.InspectionClosedEvent;
import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.view.controls.InstanceTrackerPanel;
import dd.pp.interparaiment.view.execution.MessInspectionConfiguration;

/** Owns runtime sessions; persistent settings belong to each Run Configuration. */
@Service(Service.Level.PROJECT)
public final class TracingService implements Disposable {
    private final Project project;
    private final IJavaAgentResolver resolver = new DevJavaAgentResolver();
    private final List<InspectionSession> sessions = new CopyOnWriteArrayList<>();
    private final AtomicBoolean disposed = new AtomicBoolean();
    private ToolWindow toolWindow;

    public TracingService(Project project) {
        this.project = project;
    }

    public InspectorConfiguration resolveConfiguration(MessInspectionConfiguration configuration) throws IOException {
        JavaAgentArtifacts artifacts = resolver.resolve();
        return new InspectorConfiguration(configuration.getTraceTargets(), configuration.getHost(),
                configuration.getPort(), artifacts.agentPath(), artifacts.bridgePath(), configuration.isRecordOnStart());
    }

    public PreparedInspection prepareInspection(MessInspectionConfiguration configuration) throws ExecutionException {
        return prepareInspection(configuration, configuration);
    }

    public synchronized PreparedInspection prepareInspection(MessInspectionConfiguration owner,
                                                              MessInspectionConfiguration configuration) throws ExecutionException {
        if (disposed.get()) {
            throw new ExecutionException("The project is closing; an inspection cannot be started.");
        }
        try {
            InspectorViewModel viewModel = new InspectorViewModel(resolveConfiguration(configuration));
            PreparedInspection prepared = viewModel.prepareInspection();
            InspectionSession session = new InspectionSession(owner, viewModel, prepared);
            sessions.add(session);
            session.closedSubscription = viewModel.getEventManager().subscribe(InspectionClosedEvent.class, event -> {
                EventManager.Subscription subscription = session.closedSubscription;
                if (subscription != null) {
                    subscription.close();
                }
                ApplicationManager.getApplication().invokeLater(() -> updateTitle(session), project.getDisposed());
            });
            if (prepared.isClosed()) {
                session.closedSubscription.close();
            }
            ApplicationManager.getApplication().invokeLater(() -> showSession(session), project.getDisposed());
            return prepared;
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            throw new ExecutionException("Cannot prepare InstanceTracer: " + e.getMessage(), e);
        }
    }

    public void associateProcess(PreparedInspection prepared, ExecutionEnvironment environment, ProcessHandler process) {
        InspectionSession session = findSession(prepared);
        if (session == null) {
            return;
        }
        synchronized (session) {
            if (session.finished.get()) {
                return;
            }
            session.process = process;
        }
        ApplicationManager.getApplication().invokeLater(() -> updateTitle(session), project.getDisposed());
    }

    /** Idempotent, shared by failed launch, process termination and project disposal. */
    public void cancelInspection(PreparedInspection prepared) {
        InspectionSession session = findSession(prepared);
        if (session == null) {
            return;
        }
        boolean processStarted;
        synchronized (session) {
            if (!session.finished.compareAndSet(false, true)) {
                return;
            }
            processStarted = session.process != null;
        }
        if (ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().executeOnPooledThread(prepared::close);
        } else {
            prepared.close();
        }
        if (!processStarted) {
            sessions.remove(session);
            ApplicationManager.getApplication().invokeLater(() -> {
                if (toolWindow != null && session.content != null) {
                    toolWindow.getContentManager().removeContent(session.content, true);
                }
            }, project.getDisposed());
        } else {
            ApplicationManager.getApplication().invokeLater(() -> updateTitle(session), project.getDisposed());
        }
    }

    private InspectionSession findSession(PreparedInspection prepared) {
        return sessions.stream().filter(session -> session.prepared == prepared).findFirst().orElse(null);
    }

    public void attachToolWindow(ToolWindow window) {
        toolWindow = window;
        sessions.forEach(this::showSession);
    }

    private void showSession(InspectionSession session) {
        if (disposed.get() || toolWindow == null || session.content != null || !sessions.contains(session)) {
            return;
        }
        InstanceTrackerPanel panel = new InstanceTrackerPanel(project, session.viewModel, session.configuration);
        session.content = ContentFactory.getInstance().createContent(panel, session.configuration.getName(), false);
        session.content.setDisposer(panel);
        updateTitle(session);
        toolWindow.getContentManager().addContent(session.content);
        toolWindow.getContentManager().setSelectedContent(session.content);
    }

    private void updateTitle(InspectionSession session) {
        if (session.content != null) {
            String state = session.finished.get() || session.prepared.isClosed() ? "ended"
                    : session.process == null ? "starting" : "running";
            session.content.setDisplayName(session.configuration.getName() + " : "
                    + session.prepared.getConfiguration().port() + " (" + state + ")");
        }
    }

    @Override
    public synchronized void dispose() {
        if (disposed.compareAndSet(false, true)) {
            sessions.forEach(session -> cancelInspection(session.prepared));
            sessions.clear();
        }
    }

    private static final class InspectionSession {
        private final MessInspectionConfiguration configuration;
        private final InspectorViewModel viewModel;
        private final PreparedInspection prepared;
        private final AtomicBoolean finished = new AtomicBoolean();
        private volatile ProcessHandler process;
        private Content content;
        private volatile EventManager.Subscription closedSubscription;

        private InspectionSession(MessInspectionConfiguration configuration, InspectorViewModel viewModel,
                                  PreparedInspection prepared) {
            this.configuration = configuration;
            this.viewModel = viewModel;
            this.prepared = prepared;
        }
    }
}
