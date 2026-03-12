package dd.pp.interparaiment.immodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import dd.pp.interparaiment.immodel.context.Path;

public class MessModel implements IMessNode {
    private final Map<Integer, TracedTarget> targets = new LinkedHashMap<>();
    private ArrayList<IMessNode> freshMeat;

    public void put(final Path path) {
        final TracedTarget tracedTarget = this.targets.get(path.target.hashCode());

        if (tracedTarget != null) {
            tracedTarget.put(path);
        } else {
            final TracedTarget newChild = new TracedTarget(path);
            this.targets.put(path.target.hashCode(), newChild);
            this.freshMeat.add(newChild);
            // creationTreeBus.createNode(path)
        }
    }

    @Override
    public ArrayList<IMessNode> getFreshMeat() {
        final ArrayList<IMessNode> freshMeat = new ArrayList<>(this.freshMeat);
        this.freshMeat.clear();

        return freshMeat;
    }
}
