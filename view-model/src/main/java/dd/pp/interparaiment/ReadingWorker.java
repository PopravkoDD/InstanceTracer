package dd.pp.interparaiment;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;

public class ReadingWorker {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread thread;
    private final EventManager eventManager;
    private IReadingWork work;

    public ReadingWorker(final IReadingWork work, final EventManager eventManager) {
        this.eventManager = eventManager;
        this.work = work;
        thread = new Thread(this::runLoop, "reading-worker");
    }

    public void start() {
        thread.start();
    }

    private void runLoop() {
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
            eventManager.notify(new ShowMessageInConsoleRequest("PEPE WATAFA: " + t.getMessage()));
        } finally {
            cleanup();
        }
    }

    private void cleanup() {

    }

    public void close() {
        running.set(false);
        thread.interrupt();
    }

    public interface IReadingWork {
        void run() throws IOException, InterruptedException;
    }
}
