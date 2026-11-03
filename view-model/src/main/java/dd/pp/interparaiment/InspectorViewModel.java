package dd.pp.interparaiment;

import java.io.IOException;
import java.util.Objects;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.requests.AgentConfigurationChangedEvent;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;
import dd.pp.interparaiment.event.requests.StartInspectionRequest;
import dd.pp.interparaiment.event.requests.StopInspectionRequest;
import dd.pp.interparaiment.immodel.MessModel;
import dd.pp.interparaiment.p2p.AgentReader;
import dd.pp.interparaiment.p2p.DataResolvingWorker;

public class InspectorViewModel implements AutoCloseable {
    private final EventManager eventManager = new EventManager();
    private final MessModel messModel = new MessModel();
    private InspectorConfiguration configuration;
    private PreparedInspection inspection;
    private boolean preparing;
    private boolean closed;
    private long preparationVersion;

    public InspectorViewModel(InspectorConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        eventManager.subscribe(StartInspectionRequest.class, request -> {
            PreparedInspection requested = request.getInspection();
            if (requested != null) {
                startReading(requested);
            }
        });
        eventManager.subscribe(StopInspectionRequest.class, request -> {
            PreparedInspection active = getActiveInspection();
            if (active != null) {
                active.close();
            }
        });
    }

    private void startReading(PreparedInspection requested) {
        synchronized (this) {
            if (inspection != requested || closed) {
                throw new IllegalStateException("This inspection no longer owns the receiver");
            }
        }
        try {
            requested.reader.open();
        } catch (IOException e) {
            requested.close();
            eventManager.notify(new ShowMessageInConsoleRequest("Connection failed: " + e.getMessage()));
        }
    }

    public synchronized InspectorConfiguration getConfiguration() {
        return configuration;
    }

    public synchronized void updateOptions(InspectorConfiguration options) {
        Objects.requireNonNull(options, "options");
        if (!configuration.equals(options)) {
            configuration = options;
            eventManager.notify(new AgentConfigurationChangedEvent(options));
        }
    }

    public synchronized void updateTraceTargets(List<String> targets) {
        updateOptions(configuration.withTraceTargets(targets));
    }

    /** Complete JVM option, kept independent of the IntelliJ execution adapters. */
    public synchronized String getAgentArgs() {
        return getAgentArgs(configuration);
    }

    public String getAgentArgs(InspectorConfiguration snapshot) {
        if (snapshot.traceTargets().isEmpty()) {
            throw new IllegalStateException("Configure at least one trace target before launching MessInspection");
        }
        StringBuilder args = new StringBuilder("-javaagent:").append(snapshot.agentPath())
                .append("=BridgePath=").append(snapshot.bridgePath());
        for (String target : snapshot.traceTargets()) {
            args.append(",TraceTarget=").append(target);
        }
        return args.append(",host=").append(snapshot.host())
                .append(",port=").append(snapshot.port()).toString();
    }

    public synchronized PreparedInspection getActiveInspection() {
        return inspection;
    }

    /** Resolving artifact paths precedes this call; bind the receiver on a background thread. */
    public PreparedInspection prepareInspection(InspectorConfiguration snapshot, boolean recordOnStart) throws IOException {
        final long version;
        synchronized (this) {
            if (closed) {
                throw new IOException("ViewModel has been disposed");
            }
            if (preparing || inspection != null) {
                throw new IOException("This project already has an inspection session or a session still draining");
            }
            preparing = true;
            version = ++preparationVersion;
        }
        PreparedInspection prepared = null;
        try {
            prepared = new PreparedInspection(snapshot, recordOnStart);
            synchronized (this) {
                if (closed || version != preparationVersion) {
                    throw new IOException("Inspection preparation was cancelled");
                }
                inspection = prepared;
            }
            return prepared;
        } catch (IOException | RuntimeException e) {
            if (prepared != null) {
                prepared.close();
            }
            throw e;
        } finally {
            synchronized (this) {
                preparing = false;
            }
        }
    }

    /** Owns a reserved listener, then the connected session, until its workers have drained. */
    public final class PreparedInspection implements AutoCloseable {
        private final AgentReader reader;
        private final DataResolvingWorker dataResolver;
        private final boolean recordOnStart;
        private final AtomicBoolean closing = new AtomicBoolean();
        private volatile boolean connected;

        private PreparedInspection(InspectorConfiguration snapshot, boolean recordOnStart) throws IOException {
            this.recordOnStart = recordOnStart;
            dataResolver = new DataResolvingWorker(InspectorViewModel.this);
            reader = new AgentReader(eventManager, dataResolver, snapshot, this, !recordOnStart,
                    this::connectionReady, this::close);
        }

        private void connectionReady() {
            if (closing.get()) {
                return;
            }
            if (recordOnStart) {
                dataResolver.resumeProcessing();
            }
            connected = true;
        }

        public boolean isConnected() {
            return connected && !closing.get();
        }

        public DataResolvingWorker.RecordingState getRecordingState() {
            return dataResolver.getRecordingState();
        }

        /** The caller publishes RecordingStartedEvent only after this method returns. */
        public void startRecording() {
            if (!isConnected()) {
                throw new IllegalStateException("There is no connected inspection session");
            }
            dataResolver.resumeProcessing();
        }

        /** Admission closes before the caller publishes RecordingStoppedEvent. Never waits on the caller. */
        public CompletableFuture<Void> stopRecording() {
            return dataResolver.stopAndDrain();
        }

        @Override
        public void close() {
            if (!closing.compareAndSet(false, true)) {
                return;
            }
            connected = false;
            reader.close();
            dataResolver.shutdownAfterDraining().whenComplete((ignored, error) -> {
                synchronized (InspectorViewModel.this) {
                    if (inspection == this) {
                        inspection = null;
                    }
                }
            });
        }
    }

    @Override
    public void close() {
        PreparedInspection active;
        synchronized (this) {
            closed = true;
            preparationVersion++;
            active = inspection;
        }
        if (active != null) {
            active.close();
        }
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public MessModel getMessModel() {
        return messModel;
    }
}
