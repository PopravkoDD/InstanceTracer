package dd.pp.interparaiment.immodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import dd.pp.interparaiment.immodel.context.Path;
import dd.pp.interparaiment.immodel.context.RawPath;

public class MessModel implements IMessNode {
    private final Map<Integer, TracedTarget> targets = new LinkedHashMap<>();
    private ArrayList<IMessNode> indexedChildren;

    public void put(final Path path) {
        final TracedTarget tracedTarget = targets.get(path.target.hashCode());

        if (tracedTarget != null) {
            tracedTarget.put(path);
        } else {
            targets.put(path.target.hashCode(), new TracedTarget(path));

            // creationTreeBus.createNode(path)
        }
    }

    public void putRaw(final RawPath path) {
        final int targetNameHash = Arrays.hashCode(path.target);

        final TracedTarget tracedTarget = targets.get(targetNameHash);

        if (tracedTarget != null) {
            tracedTarget.putRaw(path);
        } else {
            targets.put(targetNameHash, new TracedTarget(path));
        }
    }

    @Override
    public ArrayList<IMessNode> getChildrenIndexed() {
        if (this.indexedChildren == null ||
                this.indexedChildren.size() != this.targets.size()) {
            this.indexedChildren = new ArrayList<>(this.targets.values());
        }

        return this.indexedChildren;
    }

    @Override
    public Collection<TracedTarget> getChildrenPure() {
        return this.targets.values();
    }
}
