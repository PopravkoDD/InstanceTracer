package dd.pp.interparaiment.view.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import dd.pp.interparaiment.InspectorConfiguration;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
@State(name = "InstanceTracerSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class TrackerSettingsService implements PersistentStateComponent<TrackerSettingsService.SettingsState> {
    private static final Logger LOG = Logger.getInstance(TrackerSettingsService.class);
    private List<String> traceTargets = List.of();
    private ClientConfiguration clientConfiguration = new ClientConfiguration();

    public static final class SettingsState {
        public List<String> traceTargets = new ArrayList<>();
        public boolean recordOnStart;
    }

    @Override
    public synchronized @NotNull SettingsState getState() {
        SettingsState state = new SettingsState();
        state.traceTargets = new ArrayList<>(traceTargets);
        state.recordOnStart = clientConfiguration.recordOnStart();
        return state;
    }

    @Override
    public synchronized void loadState(@NotNull SettingsState state) {
        clientConfiguration = new ClientConfiguration(state.recordOnStart);
        try {
            traceTargets = InspectorConfiguration.normalizeTraceTargets(state.traceTargets);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid persisted InstanceTracer trace targets; using defaults", e);
            traceTargets = List.of();
        }
    }

    public synchronized List<String> getTraceTargets() {
        return traceTargets;
    }

    public synchronized ClientConfiguration getClientConfiguration() {
        return clientConfiguration;
    }

    public synchronized void saveViewModelConfiguration(InspectorConfiguration configuration) {
        traceTargets = configuration.traceTargets();
    }

    public synchronized void updateClientConfiguration(ClientConfiguration configuration) {
        clientConfiguration = Objects.requireNonNull(configuration);
    }
}
