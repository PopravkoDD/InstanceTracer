package dd.pp.interparaiment.immodel;

import java.util.LinkedList;
import java.util.List;

import dd.pp.interparaiment.immodel.context.Path;
import dd.pp.interparaiment.immodel.context.RawPath;

public class CallingLine {
    private final int line;

    private final List<TracedInstance> instances = new LinkedList<>();

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

    public List<TracedInstance> getInstances() {
        return instances;
    }
}
