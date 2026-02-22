package dd.pp.interparaiment.event;

public abstract class DataEvent<TData> {
    private final TData data;
    public DataEvent(final TData data) {
        this.data = data;
    }

    public TData getData() {
        return this.data;
    }
}
