package dd.pp.interparaiment.peer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import dd.pp.interparaiment.ReadingWorker;
import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;

public class AgentReader {
    private static final String host = "127.0.0.1";
    private static final int port = 35824;

    private final EventManager eventManager;
    private final ReadingWorker worker;
    private final ServerSocket serverSocket;
    private Socket socket;
    private DataInputStream in;

    public AgentReader(final EventManager eventManager) throws IOException {
        this.eventManager = eventManager;
        this.serverSocket = new ServerSocket(port, 1, InetAddress.getByName(host));
        this.serverSocket.setSoTimeout(10000);
        this.worker = new ReadingWorker(this::run, eventManager);
    }

    public void open() throws IOException {
        this.eventManager.notify(new ShowMessageInConsoleRequest("Opening connection..."));
        this.socket = this.serverSocket.accept();
        this.eventManager.notify(new ShowMessageInConsoleRequest("Connection opened, starting reading worker!"));

        this.socket.setTcpNoDelay(true);
        this.socket.setKeepAlive(true);

        this.in = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));

        this.worker.start();
    }

    public void run() throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > 50_000_000) {
            throw new IOException("Invalid frame length: " + length);
        }

        byte type = in.readByte();
        eventManager.notify(new ShowMessageInConsoleRequest("Got a message, truing to read"));

        int payloadLength = length - 1;
        byte[] payload = new byte[payloadLength];
        in.readFully(payload);

        handleFrame(type, payload);
    }

    private void handleFrame(byte type, byte[] payload) {
        switch (type) {
            case 1 -> handleString(payload);
            case 2 -> handleBinary(payload);
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    private void handleBinary(byte[] payload) {
        this.eventManager.notify(new ShowMessageInConsoleRequest("Binary payload size = " + payload.length));
    }
    private void handleString(byte[] payload) {
        this.eventManager.notify(new ShowMessageInConsoleRequest("Binary payload size = " + payload.length));
    }
}
