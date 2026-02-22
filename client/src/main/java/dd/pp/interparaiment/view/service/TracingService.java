package dd.pp.interparaiment.view.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import dd.pp.interparaiment.InspectorViewModel;

@Service(Service.Level.PROJECT)
public final class TracingService implements Disposable {
    private final Project project;
    private final InspectorViewModel viewModel = new InspectorViewModel();

    public TracingService(Project project) {
        this.project = project;
    }

    public InspectorViewModel getViewModel() {
        return this.viewModel;
    }

    public void start() {}

    @Override
    public void dispose() {

    }
}
