package dd.pp.interparaiment;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;

public class ReadingWorker extends Thread {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final EventManager eventManager;
    private IReadingWork work;
    private final Runnable onClose;

    public ReadingWorker(final IReadingWork work, final EventManager eventManager) {
        this(work, eventManager, () -> {});
    }

    public ReadingWorker(final IReadingWork work, final EventManager eventManager, final Runnable onClose) {
        this.eventManager = eventManager;
        this.work = work;
        this.onClose = onClose;
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
        interrupt();
    }

    public interface IReadingWork {
        void run() throws IOException, InterruptedException;
    }
}
