package dd.pp.interparaiment.p2p;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

import dd.pp.interparaiment.InspectorViewModel;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;
import dd.pp.interparaiment.p2p.resolver.StringMessageResolver;

public class DataResolvingWorker extends Thread {
    private final InspectorViewModel viewModel;
    private int dropped = 0;
    private final ArrayBlockingQueue<byte[]> messageQueue = new ArrayBlockingQueue<>(100000);

    public DataResolvingWorker(final InspectorViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                byte[] payload = messageQueue.take();
                handleRaw(payload);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
        this.viewModel.getEventManager().notify(new ShowMessageInConsoleRequest("Got a message: " + message));

        this.viewModel.getMessModel().put(StringMessageResolver.resolve(message));
    }
    private void handleSingleRawMessage(byte[] payload) {
    }
    private void handleRawBulk(byte[] payload) {
    }

    public void emmit(final byte[] payload) {
        if (!messageQueue.offer(payload)) {
            dropped++;
        }
    }
}
