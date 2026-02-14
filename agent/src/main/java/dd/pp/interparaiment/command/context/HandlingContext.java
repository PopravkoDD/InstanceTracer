package dd.pp.interparaiment.command.context;

import javafx.event.Event;

public class HandlingContext {
    private final Event event;

    public Event getEvent() {
        return event;
    }

    public HandlingContext(final Event event) {
        this.event = event;
    }

    public final StringBuilder pathLogResult = new StringBuilder();

}
