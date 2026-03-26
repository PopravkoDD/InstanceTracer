package dd.pp.interparaiment.view.renderers;

import javax.swing.Icon;
import javax.swing.JTree;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;

import dd.pp.interparaiment.immodel.CallingClass;
import dd.pp.interparaiment.immodel.CallingLine;
import dd.pp.interparaiment.immodel.CallingMethod;
import dd.pp.interparaiment.immodel.IMessNode;
import dd.pp.interparaiment.immodel.MessModel;
import dd.pp.interparaiment.immodel.TracedTarget;
import dd.pp.interparaiment.view.model.construction.ConstructionTreeNode;

public class MessTreeRenderer extends ColoredTreeCellRenderer {
    private static final Icon ROOT_ICON = AllIcons.Nodes.InspectionResults;
    private static final Icon TARGET_ICON = AllIcons.Nodes.Bookmark;
    private static final Icon CLASS_ICON = AllIcons.Nodes.Class;
    private static final Icon METHOD_ICON = AllIcons.Nodes.Method;
    private static final Icon LINE_ICON= AllIcons.Nodes.BookmarkGroup;
    @Override
    public void customizeCellRenderer(@NotNull JTree jTree, Object node, boolean b, boolean b1, boolean b2, int i, boolean b3) {
        final ConstructionTreeNode constructionNode = (ConstructionTreeNode) node;

        append(constructionNode.getSimpleValue());

        final IMessNode vmNode = constructionNode.getVmNode();
        if (vmNode instanceof MessModel) {
            setIcon(ROOT_ICON);
        } else if (vmNode instanceof TracedTarget) {
            setIcon(TARGET_ICON);
        } else if (vmNode instanceof CallingClass) {
            setIcon(CLASS_ICON);
        } else if (vmNode instanceof CallingMethod) {
            setIcon(METHOD_ICON);
        } else if (vmNode instanceof CallingLine) {
            setIcon(LINE_ICON);
        }
    }
}
