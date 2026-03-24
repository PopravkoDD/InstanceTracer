package dd.pp.interparaiment.immodel;

import java.util.ArrayList;

public class TracedInstance implements IMessNode{

    private final int instanceHash;

    public TracedInstance(final int instanceHash) {
        this.instanceHash = instanceHash;
    }

    public int getInstanceHash() {
        return instanceHash;
    }

    @Override
    public int getNodesCount() {
        return 0;
    }

    @Override
    public ArrayList<IMessNode> getFreshMeat() {
        return null;
    }

    @Override
    public String getValue() {
        return String.valueOf(this.instanceHash);
    }
}
