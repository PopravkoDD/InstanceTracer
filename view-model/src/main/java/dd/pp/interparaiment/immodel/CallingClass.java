package dd.pp.interparaiment.immodel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import dd.pp.interparaiment.decoder.StringDecoder;
import dd.pp.interparaiment.immodel.context.Path;
import dd.pp.interparaiment.immodel.context.RawPath;

public class CallingClass {
    private String name;
    private byte[] rawName;
    private final Map<Integer, CallingMethod> callingMethods = new HashMap<>();

    public CallingClass(final Path path) {
        this.name = path.caller;
        this.callingMethods.put(path.method.hashCode(), new CallingMethod(path));
    }

    public CallingClass(final RawPath path) {
        this.rawName = path.caller;
        this.callingMethods.put(Arrays.hashCode(path.method), new CallingMethod(path));
    }

    public void put(final Path path) {
        final Integer key = path.method.hashCode();
        final CallingMethod callingMethod = callingMethods.get(key);

        if (callingMethod != null) {
            callingMethod.put(path);
        } else {
            callingMethods.put(key, new CallingMethod(path));
        }
    }

    public void putRaw(final RawPath path) {
        final Integer key = Arrays.hashCode(path.method);

        final CallingMethod callingMethod = callingMethods.get(key);

        if (callingMethod != null) {
            callingMethod.putRaw(path);
        } else {
            callingMethods.put(key, new CallingMethod(path));
        }
    }

    public String getName() {
        if (this.name == null) {
            this.name = StringDecoder.decodeUtf8(this.rawName);
        }

        return name;
    }
}
