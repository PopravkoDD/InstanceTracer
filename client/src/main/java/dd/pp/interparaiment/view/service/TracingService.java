package dd.pp.interparaiment.view.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

@Service(Service.Level.PROJECT)
public class TracingService implements Disposable {
    private final Project project;

    public TracingService(Project project) {
        this.project = project;
    }

    public void start() {}

    @Override
    public void dispose() {

    }
}
