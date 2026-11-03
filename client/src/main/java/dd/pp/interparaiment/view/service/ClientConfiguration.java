package dd.pp.interparaiment.view.service;

/** Recording preferences; the standard IDE Run/Debug controls choose the executor. */
public record ClientConfiguration(boolean recordOnStart) {
    public ClientConfiguration() {
        this(false);
    }
}
