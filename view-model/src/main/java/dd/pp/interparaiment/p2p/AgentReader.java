package dd.pp.interparaiment.p2p;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
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
    private volatile boolean targetTerminated;

    public AgentReader(final EventManager eventManager, final DataResolvingWorker dataResolver,
                       final InspectorConfiguration snapshot) throws IOException {
        this.eventManager = eventManager;
        this.dataResolver = dataResolver;
        this.serverSocket = new ServerSocket(snapshot.port(), 1, InetAddress.getByName(snapshot.host()));
        this.serverSocket.setSoTimeout(250);
        this.worker = new ReadingWorker(this::run, eventManager, this::close);
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
        Socket accepted = awaitConnection();
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
    }

    private Socket awaitConnection() throws IOException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (true) {
            try {
                return serverSocket.accept();
            } catch (SocketTimeoutException e) {
                if (targetTerminated) {
                    throw new IOException("The target exited before the agent connected");
                }
                if (System.nanoTime() >= deadline) {
                    throw new SocketTimeoutException("The agent did not connect within 30 seconds");
                }
            }
        }
    }

    public void targetTerminated() {
        targetTerminated = true;
    }

    public void run() throws IOException {
        if (in == null) {
            accept();
        }
        if (closed.get()) {
            return;
        }
        int length = in.readInt();
        if (length <= 0 || length > 50_000_000) {
            throw new IOException("Invalid frame length: " + length);
        }

        byte[] payload = new byte[length];
        in.readFully(payload);

        dataResolver.emmit(payload);
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
