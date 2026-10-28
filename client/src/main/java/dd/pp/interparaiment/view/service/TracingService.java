package dd.pp.interparaiment.view.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import dd.pp.interparaiment.InspectorViewModel;
import dd.pp.interparaiment.event.requests.AgentConfigurationChangedEvent;

@Service(Service.Level.PROJECT)
public final class TracingService implements Disposable {
    private final Project project;
    private final InspectorViewModel viewModel;

    public TracingService(Project project) {
        this.project = project;
        TrackerSettingsService settings = project.getService(TrackerSettingsService.class);
        viewModel = new InspectorViewModel(settings.getViewModelConfiguration());
        viewModel.getEventManager().subscribe(AgentConfigurationChangedEvent.class,
                event -> settings.saveViewModelConfiguration(event.getData()));
    }

    public InspectorViewModel getViewModel() {
        return this.viewModel;
    }

    public void start() {}

    @Override
    public void dispose() {
        viewModel.close();
    }
}
