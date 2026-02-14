package dd.pp.interparaiment.view.controls;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;

import dd.pp.interparaiment.view.actions.ConfigureTrackerAction;
import dd.pp.interparaiment.view.actions.TrackerStartAction;
import dd.pp.interparaiment.view.actions.TrackerStopAction;
import dd.pp.interparaiment.view.actions.TrackingProcessState;

public class InstanceTrackerPanel extends JBPanel {
    public InstanceTrackerPanel(final Project project) {
        super(new BorderLayout());

        JBLabel title = new JBLabel("Instance tracker");
        title.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        final JComponent view = initView(project);

        add(title, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);
    }

    private JComponent initView(final Project project) {
        final TrackerLogView logView = new TrackerLogView();

        //Tree view
        final JPanel treePanel = new JPanel(new BorderLayout());

        final JComponent miniToolbar = createMiniToolbar();

        treePanel.add(miniToolbar, BorderLayout.NORTH);

        JTree tree = new JTree();
        treePanel.add(new JBScrollPane(tree), BorderLayout.CENTER);

        final JBSplitter splitter = new JBSplitter(false, 0.25f);
        splitter.setFirstComponent(treePanel);
        splitter.setSecondComponent(new JBScrollPane(logView));

        return splitter;
    }

    private static JComponent createMiniToolbar() {
        final DefaultActionGroup group = new DefaultActionGroup();
        final TrackingProcessState state = new TrackingProcessState();

        final TrackerStartAction startAction = new TrackerStartAction(state);
        final TrackerStopAction trackerStopAction = new TrackerStopAction(state);
        final ConfigureTrackerAction configureTrackerAction = new ConfigureTrackerAction(state);

        group.add(startAction);
        group.add(trackerStopAction);

        group.addSeparator();

        group.add(configureTrackerAction);

        final ActionToolbar toolbar = ActionManager.getInstance()
                .createActionToolbar("TrackerToolbar", group, true);

        toolbar.setTargetComponent(null);
        return toolbar.getComponent();
    }
}
