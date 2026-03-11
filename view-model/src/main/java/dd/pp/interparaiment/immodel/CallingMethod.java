package dd.pp.interparaiment.immodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import dd.pp.interparaiment.immodel.context.Path;
import dd.pp.interparaiment.immodel.context.RawPath;

public class CallingMethod implements IMessNode {
    private String name;
    private byte[] rawName;

    private final Map<Integer, CallingLine> callingLines = new HashMap<>();
    private ArrayList<IMessNode> indexedChildren;


    public CallingMethod(final Path path) {
        this.name = path.method;
        this.callingLines.put(path.line, new CallingLine(path));
    }

    public CallingMethod(final RawPath path) {
        this.rawName = path.method;
        this.callingLines.put(path.line, new CallingLine(path));
    }

    public void put(final Path path) {
        final CallingLine callingLine = callingLines.get(path.line);

        if (callingLine != null) {
            callingLine.put(path);
        } else {
            callingLines.put(path.line, new CallingLine(path));
        }
    }

    public void putRaw(final RawPath path) {
        final CallingLine callingLine = callingLines.get(path.line);

        if (callingLine != null) {
            callingLine.putRaw(path);
        } else {
            callingLines.put(path.line, new CallingLine(path));
        }
    }

    @Override
    public ArrayList<IMessNode> getChildrenIndexed() {
        if (this.indexedChildren == null ||
                this.indexedChildren.size() != this.callingLines.size()) {
            this.indexedChildren = new ArrayList<>(this.callingLines.values());
        }

        return this.indexedChildren;
    }

    @Override
    public Collection<CallingLine> getChildrenPure() {
        return this.callingLines.values();
    }
}
