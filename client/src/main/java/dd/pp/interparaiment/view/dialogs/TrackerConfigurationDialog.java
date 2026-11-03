package dd.pp.interparaiment.view.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jetbrains.annotations.Nullable;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
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
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiJavaCodeReferenceCodeFragment;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;

/** Edits a private list; the caller applies it through ViewModel only after confirmation. */
public final class TrackerConfigurationDialog extends DialogWrapper {
    private final Project project;
    private final DefaultListModel<String> targets = new DefaultListModel<>();
    private final JBList<String> targetList = new JBList<>(this.targets);
    private final PsiJavaCodeReferenceCodeFragment referenceFragment;
    private final EditorTextField className;

    public TrackerConfigurationDialog(final Project project, final List<String> traceTargets) {
        super(project);
        this.project = project;
        traceTargets.forEach(this.targets::addElement);
        this.referenceFragment = JavaCodeFragmentFactory.getInstance(project).createReferenceCodeFragment(
                "", JavaPsiFacade.getInstance(project).findPackage(""), true, true);
        this.referenceFragment.forceResolveScope(GlobalSearchScope.allScope(project));
        this.referenceFragment.setVisibilityChecker(JavaCodeFragment.VisibilityChecker.EVERYTHING_VISIBLE);
        final Document document = PsiDocumentManager.getInstance(project).getDocument(this.referenceFragment);
        this.className = new EditorTextField(document, project, JavaFileType.INSTANCE);
        this.className.setPlaceholder("Fully qualified Java class (Ctrl+Space for completion)");
        this.className.setDisposedWith(getDisposable());
        setTitle("InstanceTracer Trace Targets");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        final JPanel panel = new JPanel(new BorderLayout(0, 8));
        final JPanel input = new JPanel(new BorderLayout(8, 0));
        final JButton add = new JButton("Add");
        add.addActionListener(event -> addTarget());
        input.add(this.className, BorderLayout.CENTER);
        input.add(add, BorderLayout.EAST);

        final JPanel top = new JPanel(new BorderLayout(0, 4));
        top.add(new JLabel("Java class — All Places"), BorderLayout.NORTH);
        top.add(input, BorderLayout.CENTER);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JBScrollPane(this.targetList), BorderLayout.CENTER);

        final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        final JButton remove = new JButton("Remove selected");
        remove.addActionListener(event -> removeTargets());
        buttons.add(remove);
        panel.add(buttons, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(580, 300));
        return panel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return this.className;
    }

    private boolean addTarget() {
        if (DumbService.isDumb(this.project)) {
            setErrorText("Wait for Java indexing to finish before adding trace targets.", this.className);
            return false;
        }
        PsiDocumentManager.getInstance(this.project).commitDocument(this.className.getDocument());
        final String qualifiedName = ReadAction.compute(() -> {
            if (PsiTreeUtil.findChildOfType(this.referenceFragment, PsiErrorElement.class) != null) {
                return null;
            }
            final PsiJavaCodeReferenceElement reference = this.referenceFragment.getReferenceElement();
            final PsiElement resolved = reference == null ? null : reference.resolve();
            return resolved instanceof PsiClass psiClass ? psiClass.getQualifiedName() : null;
        });
        if (qualifiedName == null) {
            setErrorText("Select a resolvable Java class using its fully qualified name or code completion.", this.className);
            return false;
        }
        if (!this.targets.contains(qualifiedName)) {
            this.targets.addElement(qualifiedName);
        }
        this.className.setText("");
        setErrorText(null);
        return true;
    }

    private void removeTargets() {
        final int[] indices = this.targetList.getSelectedIndices();
        for (int index = indices.length - 1; index >= 0; index--) {
            this.targets.remove(indices[index]);
        }
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (DumbService.isDumb(this.project)) {
            return new ValidationInfo("Wait for Java indexing to finish before validating trace targets.", this.targetList);
        }
        return ReadAction.compute(() -> {
            for (String target : getTraceTargets()) {
                if (JavaPsiFacade.getInstance(this.project).findClass(target, GlobalSearchScope.allScope(this.project)) == null) {
                    return new ValidationInfo("Java class cannot be resolved in All Places: " + target, this.targetList);
                }
            }
            return null;
        });
    }

    @Override
    protected void doOKAction() {
        if (!this.className.getText().isBlank() && !addTarget()) {
            return;
        }
        super.doOKAction();
    }

    public List<String> getTraceTargets() {
        return List.copyOf(Collections.list(this.targets.elements()));
    }
}
