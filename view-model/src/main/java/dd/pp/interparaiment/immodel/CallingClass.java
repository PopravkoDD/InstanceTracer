package dd.pp.interparaiment.immodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import dd.pp.interparaiment.immodel.context.Path;

public class CallingClass implements IMessNode {
    private String name;

    private final Map<Integer, CallingMethod> callingMethods = new HashMap<>();
    private ArrayList<IMessNode> freshMeat;

    public CallingClass(final Path path) {
        this.name = path.caller;
        this.callingMethods.put(path.method.hashCode(), new CallingMethod(path));
    }

    public void put(final Path path) {
        final Integer key = path.method.hashCode();
        final CallingMethod callingMethod = this.callingMethods.get(key);

        if (callingMethod != null) {
            callingMethod.put(path);
        } else {
            final CallingMethod newChild = new CallingMethod(path);
            this.callingMethods.put(key, newChild);
            this.freshMeat.add(newChild);
        }
    }

    @Override
    public ArrayList<IMessNode> getFreshMeat() {
        final ArrayList<IMessNode> freshMeat = new ArrayList<>(this.freshMeat);
        this.freshMeat.clear();

        return freshMeat;
    }
}
