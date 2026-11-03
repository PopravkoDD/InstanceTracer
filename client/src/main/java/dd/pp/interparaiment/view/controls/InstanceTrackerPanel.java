package dd.pp.interparaiment.view.controls;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;

import dd.pp.interparaiment.InspectorViewModel;
import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.requests.InstanceCreatedEvent;
import dd.pp.interparaiment.event.requests.InspectionClosedEvent;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;
import dd.pp.interparaiment.view.listeners.InstanceConstructionListener;
import dd.pp.interparaiment.view.actions.ConfigureTrackerAction;
import dd.pp.interparaiment.view.actions.DumpTheMessAction;
import dd.pp.interparaiment.view.actions.StartRecordingTrackerAction;
import dd.pp.interparaiment.view.actions.StopRecordingTrackerAction;
import dd.pp.interparaiment.view.actions.MessActionsContext;
import dd.pp.interparaiment.view.listeners.MessTreeMouseListener;
import dd.pp.interparaiment.view.model.construction.ConstructionTreeModel;
import dd.pp.interparaiment.view.model.construction.ConstructionTreeNode;
import dd.pp.interparaiment.view.renderers.MessTreeRenderer;
import dd.pp.interparaiment.view.execution.MessInspectionConfiguration;

public class InstanceTrackerPanel extends JBPanel implements Disposable {

    private UIUpdater updater;
    private final EventManager eventManager;
    private final InspectorViewModel viewModel;

    private final Project project;
    private final MessInspectionConfiguration configuration;
    private InstanceConstructionListener constructionListener;
    private EventManager.Subscription consoleSubscription;
    private EventManager.Subscription constructionSubscription;
    private EventManager.Subscription closedSubscription;
    private MessLogView logView;

    public InstanceTrackerPanel(Project project, InspectorViewModel viewModel, MessInspectionConfiguration configuration) {
        super(new BorderLayout());

        this.project = project;
        this.configuration = configuration;
        this.viewModel = viewModel;
        this.eventManager = this.viewModel.getEventManager();

        JBLabel title = new JBLabel("Instance tracker");
        title.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        final JComponent view = initView(project);

        add(title, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);
    }

    private JComponent initView(final Project project) {
        this.logView = new MessLogView(project);
        this.constructionListener = new InstanceConstructionListener(logView, project);
        this.consoleSubscription = this.eventManager.subscribe(ShowMessageInConsoleRequest.class,
                data -> logView.logInfo(data.getData()));
        this.constructionSubscription = this.eventManager.subscribe(InstanceCreatedEvent.class, constructionListener);

        //Tree view
        final JPanel treePanel = new JPanel(new BorderLayout());

        final ConstructionTreeModel treeModel = new ConstructionTreeModel(this.viewModel.getMessModel(), new ConstructionTreeNode(null, this.viewModel.getMessModel()));
        final JTree tree = createMessTree(treeModel, project);

        this.updater = new UIUpdater(tree, treeModel::syncModels);
        this.closedSubscription = this.eventManager.subscribe(InspectionClosedEvent.class, event ->
                ApplicationManager.getApplication().invokeLater(this::finishSessionUi, project.getDisposed()));
        if (!viewModel.isInspectionActive()) {
            finishSessionUi();
        }

        final JComponent miniToolbar = createMiniToolbar();
        treePanel.add(miniToolbar, BorderLayout.NORTH);
        treePanel.add(new JBScrollPane(tree), BorderLayout.CENTER);

        final JBSplitter splitter = new JBSplitter(false, 0.25f);
        splitter.setFirstComponent(treePanel);
        splitter.setSecondComponent(logView);

        return splitter;
    }

    @NotNull
    private static JTree createMessTree(ConstructionTreeModel treeModel, final Project project) {
        final JTree tree = new JTree(treeModel);
        tree.setCellRenderer(new MessTreeRenderer());

        tree.addMouseListener(new MessTreeMouseListener(tree, project));

        return tree;
    }

    private JComponent createMiniToolbar() {

        final DefaultActionGroup group = new DefaultActionGroup();
        final MessActionsContext state = new MessActionsContext(this.updater);

        final DumpTheMessAction dumpMessAction = new DumpTheMessAction(state, this.eventManager);
        final StartRecordingTrackerAction startAction = new StartRecordingTrackerAction(state, this.viewModel);
        final StopRecordingTrackerAction trackerStopAction = new StopRecordingTrackerAction(state, this.viewModel);
        final ConfigureTrackerAction configureTrackerAction = new ConfigureTrackerAction(project, configuration);

        group.add(startAction);
        group.add(dumpMessAction);
        group.add(trackerStopAction);

        group.addSeparator();

        group.add(configureTrackerAction);

        final ActionToolbar toolbar = ActionManager.getInstance()
                .createActionToolbar("TrackerToolbar", group, true);

        toolbar.setTargetComponent(this);
        return toolbar.getComponent();
    }

    private void finishSessionUi() {
        consoleSubscription.close();
        constructionSubscription.close();
        closedSubscription.close();
        constructionListener.finish();
        updater.update();
    }

    @Override
    public void dispose() {
        consoleSubscription.close();
        constructionSubscription.close();
        closedSubscription.close();
        constructionListener.dispose();
        logView.dispose();
    }
}
