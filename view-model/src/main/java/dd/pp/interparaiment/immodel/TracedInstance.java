package dd.pp.interparaiment.immodel;

import java.util.ArrayList;
import java.util.Collection;

public class TracedInstance implements IMessNode {

    private final int instanceHash;

    public TracedInstance(final int instanceHash) {
        this.instanceHash = instanceHash;
    }

    public int getInstanceHash() {
        return instanceHash;
    }

    @Override
    public ArrayList<IMessNode> getChildrenIndexed() {
        return null;
    }

    @Override
    public Collection<? extends IMessNode> getChildrenPure() {
        return null;
    }
}
