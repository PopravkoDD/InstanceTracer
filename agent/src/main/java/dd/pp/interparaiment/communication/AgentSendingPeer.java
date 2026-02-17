package dd.pp.interparaiment.communication;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class AgentSendingPeer {
    private static final String host = "127.0.0.1";
    private static final int port = 35824;
    private static AgentSendingPeer instance;

    private Socket socket;
    private DataOutputStream out;
    private final AtomicBoolean open = new AtomicBoolean(false);

    private AgentSendingPeer() throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        init();
    }

    public static AgentSendingPeer getInstance() throws IOException {
        if (instance == null) {
            instance = new AgentSendingPeer();
        }

        return instance;
    }

    private void init() throws IOException {
        if (open.get()) {
            return;
        }

        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 3000);

        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);

        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        open.set(true);
    }

    public void sendMessage(byte type, byte[] payload) throws IOException {
        ensureOpen();

        out.writeInt(1 + payload.length);
        out.writeByte(type);
        out.write(payload);

        out.flush();
    }

    public void sendText(byte type, String text) throws IOException {
        sendMessage(type, text.getBytes(StandardCharsets.UTF_8));
    }

    private void ensureOpen() throws IOException {
        if (!open.get() || socket == null || socket.isClosed()) {
            throw new IOException("Channel is not initialized or already closed");
        }
    }

    public void close() {
        open.set(false);
        try {
            if (out != null) {
                out.flush();
            }
        } catch (IOException ignored) {
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException ignored) {
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        out = null;
        socket = null;
    }
}
