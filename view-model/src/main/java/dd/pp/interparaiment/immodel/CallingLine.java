package dd.pp.interparaiment.immodel;

import java.util.ArrayList;
import java.util.Collection;

import dd.pp.interparaiment.immodel.context.Path;
import dd.pp.interparaiment.immodel.context.RawPath;

public class CallingLine implements IMessNode {
    private final int line;

    private final ArrayList<TracedInstance> instances = new ArrayList<>();

    public CallingLine(final Path path) {
        this.line = path.line;
        this.instances.add(new TracedInstance(path.instanceHash));
    }
    public CallingLine(final RawPath path) {
        this.line = path.line;
        this.instances.add(new TracedInstance(path.instanceHash));
    }

    public void put(final Path path) {
        instances.add(new TracedInstance(path.instanceHash));
        // here path can be passed to the PathPool, in the case of allocation overhead
    }

    public void putRaw(final RawPath path) {
        instances.add(new TracedInstance(path.instanceHash));
        // here path can be passed to the PathPool, in the case of allocation overhead
    }

    public int getLine() {
        return line;
    }

    @Override
    public ArrayList<TracedInstance> getChildrenIndexed() {
        return this.instances;
    }

    @Override
    public Collection<TracedInstance> getChildrenPure() {
        return this.instances;
    }
}
