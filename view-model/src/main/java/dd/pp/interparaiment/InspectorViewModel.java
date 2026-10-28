package dd.pp.interparaiment;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
    private AgentReader reader;
    private InspectorConfiguration configuration;
    private boolean resolverStarted;
    private boolean closed;
    private boolean preparing;
    private long preparationVersion;
    private final DataResolvingWorker dataResolver;

    private final MessModel messModel;


    public InspectorViewModel() {
        this(new InspectorConfiguration());
    }

    public InspectorViewModel(InspectorConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.messModel = new MessModel();

        this.dataResolver = new DataResolvingWorker(this);

        initListeners();
    }

    private void initListeners() {
        this.eventManager.subscribe(StartInspectionRequest.class, data -> {
            if (data.getInspection() == null) {
                final long version;
                synchronized (this) {
                    version = preparationVersion;
                }
                CompletableFuture.runAsync(() -> startCurrentInspection(version));
                return;
            }
            try {
                startInspection(data.getInspection());
            } catch (IOException e) {
                this.eventManager.notify(new ShowMessageInConsoleRequest("Connection failed: " + e.getMessage()));
            }
        });

        this.eventManager.subscribe(StopInspectionRequest.class, data -> stopInspection());
    }

    private void startCurrentInspection(long version) {
        try {
            synchronized (this) {
                if (closed || version != preparationVersion) {
                    return;
                }
                if (reader != null && !reader.isClosed()) {
                    startInspection(null);
                    return;
                }
            }
            PreparedInspection inspection = prepareInspection(getConfiguration(), version);
            startInspection(inspection);
        } catch (IOException e) {
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

    /** The client supplies BridgePath and the agent JAR, which are platform-specific. */
    public synchronized String getAgentArgs() {
        if (configuration.traceTargets().isEmpty()) {
            throw new IllegalStateException("Configure at least one trace target before starting tracking");
        }
        StringBuilder args = new StringBuilder();
        for (String target : configuration.traceTargets()) {
            args.append("TraceTarget=").append(target).append(',');
        }
        return args.append("host=").append(configuration.host())
                .append(",port=").append(configuration.port()).toString();
    }

    /** Bind before launching the agent, on a background thread. Accepting starts via the existing command. */
    public PreparedInspection prepareInspection(InspectorConfiguration snapshot) throws IOException {
        return prepareInspection(snapshot, -1);
    }

    private PreparedInspection prepareInspection(InspectorConfiguration snapshot, long expectedVersion) throws IOException {
        final long version;
        synchronized (this) {
            if (closed) {
                throw new IOException("ViewModel has been disposed");
            }
            if (expectedVersion >= 0 && expectedVersion != preparationVersion) {
                throw new IOException("Inspection preparation was cancelled");
            }
            if (preparing || (reader != null && !reader.isClosed())) {
                throw new IOException("An inspection is already prepared, running, or draining; stop tracking or wait for it to finish");
            }
            preparing = true;
            version = ++preparationVersion;
        }
        // DNS and binding must not hold the ViewModel monitor needed by IDE actions.
        AgentReader prepared = null;
        try {
            prepared = new AgentReader(eventManager, dataResolver, Objects.requireNonNull(snapshot));
            synchronized (this) {
                if (closed || version != preparationVersion) {
                    throw new IOException("Inspection preparation was cancelled");
                }
                reader = prepared;
                return new PreparedInspection(prepared);
            }
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

    private synchronized void startInspection(PreparedInspection inspection) throws IOException {
        if (inspection != null && reader != inspection.prepared) {
            throw new IOException("This inspection no longer owns the receiver");
        }
        if (reader == null || reader.isClosed()) {
            throw new IOException("Prepare the receiver on a background thread before starting inspection");
        }
        if (!resolverStarted) {
            dataResolver.start();
            resolverStarted = true;
        }
        reader.open();
    }

    private synchronized void stopInspection() {
        preparationVersion++;
        if (reader != null) {
            reader.close();
            reader = null;
        }
    }

    /** Ownership token: failure of one launch cannot close another launch's receiver. */
    public final class PreparedInspection implements AutoCloseable {
        private final AgentReader prepared;

        private PreparedInspection(AgentReader prepared) {
            this.prepared = prepared;
        }

        public void targetTerminated() {
            prepared.targetTerminated();
        }

        @Override
        public void close() {
            synchronized (InspectorViewModel.this) {
                prepared.close();
                if (reader == prepared) {
                    reader = null;
                }
            }
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        stopInspection();
        dataResolver.interrupt();
    }

    public EventManager getEventManager() {
        return this.eventManager;
    }

    public MessModel getMessModel() {
        return messModel;
    }
}
