package dd.pp.interparaiment.view.service;

import java.io.IOException;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import dd.pp.interparaiment.InspectorConfiguration;
import dd.pp.interparaiment.InspectorViewModel;
import dd.pp.interparaiment.event.requests.AgentConfigurationChangedEvent;

@Service(Service.Level.PROJECT)
public final class TracingService implements Disposable {
    private final Project project;
    private InspectorViewModel viewModel;
    private boolean disposed;

    public TracingService(Project project) {
        this.project = project;
    }

    /** Artifact resolution is kept out of the UI and the recording toggle actions. */
    public InspectorConfiguration resolveConfiguration(String host, int port) throws IOException {
        IJavaAgentResolver.AgentArtifacts artifacts = project.getService(IJavaAgentResolver.class).resolve();
        return new InspectorConfiguration(host, port,
                project.getService(TrackerSettingsService.class).getTraceTargets(), artifacts.agent(), artifacts.hooks());
    }

    public synchronized InspectorViewModel getViewModel() {
        if (disposed) {
            throw new IllegalStateException("Tracing service has been disposed");
        }
        if (viewModel == null) {
            try {
                InspectorConfiguration initial = resolveConfiguration(InspectorConfiguration.DEFAULT_HOST,
                        InspectorConfiguration.DEFAULT_PORT);
                return getViewModel(initial);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return viewModel;
    }

    public synchronized InspectorViewModel getViewModel(InspectorConfiguration initial) {
        if (disposed) {
            throw new IllegalStateException("Tracing service has been disposed");
        }
        if (viewModel == null) {
            viewModel = new InspectorViewModel(initial);
            TrackerSettingsService settings = project.getService(TrackerSettingsService.class);
            viewModel.getEventManager().subscribe(AgentConfigurationChangedEvent.class,
                    event -> settings.saveViewModelConfiguration(event.getData()));
        }
        return viewModel;
    }

    @Override
    public void dispose() {
        InspectorViewModel current;
        synchronized (this) {
            disposed = true;
            current = viewModel;
        }
        if (current != null) {
            current.close();
        }
    }
}
