package dd.pp.interparaiment.event.requests;

import dd.pp.interparaiment.event.DataEvent;
import dd.pp.interparaiment.immodel.context.Path;

public class InstanceCreatedEvent extends DataEvent<Path> {
    public InstanceCreatedEvent(Path path) {
        super(path);
    }
}
