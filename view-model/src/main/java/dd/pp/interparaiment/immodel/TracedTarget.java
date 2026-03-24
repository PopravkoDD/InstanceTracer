package dd.pp.interparaiment.immodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import dd.pp.interparaiment.immodel.context.Path;

public class TracedTarget implements IMessNode {
    private final String name;
    private final Map<Integer, CallingClass> callerClasses = new LinkedHashMap<>();
    private final ArrayList<IMessNode> freshMeat = new ArrayList<>();

    public TracedTarget(final Path path) {
        this.name = path.target;
        final CallingClass callingClass = new CallingClass(path);
        this.callerClasses.put(path.caller.hashCode(), callingClass);
        this.freshMeat.add(callingClass);
    }

    public void put(final Path path) {
        final int key = path.caller.hashCode();

        final CallingClass callerClass = this.callerClasses.get(key);

        if (callerClass != null) {
            callerClass.put(path);
        } else {
            final CallingClass newChild = new CallingClass(path);
            this.callerClasses.put(key, newChild);
            this.freshMeat.add(newChild);
        }
    }

    @Override
    public int getNodesCount() {
        return this.callerClasses.size();
    }

    @Override
    public ArrayList<IMessNode> getFreshMeat() {
        final ArrayList<IMessNode> freshMeat = new ArrayList<>(this.freshMeat);
        this.freshMeat.clear();

        return freshMeat;
    }

    @Override
    public String getValue() {
        return this.name;
    }
}
