package dd.pp.interparaiment.event;

public interface IListener<TEventData> {
    void accept(TEventData data);
}
