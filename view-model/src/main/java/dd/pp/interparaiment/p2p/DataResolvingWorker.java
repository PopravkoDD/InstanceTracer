package dd.pp.interparaiment.p2p;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

import dd.pp.interparaiment.InspectorViewModel;
import dd.pp.interparaiment.event.requests.InstanceCreatedEvent;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;
import dd.pp.interparaiment.immodel.context.Path;
import dd.pp.interparaiment.p2p.resolver.StringMessageResolver;

public class DataResolvingWorker extends Thread implements AutoCloseable {
    private final InspectorViewModel viewModel;
    private final ArrayBlockingQueue<byte[]> messageQueue = new ArrayBlockingQueue<>(100000);
    private final Object completionMonitor = new Object();
    private long accepted;
    private long completed;
    private long dropped;
    private volatile boolean closed;
    private volatile Throwable failure;

    public DataResolvingWorker(InspectorViewModel viewModel) {
        super("InstanceTracer data resolver");
        setDaemon(true);
        this.viewModel = viewModel;
    }

    public synchronized void startIfNeeded() {
        if (closed || failure != null) {
            throw new IllegalStateException("The inspection data resolver is closed");
        }
        if (getState() == State.NEW) {
            start();
        }
    }

    @Override
    public void run() {
        try {
            while (!closed && !isInterrupted()) {
                byte[] payload = messageQueue.take();
                try {
                    handleRaw(payload);
                } finally {
                    synchronized (completionMonitor) {
                        completed++;
                        completionMonitor.notifyAll();
                    }
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Throwable exception) {
            failure = exception;
            synchronized (completionMonitor) {
                completionMonitor.notifyAll();
            }
            try {
                viewModel.getEventManager().notify(new ShowMessageInConsoleRequest(
                        "Inspection data processing failed: " + exception.getMessage()));
            } finally {
                viewModel.close();
            }
        }
    }

    private void handleRaw(byte[] payload) {
        byte type = payload[0];
        byte[] purePayload = Arrays.copyOfRange(payload, 1, payload.length);
        switch (type) {
            case 1 -> handleString(purePayload);
            case 2 -> handleSingleRawMessage(purePayload);
            case 3 -> handleRawBulk(purePayload);
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    private void handleString(byte[] payload) {
        String message = new String(payload, StandardCharsets.UTF_8);
        Path path = StringMessageResolver.resolve(message);
        viewModel.getMessModel().put(path);
        viewModel.getEventManager().notify(new InstanceCreatedEvent(path));
    }

    private void handleSingleRawMessage(byte[] payload) {
    }

    private void handleRawBulk(byte[] payload) {
    }

    public void emmit(byte[] payload) {
        boolean reportDrop = false;
        synchronized (completionMonitor) {
            if (closed || failure != null) {
                return;
            }
            if (messageQueue.offer(payload)) {
                accepted++;
            } else {
                dropped++;
                reportDrop = dropped == 1;
            }
        }
        if (reportDrop) {
            viewModel.getEventManager().notify(new ShowMessageInConsoleRequest(
                    "Inspection queue is full; the existing overflow policy is dropping messages"));
        }
    }

    /** Waits for accepted work, including the current frame; it never pauses the consumer. */
    public void awaitIdle() throws InterruptedException {
        if (Thread.currentThread() == this) {
            return;
        }
        synchronized (completionMonitor) {
            while (completed < accepted && !closed && failure == null) {
                completionMonitor.wait();
            }
            if (failure != null) {
                throw new IllegalStateException("Inspection data processing failed", failure);
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        interrupt();
        synchronized (completionMonitor) {
            completionMonitor.notifyAll();
        }
        if (Thread.currentThread() != this && getState() != State.NEW) {
            try {
                join();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        int unfinished = messageQueue.size();
        messageQueue.clear();
        if (unfinished > 0) {
            viewModel.getEventManager().notify(new ShowMessageInConsoleRequest(
                    "Inspection closed with " + unfinished + " accepted messages unprocessed after interruption or failure"));
        }
        if (dropped > 0) {
            viewModel.getEventManager().notify(new ShowMessageInConsoleRequest(
                    "Inspection finished with " + dropped + " messages dropped by the existing queue overflow policy"));
        }
    }
}
