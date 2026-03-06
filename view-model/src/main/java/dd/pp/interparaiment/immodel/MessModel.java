package dd.pp.interparaiment.immodel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import dd.pp.interparaiment.immodel.context.Path;
import dd.pp.interparaiment.immodel.context.RawPath;

public class MessModel {
    private final Map<Integer, TracedTarget> targets = new HashMap<>();

    public void put(final Path path) {
        final TracedTarget tracedTarget = targets.get(path.target.hashCode());

        if (tracedTarget != null) {
            tracedTarget.put(path);
        } else {
            targets.put(path.target.hashCode(), new TracedTarget(path));
        }
    }

    public void putRaw(final RawPath path) {
        final int targetNameHash = Arrays.hashCode(path.target);

        final TracedTarget tracedTarget = targets.get(targetNameHash);

        if (tracedTarget != null) {
            tracedTarget.putRaw(path);
        } else {
            targets.put(targetNameHash, new TracedTarget(path));
        }
    }
}
