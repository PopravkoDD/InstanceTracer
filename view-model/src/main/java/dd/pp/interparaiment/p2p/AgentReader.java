package dd.pp.interparaiment.p2p;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import dd.pp.interparaiment.InspectorConfiguration;
import dd.pp.interparaiment.ReadingWorker;
import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;

public class AgentReader implements AutoCloseable {
    private final EventManager eventManager;
    private final ReadingWorker worker;
    private final ServerSocket serverSocket;
    private final DataResolvingWorker dataResolver;
    private Socket socket;
    private DataInputStream in;
    private final AtomicBoolean closed = new AtomicBoolean();
    private boolean started;
    private final Runnable onConnected;

    public AgentReader(final EventManager eventManager, final DataResolvingWorker dataResolver,
                       final InspectorConfiguration snapshot, Object inspection, boolean muted,
                       Runnable onConnected, Runnable onClosed) throws IOException {
        this.eventManager = eventManager;
        this.dataResolver = dataResolver;
        this.serverSocket = new ServerSocket(snapshot.port(), 1, InetAddress.getByName(snapshot.host()));
        this.onConnected = onConnected;
        this.worker = new ReadingWorker(this::run, eventManager, () -> {
            close();
            onClosed.run();
        }, inspection, muted);
    }

    public synchronized void open() throws IOException {
        if (closed.get()) {
            throw new IOException("Receiver is closed");
        }
        if (started) {
            return;
        }
        started = true;
        worker.start();
    }

    private void accept() throws IOException {
        this.eventManager.notify(new ShowMessageInConsoleRequest("Opening connection..."));
        Socket accepted = serverSocket.accept();
        synchronized (this) {
            if (closed.get()) {
                accepted.close();
                return;
            }
            this.socket = accepted;
        }
        this.eventManager.notify(new ShowMessageInConsoleRequest("Connection opened, starting reading worker!"));

        this.socket.setTcpNoDelay(true);
        this.socket.setKeepAlive(true);

        this.in = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
        onConnected.run();
    }

    public void run() throws IOException {
        if (in == null) {
            accept();
        }
        if (closed.get()) {
            return;
        }
        int firstHeaderByte = in.read();
        if (firstHeaderByte < 0) {
            throw new EOFException();
        }
        try (DataResolvingWorker.Admission admission = dataResolver.beginAdmission(worker.isReadingMuted())) {
            // Admission covers the remaining header, payload I/O, enqueueing and decoding.
            // Waiting on an idle stream before its first byte must not prevent StopRecording.
            int length = (firstHeaderByte << 24) | (in.readUnsignedByte() << 16)
                    | (in.readUnsignedByte() << 8) | in.readUnsignedByte();
            if (length <= 0 || length > 50_000_000) {
                throw new IOException("Invalid frame length: " + length);
            }
            if (admission == null) {
                // Muting never leaves a payload in the TCP stream or allocates/deserializes it.
                in.skipNBytes(length);
            } else {
                byte[] payload = new byte[length];
                in.readFully(payload);
                admission.emit(payload);
            }
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        worker.close();
        try {
            serverSocket.close();
        } catch (IOException e) {
            eventManager.notify(new ShowMessageInConsoleRequest("Cannot close listener: " + e.getMessage()));
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            eventManager.notify(new ShowMessageInConsoleRequest("Cannot close connection: " + e.getMessage()));
        }
    }
}
