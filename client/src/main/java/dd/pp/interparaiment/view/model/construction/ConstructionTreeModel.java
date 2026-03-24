package dd.pp.interparaiment.view.model.construction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.Lock;

import javax.swing.tree.DefaultTreeModel;

import dd.pp.interparaiment.immodel.MessModel;

public class ConstructionTreeModel extends DefaultTreeModel {

    private final MessModel messModel;

    private final ConstructionTreeNode root;

    public ConstructionTreeModel(final MessModel messModel, final ConstructionTreeNode root) {
        super(root);
        this.messModel = messModel;
        this.root = root;
    }

    public void syncModels() {
        final Lock readLock = messModel.getReadLock();
        readLock.lock();
        try {
            dfsPreOrder();
//            this.reload();
        } finally {
            readLock.unlock();
        }
    }

    public  void dfsPreOrder() {
        Deque<ConstructionTreeNode> stack = new ArrayDeque<>();
        stack.push(this.root);

        while (!stack.isEmpty()) {
            ConstructionTreeNode current = stack.pop();

            syncModels(current);

            List<ConstructionTreeNode> children = current.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                ConstructionTreeNode child = children.get(i);
                if (child != null) {
                    stack.push(child);
                }
            }
        }
    }

    private void syncModels(ConstructionTreeNode current) {
        if (current.isLeaf() && !current.getVmNode().getFreshMeat().isEmpty()) {
            nodeChanged(current);
        } else {
            final int[] newIndexes = current.syncModels();

            if (newIndexes != null) {
                nodesWereInserted(current, newIndexes);
            }
        }

    }

//    @Override
//    public Object getRoot() {
//        return this.root;
//    }
//
//    @Override
//    public Object getChild(Object parent, int index) {
//        return ((ConstructionTreeNode) parent).getChildAt(index);
//    }
//
//    @Override
//    public int getChildCount(Object parent) {
//        return ((ConstructionTreeNode) parent).getChildCount();
//    }
//
//    @Override
//    public boolean isLeaf(Object node) {
//        return ((ConstructionTreeNode) node).isLeaf();
//    }
//
//    @Override
//    public void valueForPathChanged(TreePath path, Object newValue) {
//        // ignored
//    }
//
//    @Override
//    public int getIndexOfChild(Object parent, Object child) {
//        return ((ConstructionTreeNode) parent).getIndex(((ConstructionTreeNode) child));
//    }
//
//    @Override
//    public void addTreeModelListener(TreeModelListener l) {
//        // ignored
//    }
//
//    @Override
//    public void removeTreeModelListener(TreeModelListener l) {
//        // ignored
//    }
}
