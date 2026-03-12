package dd.pp.interparaiment.immodel;

public class TracedInstance {

    private final int instanceHash;

    public TracedInstance(final int instanceHash) {
        this.instanceHash = instanceHash;
    }

    public int getInstanceHash() {
        return instanceHash;
    }
}
