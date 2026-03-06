package dd.pp.interparaiment.immodel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import dd.pp.interparaiment.decoder.StringDecoder;
import dd.pp.interparaiment.immodel.context.Path;
import dd.pp.interparaiment.immodel.context.RawPath;

public class TracedTarget {
    private String name;
    private byte[] rawName;

    private final Map<Integer, CallingClass> callerClasses = new HashMap<>();

    public TracedTarget(final Path path) {
        this.name = path.target;
        this.callerClasses.put(path.caller.hashCode(), new CallingClass(path));
    }

    public TracedTarget(final RawPath path) {
        this.rawName = path.target;
        this.callerClasses.put(Arrays.hashCode(path.caller), new CallingClass(path));
    }

    public void put(final Path path) {
        final int key = path.caller.hashCode();

        final CallingClass callerClass = callerClasses.get(key);

        if (callerClass != null) {
            callerClass.put(path);
        } else {
            callerClasses.put(key, new CallingClass(path));
        }
    }

    public void putRaw(final RawPath path) {
        final Integer key = Arrays.hashCode(path.caller);

        final CallingClass callerClass = callerClasses.get(key);

        if (callerClass != null) {
            callerClass.putRaw(path);
        } else {
            callerClasses.put(key, new CallingClass(path));
        }
    }

    public String getName() {
        if (this.name == null) {
            this.name = StringDecoder.decodeUtf8(this.rawName);
        }

        return name;
    }
}
