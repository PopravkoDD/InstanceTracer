package dd.pp.interparaiment.view.model.construction;

import java.util.Collection;
import java.util.Enumeration;
import java.util.NoSuchElementException;


import dd.pp.interparaiment.immodel.IMessNode;
import javax.swing.tree.TreeNode;

public class ConstructionTreeNode implements TreeNode {
    private final IMessNode vmNode;
    private ConstructionTreeNode parent;
    private Collection<? extends IMessNode> childrenCollection;
    private IMessNode[] childrenArray;



    public ConstructionTreeNode(final ConstructionTreeNode parent, final IMessNode node) {
        this.parent = parent;

        this.vmNode = node;

        this.childrenCollection = node.getChildren();

        updateChildren();
    }

    private void updateChildren() {
        this.childrenArray = this.vmNode.getChildren();
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        if (this.childrenCollection == null) {
            return null;
        }

        if (childrenArray.length != childrenCollection.size()) {
            updateChildren();
        }

        return childrenArray[childIndex];
    }

    @Override
    public int getChildCount() {
        if (this.childrenCollection == null) {
            return 0;
        }

        if (childrenArray.length != childrenCollection.size()) {
            updateChildren();
        }

        return childrenArray.length;
    }

    @Override
    public TreeNode getParent() {
        return parent;
    }

    @Override
    public int getIndex(TreeNode node) {

        for (IMessNode iMessNode : childrenCollection) {

        }
        for (int i = 0; i < childrenCollection.size(); i++) {
            if (childrenArray[i] == node) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public boolean isLeaf() {
        return childrenCollection == null;
    }

    @Override
    public Enumeration<? extends TreeNode> children() {
        return new Enumeration<>() {
            private int index = 0;

            @Override
            public boolean hasMoreElements() {
                return this.index < ConstructionTreeNode.this.childrenArray.length;
            }

            @Override
            public TreeNode nextElement() {
                if (!hasMoreElements()) {
                    throw new NoSuchElementException();
                }
                return ConstructionTreeNode.this.childrenArray[this.index++];
            }
        };
    }
}
