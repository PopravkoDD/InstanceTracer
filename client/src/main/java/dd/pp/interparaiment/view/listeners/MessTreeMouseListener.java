package dd.pp.interparaiment.view.listeners;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;

import dd.pp.interparaiment.immodel.CallingClass;
import dd.pp.interparaiment.immodel.CallingLine;
import dd.pp.interparaiment.immodel.CallingMethod;
import dd.pp.interparaiment.immodel.IMessNode;
import dd.pp.interparaiment.view.model.construction.ConstructionTreeNode;

public class MessTreeMouseListener extends MouseAdapter {
    private final JTree tree;
    private final Project project;

    public MessTreeMouseListener(final JTree tree, final Project project) {
        this.tree = tree;
        this.project = project;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
            TreePath path = tree.getPathForLocation(e.getX(), e.getY());

            if (path == null) {
                return;
            }

            final ConstructionTreeNode node = (ConstructionTreeNode) path.getLastPathComponent();

            final IMessNode vmNode = node.getVmNode();

            if (vmNode instanceof CallingClass) {
                navigateToClass(node);
            } else if (vmNode instanceof CallingMethod) {
//                navigateToMethod(node);
            } else if (vmNode instanceof CallingLine) {
                navigateToLine(node);
            }
        }
    }

    private void navigateToClass(final ConstructionTreeNode node) {
        PsiClass psiClass = JavaPsiFacade.getInstance(this.project)
                .findClass(node.getFullValue(), GlobalSearchScope.allScope(this.project));

        if (psiClass != null && psiClass.canNavigate()) {
            psiClass.navigate(true);
        }
    }

    private void navigateToMethod(final ConstructionTreeNode node) {
        final ConstructionTreeNode callingClass = ((ConstructionTreeNode) node.getParent());

        PsiClass psiClass = JavaPsiFacade.getInstance(this.project)
                .findClass(callingClass.getFullValue(), GlobalSearchScope.allScope(this.project));

        if (psiClass == null) {
            return;
        }

        PsiMethod[] methods = psiClass.findMethodsByName(node.getFullValue(), true);
        if (methods.length > 0 && methods[0].canNavigate()) {
            methods[0].navigate(true);
        }

    }

    private void navigateToLine(final ConstructionTreeNode node) {

    }
}
