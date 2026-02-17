package dd.pp.interparaiment.view.communications.peers;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientReceivingPeer {
    private static final String host = "127.0.0.1";
    private static final int port = 35824;

    private final Socket socket;
    private final DataInputStream in;
    public ClientReceivingPeer() throws IOException {
        socket = new Socket();

        this.in = new DataInputStream(
                new BufferedInputStream(socket.getInputStream())
        );
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
            System.out.println(e.getMessage());
        }
    }

    private void handleFrame(byte type, byte[] payload) throws Exception {
        if (type == (byte) 1) {

        }
    }

    private void handleString(byte[] payload) {

    }

    private void handleJson(byte[] payload) throws Exception {
        String json = new String(payload, StandardCharsets.UTF_8);
        System.out.println("JSON: " + json);
    }

    private void handleBinary(byte[] payload) {
        System.out.println("Binary payload size = " + payload.length);
    }

    private void handleAck(byte[] payload) {
        System.out.println("ACK received");
    }
}
