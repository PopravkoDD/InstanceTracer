package dd.pp.interparaiment.view.model.construction;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

import dd.pp.interparaiment.immodel.CallingLine;
import dd.pp.interparaiment.immodel.IMessNode;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

public class ConstructionTreeNode implements TreeNode {
    private final IMessNode vmNode;

    private final boolean isLeaf;
    private final ConstructionTreeNode parent;
    private final List<ConstructionTreeNode> children = new ArrayList<>(100);
    public ConstructionTreeNode(final ConstructionTreeNode parent, final IMessNode node) {
        this.parent = parent;
        this.vmNode = node;

        this.isLeaf = node instanceof CallingLine;

//        syncModels();
    }

    public int[] syncModels() {
        final ArrayList<IMessNode> freshMeat = this.vmNode.getFreshMeat();

        if (freshMeat != null && !freshMeat.isEmpty()) {
            final int[] indexes = new int[freshMeat.size()];

            for (int i = 0; i < freshMeat.size(); i++) {
                IMessNode iMessNode = freshMeat.get(i);
                this.children.add(new ConstructionTreeNode(this, iMessNode));
                indexes[i] = this.children.size() - 1;
            }

            return indexes;
        }

        return null;
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        return this.children.get(childIndex);
    }

    @Override
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public TreeNode getParent() {
        return parent;
    }

    @Override
    public int getIndex(TreeNode node) {
        for (int i = 0; i < this.children.size(); i++) {
            if (this.children.get(i) == node) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public boolean getAllowsChildren() {
        return !this.isLeaf;
    }

    @Override
    public boolean isLeaf() {
        return this.isLeaf;
    }

    @Override
    public Enumeration<? extends TreeNode> children() {
        return new Enumeration<>() {
            private int index = 0;

            @Override
            public boolean hasMoreElements() {
                return this.index < ConstructionTreeNode.this.children.size();
            }

            @Override
            public TreeNode nextElement() {
                if (!hasMoreElements()) {
                    throw new NoSuchElementException();
                }
                return ConstructionTreeNode.this.children.get(this.index++);
            }
        };
    }

    public List<ConstructionTreeNode> getChildren() {
        return this.children;
    }

    @Override
    public String toString() {
        return this.vmNode.getValue() + "(" + this.getLeafsCount() + ")";
    }

    public IMessNode getVmNode() {
        return vmNode;
    }

    public int getLeafsCount() {
        if (this.isLeaf) {
            return vmNode.getNodesCount();
        }

        int count = 0;

        for (ConstructionTreeNode child : this.children) {
            count += child.getLeafsCount();
        }

        return count;
    }
}
