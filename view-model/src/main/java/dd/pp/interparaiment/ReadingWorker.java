package dd.pp.interparaiment;

import java.util.concurrent.atomic.AtomicBoolean;

public class ReadingWorker {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread thread;
    private IReadingWork work;

    public ReadingWorker(final IReadingWork work) {
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
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void doOneStepBlocking() throws InterruptedException {
        Thread.sleep(50);
    }

    private void cleanup() {

    }

    public void close() {
        running.set(false);
        thread.interrupt();
    }

    public interface IReadingWork {
        void run() throws InterruptedException;
    }
}
