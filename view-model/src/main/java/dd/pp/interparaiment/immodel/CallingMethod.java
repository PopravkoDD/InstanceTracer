package dd.pp.interparaiment.immodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import dd.pp.interparaiment.immodel.context.Path;

public class CallingMethod implements IMessNode {
    private final String name;

    private final Map<Integer, CallingLine> callingLines = new HashMap<>();
    private final ArrayList<IMessNode> freshMeat = new ArrayList<>();


    public CallingMethod(final Path path) {
        this.name = path.method;
        final CallingLine callingLine = new CallingLine(path);
        this.callingLines.put(path.line, callingLine);
        this.freshMeat.add(callingLine);
    }

    public void put(final Path path) {
        final CallingLine callingLine = this.callingLines.get(path.line);

        if (callingLine != null) {
            callingLine.put(path);
        } else {
            final CallingLine newChild = new CallingLine(path);
            this.callingLines.put(path.line, newChild);
            this.freshMeat.add(newChild);
        }
    }

    @Override
    public int getNodesCount() {
        return this.callingLines.size();
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
