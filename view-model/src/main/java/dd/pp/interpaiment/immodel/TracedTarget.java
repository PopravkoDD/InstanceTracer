package dd.pp.interpaiment.immodel;

import java.util.HashMap;
import java.util.Map;

public class TracedTarget {
    private final String name;
    private final Map<String, CallingClass> callerClasses = new HashMap<>();

    public TracedTarget(final Path path) {
        this.name = path.target;
        this.callerClasses.put(path.caller, new CallingClass(path));
    }

    public void put(final Path path) {
        final CallingClass callerClass = callerClasses.get(path.caller);

        if (callerClass != null) {
            callerClass.put(path);
        } else {
            callerClasses.put(path.caller, new CallingClass(path));
        }
    }
}
