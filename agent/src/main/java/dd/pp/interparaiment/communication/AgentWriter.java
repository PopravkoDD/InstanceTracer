package dd.pp.interparaiment.communication;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class AgentWriter {
    private static final String host = "127.0.0.1";
    private static final int port = 35824;
    private static AgentWriter instance;

    private Socket socket;
    private DataOutputStream out;
    private final AtomicBoolean open = new AtomicBoolean(false);

    private AgentWriter() throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        init();
    }

    public static AgentWriter getInstance() throws IOException {
        if (instance == null) {
            instance = new AgentWriter();
        }

        return instance;
    }

    private void init() throws IOException {
        if (this.open.get()) {
            return;
        }

        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(host, port), 3000);

        this.socket.setTcpNoDelay(true);
        this.socket.setKeepAlive(true);

        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.open.set(true);
    }

    public void sendMessage(byte type, byte[] payload) throws IOException {
        ensureOpen();

        this.out.writeInt(1 + payload.length);
        this.out.writeByte(type);
        this.out.write(payload);

        this.out.flush();
    }

    public void sendText(byte type, String text) throws IOException {
        sendMessage(type, text.getBytes(StandardCharsets.UTF_8));
    }

    private void ensureOpen() throws IOException {
        if (!open.get() || this.socket == null || this.socket.isClosed()) {
            throw new IOException("Channel is not initialized or already closed");
        }
    }

    public void close() {
        this.open.set(false);
        try {
            if (this.out != null) {
                this.out.flush();
            }
        } catch (IOException ignored) {
        }

        try {
            if (this.out != null) {
                this.out.close();
            }
        } catch (IOException ignored) {
        }

        try {
            if (this.socket != null) {
                this.socket.close();
            }
        } catch (IOException ignored) {
        }
        this.out = null;
        this.socket = null;
    }
}
