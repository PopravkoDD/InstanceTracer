package dd.pp.interparaiment.view.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.JavaCodeFragmentFactory;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.refactoring.ui.ClassNameReferenceEditor;
import com.intellij.ui.ReferenceEditorWithBrowseButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.FormBuilder;
import dd.pp.interparaiment.InspectorConfiguration;
import dd.pp.interparaiment.InspectorViewModel;
import dd.pp.interparaiment.view.service.ClientConfiguration;
import dd.pp.interparaiment.view.service.TrackerSettingsService;
import org.jetbrains.annotations.Nullable;

public final class TrackerConfigurationDialog extends DialogWrapper {
    private final Project project;
    private final InspectorViewModel viewModel;
    private final TrackerSettingsService settings;
    private final JTextField host = new JTextField();
    private final JTextField port = new JTextField();
    private final JCheckBox debugMode = new JCheckBox("Debug mode");
    private final JTextField developmentRoot = new JTextField();
    private final DefaultListModel<String> targets = new DefaultListModel<>();
    private final JBList<String> targetList = new JBList<>(targets);

    public TrackerConfigurationDialog(Project project, InspectorViewModel viewModel,
                                      TrackerSettingsService settings) {
        super(project);
        this.project = project;
        this.viewModel = viewModel;
        this.settings = settings;
        InspectorConfiguration options = viewModel.getConfiguration();
        ClientConfiguration client = settings.getClientConfiguration();
        host.setText(options.host());
        port.setText(Integer.toString(options.port()));
        options.traceTargets().forEach(targets::addElement);
        debugMode.setSelected(client.debugMode());
        developmentRoot.setText(client.developmentProjectRoot());
        setTitle("Configure InstanceTracer");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel listPanel = ToolbarDecorator.createDecorator(targetList)
                .setAddAction(button -> editTarget(-1))
                .setEditAction(button -> editTarget(targetList.getSelectedIndex()))
                .setRemoveAction(button -> {
                    int[] selected = targetList.getSelectedIndices();
                    for (int i = selected.length - 1; i >= 0; i--) {
                        targets.remove(selected[i]);
                    }
                })
                .disableUpDownActions().createPanel();
        listPanel.setPreferredSize(new Dimension(520, 180));
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Host:", host)
                .addLabeledComponent("Port:", port)
                .addComponent(debugMode)
                .addLabeledComponent("Trace targets:", listPanel, true)
                .addComponent(new JBLabel("Class completion and search include All Places. Unresolved names are kept."))
                .addLabeledComponent("Development project root:", developmentRoot)
                .addComponent(new JBLabel("Leave empty to locate InstanceTracer above the open project."))
                .getPanel();
    }

    private void editTarget(int index) {
        TargetDialog dialog = new TargetDialog(project, index < 0 ? "" : targets.get(index));
        if (dialog.showAndGet()) {
            if (index < 0) {
                targets.addElement(dialog.getTarget());
            } else {
                targets.set(index, dialog.getTarget());
            }
        }
    }

    private InspectorConfiguration readOptions() {
        final int enteredPort;
        try {
            enteredPort = Integer.parseInt(port.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Port must be an integer between 1 and 65535");
        }
        List<String> traceTargets = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) {
            traceTargets.add(targets.get(i));
        }
        return new InspectorConfiguration(host.getText(), enteredPort, traceTargets);
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        try {
            readOptions();
            readClientOptions();
            return null;
        } catch (IllegalArgumentException e) {
            return new ValidationInfo(e.getMessage());
        }
    }

    @Override
    protected void doOKAction() {
        final InspectorConfiguration options;
        final ClientConfiguration client;
        try {
            options = readOptions();
            client = readClientOptions();
        } catch (IllegalArgumentException e) {
            setErrorText(e.getMessage());
            return;
        }
        viewModel.updateOptions(options);
        settings.updateClientConfiguration(client);
        super.doOKAction();
    }

    private ClientConfiguration readClientOptions() {
        return new ClientConfiguration(debugMode.isSelected(), developmentRoot.getText());
    }

    private static final class TargetDialog extends DialogWrapper {
        private final Project project;
        private final GlobalSearchScope scope;
        private final ReferenceEditorWithBrowseButton editor;

        private TargetDialog(Project project, String value) {
            super(project);
            this.project = project;
            scope = GlobalSearchScope.allScope(project);
            editor = new ReferenceEditorWithBrowseButton(event -> chooseClass(), project, text -> {
                JavaCodeFragment fragment = JavaCodeFragmentFactory.getInstance(project)
                        .createReferenceCodeFragment(text, JavaPsiFacade.getInstance(project).findPackage(""), true, true);
                fragment.setVisibilityChecker(JavaCodeFragment.VisibilityChecker.EVERYTHING_VISIBLE);
                fragment.forceResolveScope(scope);
                fragment.putUserData(ClassNameReferenceEditor.CLASS_NAME_REFERENCE_FRAGMENT, true);
                return PsiDocumentManager.getInstance(project).getDocument(fragment);
            }, value);
            setTitle("Trace Target");
            init();
        }

        private void chooseClass() {
            if (DumbService.isDumb(project)) {
                setErrorText("Class search is available after indexing; you can enter a fully qualified name now");
                return;
            }
            TreeClassChooser chooser = TreeClassChooserFactory.getInstance(project)
                    .createWithInnerClassesScopeChooser("Choose Trace Target — All Places", scope,
                            psiClass -> psiClass.getQualifiedName() != null, null);
            chooser.showDialog();
            PsiClass selected = chooser.getSelected();
            if (selected != null) {
                // JVM-qualified names preserve '$' for nested classes, as expected by the agent matcher.
                String name = ReadAction.compute(() -> ClassUtil.getJVMClassName(selected));
                if (name != null) {
                    editor.setText(name);
                }
            }
        }

        private String getTarget() {
            String text = editor.getText();
            if (DumbService.isDumb(project)) {
                return text;
            }
            try {
                PsiDocumentManager documents = PsiDocumentManager.getInstance(project);
                documents.commitDocument(editor.getEditorTextField().getDocument());
                return ReadAction.compute(() -> {
                    PsiFile file = documents.getPsiFile(editor.getEditorTextField().getDocument());
                    PsiJavaCodeReferenceElement reference = file == null ? null
                            : PsiTreeUtil.findChildOfType(file, PsiJavaCodeReferenceElement.class);
                    PsiElement resolved = reference == null ? null : reference.resolve();
                    PsiClass selected = resolved instanceof PsiClass ? (PsiClass) resolved
                            : JavaPsiFacade.getInstance(project).findClass(text, scope);
                    String name = selected == null ? null : ClassUtil.getJVMClassName(selected);
                    return name == null ? text : name;
                });
            } catch (IndexNotReadyException e) {
                return text;
            }
        }

        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(editor, BorderLayout.CENTER);
            panel.setPreferredSize(new Dimension(520, 40));
            return panel;
        }

        @Override
        public JComponent getPreferredFocusedComponent() {
            return editor.getEditorTextField();
        }

        @Override
        protected @Nullable ValidationInfo doValidate() {
            try {
                new InspectorConfiguration("127.0.0.1", 35824, List.of(editor.getText()));
                return null;
            } catch (IllegalArgumentException e) {
                return new ValidationInfo(e.getMessage(), editor);
            }
        }
    }
}
