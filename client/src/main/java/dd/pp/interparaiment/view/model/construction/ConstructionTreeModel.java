package dd.pp.interparaiment.view.model.construction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.Lock;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import dd.pp.interparaiment.immodel.MessModel;

public class ConstructionTreeModel implements TreeModel {

    private MessModel messModel;

    private ConstructionTreeNode root;

    public ConstructionTreeModel(final MessModel messModel) {
        this.messModel = messModel;
        this.root = new ConstructionTreeNode(null, messModel);
    }

    public void syncModels() {
        final Lock readLock = messModel.getReadLock();
        readLock.lock();
        try {
            dfsPreOrder();
        } finally {
            readLock.unlock();
        }
    }

    public  void dfsPreOrder() {
        Deque<ConstructionTreeNode> stack = new ArrayDeque<>();
        stack.push(this.root);

        while (!stack.isEmpty()) {
            ConstructionTreeNode current = stack.pop();

            current.syncModels();

            List<ConstructionTreeNode> children = current.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                ConstructionTreeNode child = children.get(i);
                if (child != null) {
                    stack.push(child);
                }
            }
        }
    }

    @Override
    public Object getRoot() {
        return this.root;
    }

    @Override
    public Object getChild(Object parent, int index) {
        return ((ConstructionTreeNode) parent).getChildAt(index);
    }

    @Override
    public int getChildCount(Object parent) {
        return ((ConstructionTreeNode) parent).getChildCount();
    }

    @Override
    public boolean isLeaf(Object node) {
        return ((ConstructionTreeNode) node).isLeaf();
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // ignored
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return ((ConstructionTreeNode) parent).getIndex(((ConstructionTreeNode) child));
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        // ignored
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        // ignored
    }
}
