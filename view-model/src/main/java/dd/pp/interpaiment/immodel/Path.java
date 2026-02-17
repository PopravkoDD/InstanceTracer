package dd.pp.interpaiment.immodel;

public class Path {
    public final String target;
    public final String caller;
    public final String method;
    public final int line;
    public final int instanceHash;

    public Path(final String target, final String caller, final String method, final int line, final int instanceHash) {
        this.target = target;
        this.caller = caller;
        this.method = method;
        this.line = line;
        this.instanceHash = instanceHash;
    }
}
