package dd.pp.interparaiment.p2p;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;

import dd.pp.interparaiment.InspectorViewModel;
import dd.pp.interparaiment.event.requests.InstanceCreatedEvent;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;
import dd.pp.interparaiment.immodel.context.Path;
import dd.pp.interparaiment.p2p.resolver.StringMessageResolver;

public class DataResolvingWorker extends Thread {
    private final InspectorViewModel viewModel;
    public enum RecordingState { PAUSED, RECORDING, DRAINING, CLOSED }

    private final ArrayBlockingQueue<byte[]> messageQueue = new ArrayBlockingQueue<>(100000);
    private final Object lifecycle = new Object();
    private RecordingState state = RecordingState.PAUSED;
    private boolean threadStarted;
    private boolean shuttingDown;
    private int admissions;
    private int pending; // queued plus currently processing
    private long dropped;
    private CompletableFuture<Void> drained = CompletableFuture.completedFuture(null);
    private final CompletableFuture<Void> terminated = new CompletableFuture<>();

    public DataResolvingWorker(final InspectorViewModel viewModel) {
        this.viewModel = viewModel;
        setName("InstanceTracer data resolver");
        setDaemon(true);
    }

    public RecordingState getRecordingState() {
        synchronized (lifecycle) {
            return state;
        }
    }

    public void resumeProcessing() {
        synchronized (lifecycle) {
            if (shuttingDown || state == RecordingState.CLOSED) {
                throw new IllegalStateException("The inspection session has ended");
            }
            if (state == RecordingState.DRAINING) {
                throw new IllegalStateException("Recording is still draining; wait before resuming");
            }
            state = RecordingState.RECORDING;
            if (!threadStarted) {
                threadStarted = true;
                start();
            }
            lifecycle.notifyAll();
        }
    }

    /** Admission begins at the first header byte; the rest of the frame is accounted for. */
    public Admission beginAdmission(boolean readingMuted) {
        synchronized (lifecycle) {
            if (readingMuted || shuttingDown || state != RecordingState.RECORDING) {
                return null;
            }
            admissions++;
            return new Admission();
        }
    }

    /** Close admission synchronously; completion includes reads and active processing. */
    public CompletableFuture<Void> stopAndDrain() {
        CompletableFuture<Void> result;
        synchronized (lifecycle) {
            if (state == RecordingState.RECORDING) {
                state = RecordingState.DRAINING;
                drained = new CompletableFuture<>();
            }
            result = drained;
            lifecycle.notifyAll();
        }
        completeDrainIfIdle();
        return result;
    }

    public CompletableFuture<Void> shutdownAfterDraining() {
        synchronized (lifecycle) {
            shuttingDown = true;
        }
        stopAndDrain();
        boolean neverStarted;
        synchronized (lifecycle) {
            neverStarted = !threadStarted;
            if (!threadStarted) {
                state = RecordingState.CLOSED;
            }
            lifecycle.notifyAll();
        }
        if (neverStarted) {
            terminated.complete(null);
        }
        return terminated;
    }

    @Override
    public void run() {
        try {
            while (true) {
                byte[] payload;
                synchronized (lifecycle) {
                    while (messageQueue.isEmpty()) {
                        if (shuttingDown && admissions == 0) {
                            return;
                        }
                        lifecycle.wait();
                    }
                    payload = messageQueue.remove();
                }
                try {
                    handleRaw(payload);
                } catch (RuntimeException error) {
                    fail(error);
                    return;
                } finally {
                    synchronized (lifecycle) {
                        pending--;
                    }
                    completeDrainIfIdle();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        } catch (RuntimeException e) {
            fail(e);
        } finally {
            CompletableFuture<Void> completion;
            synchronized (lifecycle) {
                state = RecordingState.CLOSED;
                completion = drained;
            }
            // The closing thread may reach here before an admission's idle-completion callback.
            // Normal exit requires no admissions/pending work; failures already completed this exceptionally.
            completion.complete(null);
            terminated.complete(null);
        }
    }

    private void fail(Exception error) {
        CompletableFuture<Void> drain;
        synchronized (lifecycle) {
            shuttingDown = true;
            state = RecordingState.CLOSED;
            drain = drained;
        }
        drain.completeExceptionally(error);
        terminated.completeExceptionally(error);
        viewModel.getEventManager().notify(new ShowMessageInConsoleRequest("Data processing failed: " + error.getMessage()));
    }

    private void completeDrainIfIdle() {
        CompletableFuture<Void> completion = null;
        synchronized (lifecycle) {
            if (state == RecordingState.DRAINING && admissions == 0 && pending == 0) {
                state = RecordingState.PAUSED;
                completion = drained;
            }
            lifecycle.notifyAll();
        }
        // Completion handlers may call ViewModel; do not invoke them under the worker monitor.
        if (completion != null) {
            completion.complete(null);
        }
    }

    /** One already-admitted frame may finish enqueueing after StopRecording. */
    public final class Admission implements AutoCloseable {
        private boolean closed;
        private boolean emitted;

        private Admission() {}

        public void emit(byte[] payload) {
            boolean overflow = false;
            synchronized (lifecycle) {
                if (closed || emitted) {
                    throw new IllegalStateException("Frame admission has already been consumed");
                }
                emitted = true;
                if (messageQueue.offer(payload)) {
                    pending++;
                    lifecycle.notifyAll();
                } else {
                    dropped++;
                    overflow = dropped == 1;
                }
            }
            // Preserve the existing bounded-queue overflow policy, but do not hide overload.
            if (overflow) {
                viewModel.getEventManager().notify(new ShowMessageInConsoleRequest(
                        "Recording queue is full; frames are being dropped. Stop the target or reduce the trace targets."));
            }
        }

        @Override
        public void close() {
            synchronized (lifecycle) {
                if (closed) {
                    return;
                }
                closed = true;
                admissions--;
                lifecycle.notifyAll();
            }
            completeDrainIfIdle();
        }
    }

    private void handleRaw(final byte[] payload) {
        final byte type = payload[0];

        final byte[] purePayload = Arrays.copyOfRange(payload, 1, payload.length);

        switch (type) {
            case 1 -> handleString(purePayload);
            case 2 -> handleSingleRawMessage(purePayload);
            case 3 -> handleRawBulk(purePayload);
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        }
    }
    private void handleString(byte[] payload) {
        final String message = new String(payload, StandardCharsets.UTF_8);
        final Path path = StringMessageResolver.resolve(message);
        this.viewModel.getMessModel().put(path);
        this.viewModel.getEventManager().notify(new InstanceCreatedEvent(path));
    }
    private void handleSingleRawMessage(byte[] payload) {
    }
    private void handleRawBulk(byte[] payload) {
    }

}
