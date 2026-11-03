package dd.pp.interparaiment.p2p;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import dd.pp.interparaiment.ReadingWorker;
import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;

public class AgentReader implements AutoCloseable {
    private final EventManager eventManager;
    private final ReadingWorker worker;
    private final ServerSocket serverSocket;
    private final DataResolvingWorker dataResolver;
    private final Object admissionLock = new Object();
    private final byte[] ignoredPayload = new byte[8192];
    private volatile Socket socket;
    private volatile boolean closed;
    private volatile long recordingGeneration;
    private DataInputStream in;

    public AgentReader(EventManager eventManager, DataResolvingWorker dataResolver, ServerSocket serverSocket,
                       boolean recordOnStart, Runnable onClosed) {
        this.eventManager = eventManager;
        this.dataResolver = dataResolver;
        this.serverSocket = serverSocket;
        this.worker = new ReadingWorker(this::readFrame, eventManager, recordOnStart, onClosed);
    }

    /** The listener is already bound; accepting and reading both happen on this worker. */
    public void open() {
        worker.start();
    }

    public boolean isReadingMuted() {
        return worker.isReadingMuted();
    }

    private void acceptConnection() throws IOException {
        eventManager.notify(new ShowMessageInConsoleRequest("Waiting for the inspection agent..."));
        Socket accepted = serverSocket.accept();
        synchronized (this) {
            if (closed) {
                accepted.close();
                throw new IOException("Inspection was closed before the agent connected");
            }
            socket = accepted;
        }
        accepted.setTcpNoDelay(true);
        accepted.setKeepAlive(true);
        in = new DataInputStream(new BufferedInputStream(accepted.getInputStream()));
        eventManager.notify(new ShowMessageInConsoleRequest("Agent connected"));
    }

    private void readFrame() throws IOException {
        if (in == null) {
            acceptConnection();
        }
        int length = in.readInt();
        if (length <= 0 || length > 50_000_000) {
            throw new IOException("Invalid frame length: " + length);
        }
        long generation = recordingGeneration;
        if (worker.isReadingMuted()) {
            consumeIgnoredFrame(length);
            return;
        }
        byte[] payload = new byte[length];
        in.readFully(payload);
        synchronized (admissionLock) {
            if (!worker.isReadingMuted() && !closed && generation == recordingGeneration) {
                dataResolver.emmit(payload);
            }
        }
    }

    private void consumeIgnoredFrame(int length) throws IOException {
        int remaining = length;
        while (remaining > 0) {
            int count = Math.min(remaining, ignoredPayload.length);
            in.readFully(ignoredPayload, 0, count);
            remaining -= count;
        }
    }

    /** Called after the mute event, outside its subscriber, to finish any admitted enqueue. */
    public void finishRecordingAdmission() {
        synchronized (admissionLock) {
            // Invalidate a partial pre-stop frame even if recording resumes before it completes.
            recordingGeneration++;
        }
    }

    public void awaitTermination() throws InterruptedException {
        if (Thread.currentThread() != worker && worker.getState() != Thread.State.NEW) {
            worker.join();
        }
    }

    @Override
    public void close() {
        worker.close();
        Socket connection;
        synchronized (this) {
            closed = true;
            connection = socket;
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException exception) {
                eventManager.notify(new ShowMessageInConsoleRequest("Closing agent socket: " + exception.getMessage()));
            }
        }
    }
}
