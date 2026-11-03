package dd.pp.interparaiment.view.windows;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import dd.pp.interparaiment.view.actions.ConfigureTrackerAction;
import dd.pp.interparaiment.view.service.TracingService;

public class InstanceTrackerToolWindow implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JPanel welcome = new JPanel(new BorderLayout());
        welcome.add(new JBLabel("Create a MessInspection Run Configuration, select a target, then use Run or Debug."),
                BorderLayout.CENTER);
        DefaultActionGroup actions = new DefaultActionGroup(new ConfigureTrackerAction(project, null));
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("InspectionWelcome", actions, true);
        toolbar.setTargetComponent(welcome);
        welcome.add(toolbar.getComponent(), BorderLayout.NORTH);
        Content content = ContentFactory.getInstance().createContent(welcome, "InstanceTracer", false);
        toolWindow.getContentManager().addContent(content);
        project.getService(TracingService.class).attachToolWindow(toolWindow);
    }
}
