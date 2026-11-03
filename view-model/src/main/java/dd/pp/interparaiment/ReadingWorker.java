package dd.pp.interparaiment;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.requests.RecordingStartedEvent;
import dd.pp.interparaiment.event.requests.RecordingStoppedEvent;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;

public class ReadingWorker extends Thread {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final EventManager eventManager;
    private final IReadingWork work;
    private final Runnable onClosed;
    private final EventManager.Subscription startedSubscription;
    private final EventManager.Subscription stoppedSubscription;
    private volatile boolean isReadingMuted;

    public ReadingWorker(IReadingWork work, EventManager eventManager, boolean recordOnStart, Runnable onClosed) {
        super("InstanceTracer reader");
        setDaemon(true);
        this.eventManager = eventManager;
        this.work = work;
        this.onClosed = onClosed;
        this.isReadingMuted = !recordOnStart;
        this.startedSubscription = eventManager.subscribe(RecordingStartedEvent.class,
                event -> isReadingMuted = false);
        this.stoppedSubscription = eventManager.subscribe(RecordingStoppedEvent.class,
                event -> isReadingMuted = true);
    }

    public boolean isReadingMuted() {
        return isReadingMuted;
    }

    @Override
    public void run() {
        try {
            while (running.get() && !isInterrupted()) {
                work.run();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (EOFException exception) {
            report("Socket closed by peer");
        } catch (Throwable exception) {
            report("Agent connection closed: " + exception.getMessage());
        } finally {
            running.set(false);
            startedSubscription.close();
            stoppedSubscription.close();
            onClosed.run();
        }
    }

    private void report(String message) {
        if (running.get()) {
            eventManager.notify(new ShowMessageInConsoleRequest(message));
        }
    }

    public void close() {
        running.set(false);
        startedSubscription.close();
        stoppedSubscription.close();
        if (Thread.currentThread() != this) {
            interrupt();
        }
    }

    public interface IReadingWork {
        void run() throws IOException, InterruptedException;
    }
}
