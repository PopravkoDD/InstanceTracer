package dd.pp.interparaiment.view.windows;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import dd.pp.interparaiment.view.controls.InstanceTrackerPanel;

public class InstanceTrackerToolWindow implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        final InstanceTrackerPanel panel = new InstanceTrackerPanel(project);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "Main", false);
        toolWindow.getContentManager().addContent(content);
    }
}
