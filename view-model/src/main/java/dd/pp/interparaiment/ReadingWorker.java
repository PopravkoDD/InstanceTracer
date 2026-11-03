package dd.pp.interparaiment;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.IListener;
import dd.pp.interparaiment.event.requests.RecordingStartedEvent;
import dd.pp.interparaiment.event.requests.RecordingStoppedEvent;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;

public class ReadingWorker extends Thread {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final EventManager eventManager;
    private final IReadingWork work;
    private final Runnable onClose;
    private volatile boolean isReadingMuted;
    private final IListener<RecordingStartedEvent> onRecordingStarted;
    private final IListener<RecordingStoppedEvent> onRecordingStopped;

    public ReadingWorker(final IReadingWork work, final EventManager eventManager) {
        this(work, eventManager, () -> {}, null, false);
    }

    public ReadingWorker(final IReadingWork work, final EventManager eventManager, final Runnable onClose) {
        this(work, eventManager, onClose, null, false);
    }

    public ReadingWorker(IReadingWork work, EventManager eventManager, Runnable onClose,
                         Object inspection, boolean muted) {
        this.eventManager = eventManager;
        this.work = work;
        this.onClose = onClose;
        isReadingMuted = muted;
        onRecordingStarted = event -> {
            if (inspection != null && event.inspection() == inspection) {
                isReadingMuted = false;
            }
        };
        onRecordingStopped = event -> {
            if (inspection != null && event.inspection() == inspection) {
                isReadingMuted = true;
            }
        };
        eventManager.subscribe(RecordingStartedEvent.class, onRecordingStarted);
        eventManager.subscribe(RecordingStoppedEvent.class, onRecordingStopped);
        setName("InstanceTracer reader");
        setDaemon(true);
    }


    @Override
    public void run() {
        try {
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                work.run();
            }
        } catch (InterruptedException ie) {
            eventManager.notify(new ShowMessageInConsoleRequest("Interrupting reading worker: " + ie.getMessage()));
            Thread.currentThread().interrupt();
        } catch (EOFException eofException) {
            eventManager.notify(new ShowMessageInConsoleRequest("Socket closed by peer"));
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            if (running.get()) {
                eventManager.notify(new ShowMessageInConsoleRequest("Reader failed: " + t.getMessage()));
            }
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        onClose.run();
    }

    public void close() {
        running.set(false);
        eventManager.unsubscribe(RecordingStartedEvent.class, onRecordingStarted);
        eventManager.unsubscribe(RecordingStoppedEvent.class, onRecordingStopped);
        interrupt();
    }

    public boolean isReadingMuted() {
        return isReadingMuted;
    }

    public interface IReadingWork {
        void run() throws IOException, InterruptedException;
    }
}
