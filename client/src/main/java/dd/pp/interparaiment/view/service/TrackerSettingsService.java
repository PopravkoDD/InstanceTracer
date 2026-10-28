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
    private InspectorConfiguration viewModelConfiguration = new InspectorConfiguration();
    private ClientConfiguration clientConfiguration = new ClientConfiguration();

    public static final class SettingsState {
        public String host = "127.0.0.1";
        public int port = 35824;
        public List<String> traceTargets = new ArrayList<>();
        public boolean debugMode;
        public String developmentProjectRoot = "";
    }

    @Override
    public synchronized @NotNull SettingsState getState() {
        SettingsState state = new SettingsState();
        state.host = viewModelConfiguration.host();
        state.port = viewModelConfiguration.port();
        state.traceTargets = new ArrayList<>(viewModelConfiguration.traceTargets());
        state.debugMode = clientConfiguration.debugMode();
        state.developmentProjectRoot = clientConfiguration.developmentProjectRoot();
        return state;
    }

    @Override
    public synchronized void loadState(@NotNull SettingsState state) {
        try {
            clientConfiguration = new ClientConfiguration(state.debugMode, state.developmentProjectRoot);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid persisted InstanceTracer development root; using automatic discovery", e);
            clientConfiguration = new ClientConfiguration(state.debugMode, "");
        }
        try {
            viewModelConfiguration = new InspectorConfiguration(state.host, state.port, state.traceTargets);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid persisted InstanceTracer receiver settings; using defaults", e);
            viewModelConfiguration = new InspectorConfiguration();
        }
    }

    public synchronized InspectorConfiguration getViewModelConfiguration() {
        return viewModelConfiguration;
    }

    public synchronized ClientConfiguration getClientConfiguration() {
        return clientConfiguration;
    }

    public synchronized void saveViewModelConfiguration(InspectorConfiguration configuration) {
        viewModelConfiguration = Objects.requireNonNull(configuration);
    }

    public synchronized void updateClientConfiguration(ClientConfiguration configuration) {
        clientConfiguration = Objects.requireNonNull(configuration);
    }
}
