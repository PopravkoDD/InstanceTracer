package dd.pp.interparaiment.peer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class AgentReader {
    private static final String host = "127.0.0.1";
    private static final int port = 35824;
    private Socket socket;
    private DataInputStream in;

    public AgentReader() {

    }

    public void open() throws IOException {
        this.socket = new Socket();

        this.socket.connect(new InetSocketAddress(host, port), 3000);

        this.socket.setTcpNoDelay(true);
        this.socket.setKeepAlive(true);

        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    public void run() {
        try {
            while (true) {
                int length = in.readInt();
                if (length <= 0 || length > 50_000_000) {
                    throw new IOException("Invalid frame length: " + length);
                }

                byte type = in.readByte();

                int payloadLen = length - 1;
                byte[] payload = new byte[payloadLen];
                in.readFully(payload);

                handleFrame(type, payload);
            }
        } catch (EOFException eof) {
            System.out.println("Socket closed by peer");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleFrame(byte type, byte[] payload) throws Exception {
        switch (type) {
            case 1 -> handleString(payload);
            case 2 -> handleBinary(payload);
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    private void handleBinary(byte[] payload) {
        System.out.println("Binary payload size = " + payload.length);
    }
    private void handleString(byte[] payload) {
        System.out.println("Binary payload size = " + payload.length);
    }

    public void close() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ignored) {
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        in = null;
        socket = null;
    }
}
