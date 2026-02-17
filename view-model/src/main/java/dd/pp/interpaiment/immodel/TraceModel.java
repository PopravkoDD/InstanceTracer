package dd.pp.interpaiment.immodel;

import java.util.HashMap;
import java.util.Map;

public class TraceModel {
    private final Map<String, TracedTarget> targets = new HashMap<>();

    public void put(final Path path) {
        final TracedTarget tracedTarget = targets.get(path.target);

        if (tracedTarget != null) {
            tracedTarget.put(path);
        } else {
            targets.put(path.target, new TracedTarget(path));
        }
    }
}
