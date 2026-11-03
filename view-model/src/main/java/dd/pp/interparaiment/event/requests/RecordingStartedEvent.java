package dd.pp.interparaiment.event.requests;

/** The inspection token routes this event only to its owning reader. */
public record RecordingStartedEvent(Object inspection) {}
