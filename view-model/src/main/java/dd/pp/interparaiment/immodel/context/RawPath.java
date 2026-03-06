package dd.pp.interparaiment.immodel.context;

public class RawPath {
    public final byte[] target;
    public final byte[] caller;
    public final byte[] method;
    public final int line;
    public final int instanceHash;

    public RawPath(final byte[] target, final byte[] caller, final byte[] method, final int line, final int instanceHash) {
        this.target = target;
        this.caller = caller;
        this.method = method;
        this.line = line;
        this.instanceHash = instanceHash;
    }
}
