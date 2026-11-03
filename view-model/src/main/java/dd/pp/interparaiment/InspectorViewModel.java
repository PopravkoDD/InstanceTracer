package dd.pp.interparaiment;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Objects;
import java.util.StringJoiner;

import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.requests.AgentConfigurationChangedEvent;
import dd.pp.interparaiment.event.requests.InspectionClosedEvent;
import dd.pp.interparaiment.event.requests.RecordingStartedEvent;
import dd.pp.interparaiment.event.requests.RecordingStoppedEvent;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;
import dd.pp.interparaiment.immodel.MessModel;
import dd.pp.interparaiment.p2p.AgentReader;
import dd.pp.interparaiment.p2p.DataResolvingWorker;

public class InspectorViewModel implements AutoCloseable {
    private final EventManager eventManager = new EventManager();
    private final MessModel messModel = new MessModel();
    private final DataResolvingWorker dataResolver = new DataResolvingWorker(this);
    private final Object recordingLock = new Object();
    private volatile InspectorConfiguration configuration;
    private volatile PreparedInspection preparedInspection;
    private volatile AgentReader reader;
    private volatile boolean closed;

    public InspectorViewModel(InspectorConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "Inspection configuration is required");
    }

    public InspectorConfiguration getConfiguration() {
        PreparedInspection inspection = preparedInspection;
        return inspection == null ? configuration : inspection.getConfiguration();
    }

    public synchronized void updateOptions(InspectorConfiguration options) {
        Objects.requireNonNull(options, "Inspection configuration is required");
        if (configuration.equals(options)) {
            return;
        }
        if (preparedInspection != null || closed) {
            throw new IllegalStateException("Change inspection options before preparing an execution");
        }
        configuration = options;
        eventManager.notify(new AgentConfigurationChangedEvent(options));
    }

    /** Returns one raw JVM option. IntelliJ owns command-line quoting. */
    public String getAgentArgs() {
        InspectorConfiguration snapshot = getConfiguration();
        requireTraceTargets(snapshot);
        StringJoiner options = new StringJoiner(",");
        options.add("BridgePath=" + snapshot.bridgePath());
        for (String target : snapshot.traceTargets()) {
            options.add("TraceTarget=" + target);
        }
        options.add("host=" + snapshot.host());
        options.add("port=" + snapshot.port());
        return "-javaagent:" + snapshot.agentPath() + "=" + options;
    }

    /** Reserves the listener before the agent's single outbound connection can run. */
    public synchronized PreparedInspection prepareInspection() throws IOException {
        if (closed || preparedInspection != null) {
            throw new IllegalStateException("This ViewModel already owns an inspection execution");
        }
        InspectorConfiguration snapshot = configuration;
        requireTraceTargets(snapshot);
        ServerSocket listener = bindListener(snapshot);
        try {
            InspectorConfiguration boundConfiguration = snapshot.withPort(listener.getLocalPort());
            PreparedInspection prepared = new PreparedInspection(this, boundConfiguration, listener);
            preparedInspection = prepared;
            reader = new AgentReader(eventManager, dataResolver, listener, snapshot.recordOnStart(), prepared::close);
            if (snapshot.recordOnStart()) {
                dataResolver.startIfNeeded();
            }
            reader.open();
            return prepared;
        } catch (RuntimeException | Error exception) {
            if (preparedInspection != null) {
                preparedInspection.close();
            } else {
                listener.close();
            }
            throw exception;
        }
    }

    private ServerSocket bindListener(InspectorConfiguration snapshot) throws IOException {
        InetAddress address = InetAddress.getByName(snapshot.host());
        try {
            return bind(address, snapshot.port());
        } catch (BindException exception) {
            ServerSocket listener = bind(address, 0);
            try {
                eventManager.notify(new ShowMessageInConsoleRequest("Port " + snapshot.port()
                        + " is unavailable; this inspection will use port " + listener.getLocalPort()));
                return listener;
            } catch (RuntimeException | Error notificationFailure) {
                listener.close();
                throw notificationFailure;
            }
        }
    }

    private ServerSocket bind(InetAddress address, int port) throws IOException {
        ServerSocket listener = new ServerSocket();
        try {
            listener.bind(new InetSocketAddress(address, port), 1);
            return listener;
        } catch (IOException | RuntimeException exception) {
            listener.close();
            throw exception;
        }
    }

    private static void requireTraceTargets(InspectorConfiguration snapshot) {
        if (snapshot.traceTargets().isEmpty()) {
            throw new IllegalStateException("Add at least one Java class to Trace Targets before starting MessInspection");
        }
    }

    public void startRecording() {
        synchronized (recordingLock) {
            requireActiveInspection();
            if (!reader.isReadingMuted()) {
                return;
            }
            dataResolver.startIfNeeded();
            eventManager.notify(new RecordingStartedEvent());
        }
    }

    /** Call off the UI thread: frames accepted before the mute event finish in the existing worker. */
    public void stopRecording() throws InterruptedException {
        synchronized (recordingLock) {
            requireActiveInspection();
            if (!reader.isReadingMuted()) {
                eventManager.notify(new RecordingStoppedEvent());
            }
            reader.finishRecordingAdmission();
            dataResolver.awaitIdle();
        }
    }

    private void requireActiveInspection() {
        if (!isInspectionActive()) {
            throw new IllegalStateException("There is no active inspection execution");
        }
    }

    public boolean isInspectionActive() {
        PreparedInspection inspection = preparedInspection;
        return !closed && inspection != null && !inspection.isClosed();
    }

    public boolean isRecording() {
        AgentReader currentReader = reader;
        return isInspectionActive() && currentReader != null && !currentReader.isReadingMuted();
    }

    public boolean isClosed() {
        return closed;
    }

    void finishInspection(PreparedInspection inspection) {
        synchronized (this) {
            if (closed || preparedInspection != inspection) {
                return;
            }
            closed = true;
        }
        try {
            try {
                reader.close();
            } finally {
                inspection.closeListener();
            }
            reader.awaitTermination();
            reader.finishRecordingAdmission();
            dataResolver.awaitIdle();
        } catch (IOException | IllegalStateException exception) {
            eventManager.notify(new ShowMessageInConsoleRequest("Closing inspection: " + exception.getMessage()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            try {
                dataResolver.close();
            } finally {
                eventManager.notify(new InspectionClosedEvent());
            }
        }
    }

    @Override
    public void close() {
        PreparedInspection inspection;
        synchronized (this) {
            inspection = preparedInspection;
            if (inspection == null) {
                if (closed) {
                    return;
                }
                closed = true;
            }
        }
        if (inspection != null) {
            inspection.close();
        } else {
            dataResolver.close();
        }
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public MessModel getMessModel() {
        return messModel;
    }
}
