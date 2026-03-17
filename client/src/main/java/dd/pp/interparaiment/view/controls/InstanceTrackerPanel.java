package dd.pp.interparaiment.view.controls;

import java.awt.BorderLayout;

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

import dd.pp.interparaiment.InspectorViewModel;
import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;
import dd.pp.interparaiment.view.actions.ConfigureTrackerAction;
import dd.pp.interparaiment.view.actions.TrackerStartAction;
import dd.pp.interparaiment.view.actions.TrackerStopAction;
import dd.pp.interparaiment.view.actions.MessActionsContext;
import dd.pp.interparaiment.view.model.construction.ConstructionTreeModel;
import dd.pp.interparaiment.view.service.TracingService;

public class InstanceTrackerPanel extends JBPanel {

    private UIUpdater updater;
    private final EventManager eventManager;
    private final InspectorViewModel viewModel;

    public InstanceTrackerPanel(final Project project) {
        super(new BorderLayout());

        this.viewModel = project.getService(TracingService.class).getViewModel();
        this.eventManager = this.viewModel.getEventManager();

        JBLabel title = new JBLabel("Instance tracker");
        title.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        final JComponent view = initView();

        add(title, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);
    }

    private JComponent initView() {
        final TrackerLogView logView = new TrackerLogView();

        eventManager.subscribe(ShowMessageInConsoleRequest.class, data -> logView.appendLine(data.getData()));

        //Tree view
        final JPanel treePanel = new JPanel(new BorderLayout());

        final JComponent miniToolbar = createMiniToolbar();

        treePanel.add(miniToolbar, BorderLayout.NORTH);


        final ConstructionTreeModel treeModel = new ConstructionTreeModel(this.viewModel.getMessModel());

        JTree tree = new JTree(treeModel);

        treePanel.add(new JBScrollPane(tree), BorderLayout.CENTER);

        this.updater = new UIUpdater(treePanel, treeModel::syncModels);

        final JBSplitter splitter = new JBSplitter(false, 0.25f);
        splitter.setFirstComponent(treePanel);
        splitter.setSecondComponent(new JBScrollPane(logView));

        return splitter;
    }

    private JComponent createMiniToolbar() {

        final DefaultActionGroup group = new DefaultActionGroup();
        final MessActionsContext state = new MessActionsContext(this.updater);


        final TrackerStartAction startAction = new TrackerStartAction(state, this.eventManager);
        final TrackerStopAction trackerStopAction = new TrackerStopAction(state, this.eventManager);
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
