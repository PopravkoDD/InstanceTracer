package dd.pp.interparaiment;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns the listener and workers reserved for exactly one target process. */
public final class PreparedInspection implements AutoCloseable {
    private final InspectorViewModel viewModel;
    private final InspectorConfiguration configuration;
    private final ServerSocket serverSocket;
    private final AtomicBoolean closed = new AtomicBoolean();

    PreparedInspection(InspectorViewModel viewModel, InspectorConfiguration configuration,
                       ServerSocket serverSocket) {
        this.viewModel = viewModel;
        this.configuration = configuration;
        this.serverSocket = serverSocket;
    }

    public InspectorViewModel getViewModel() {
        return viewModel;
    }

    public InspectorConfiguration getConfiguration() {
        return configuration;
    }

    public String getAgentArgs() {
        return viewModel.getAgentArgs();
    }

    public boolean isClosed() {
        return closed.get();
    }

    public boolean isRecording() {
        return !isClosed() && viewModel.isRecording();
    }

    void closeListener() throws IOException {
        serverSocket.close();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            viewModel.finishInspection(this);
        }
    }
}
