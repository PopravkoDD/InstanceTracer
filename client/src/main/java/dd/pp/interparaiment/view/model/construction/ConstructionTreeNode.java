package dd.pp.interparaiment.view.model.construction;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

import dd.pp.interparaiment.immodel.CallingLine;
import dd.pp.interparaiment.immodel.IMessNode;
import javax.swing.tree.TreeNode;

public class ConstructionTreeNode implements TreeNode {
    private final IMessNode vmNode;
    private final boolean isLeaf;
    private ConstructionTreeNode parent;
    private final List<ConstructionTreeNode> children = new ArrayList<>(100);

    public ConstructionTreeNode(final ConstructionTreeNode parent, final IMessNode node) {
        this.parent = parent;
        this.vmNode = node;

        this.isLeaf = !(node instanceof CallingLine);

        syncModels();
    }

    public void syncModels() {
        this.children.addAll(this.vmNode.getFreshMeat().stream().map(iMessNode -> new ConstructionTreeNode(this, iMessNode)).toList());
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
}
