package dd.pp.interparaiment.immodel;

import java.util.ArrayList;

import dd.pp.interparaiment.immodel.context.Path;

public class CallingLine implements IMessNode {
    private final int line;

    private final ArrayList<TracedInstance> instances = new ArrayList<>();
    private final ArrayList<TracedInstance> freshMeat = new ArrayList<>();

    public CallingLine(final Path path) {
        this.line = path.line;
        this.instances.add(new TracedInstance(path.instanceHash));
        this.freshMeat.add(new TracedInstance(path.instanceHash));
    }

    public void put(final Path path) {
        this.instances.add(new TracedInstance(path.instanceHash));
        this.freshMeat.add(new TracedInstance(path.instanceHash));
        // here path can be passed to the PathPool, in the case of allocation overhead
    }

    public int getLine() {
        return line;
    }

    @Override
    public int getNodesCount() {
        return this.instances.size();
    }

    @Override
    public ArrayList<IMessNode> getFreshMeat() {
        final ArrayList<IMessNode> freshMeat = new ArrayList<>(this.freshMeat);
        this.freshMeat.clear();

        return freshMeat;
    }

    @Override
    public String getValue() {
//        return this.line + "(" + instances.size() + ")";
        return String.valueOf(this.line);
    }
}
