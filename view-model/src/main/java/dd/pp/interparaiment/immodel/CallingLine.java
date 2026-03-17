package dd.pp.interparaiment.immodel;

import java.util.ArrayList;

import dd.pp.interparaiment.immodel.context.Path;
import dd.pp.interparaiment.immodel.context.RawPath;

public class CallingLine implements IMessNode {
    private final int line;

    private final ArrayList<Integer> instances = new ArrayList<>();

    public CallingLine(final Path path) {
        this.line = path.line;
        this.instances.add(path.instanceHash);
    }
    public CallingLine(final RawPath path) {
        this.line = path.line;
        this.instances.add(path.instanceHash);
    }

    public void put(final Path path) {
        instances.add(path.instanceHash);
        // here path can be passed to the PathPool, in the case of allocation overhead
    }

    public int getLine() {
        return line;
    }

    @Override
    public ArrayList<IMessNode> getFreshMeat() {
        return null;
    }

    @Override
    public String getValue() {
        return this.line + "(" + instances.size() + ")";
    }
}
