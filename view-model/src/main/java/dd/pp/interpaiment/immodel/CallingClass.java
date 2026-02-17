package dd.pp.interpaiment.immodel;

import java.util.HashMap;
import java.util.Map;

public class CallingClass {
    private final String name;
    private final Map<String, CallingMethod> callingMethods = new HashMap<>();

    public CallingClass(final Path path) {
        this.name = path.caller;
        this.callingMethods.put(path.method, new CallingMethod(path));
    }

    public void put(final Path path) {
        final CallingMethod callingMethod = callingMethods.get(path.method);

        if (callingMethod != null) {
            callingMethod.put(path);
        } else {
            callingMethods.put(path.method, new CallingMethod(path));
        }
    }
}
