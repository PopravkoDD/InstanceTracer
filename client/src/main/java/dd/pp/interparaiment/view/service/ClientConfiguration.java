package dd.pp.interparaiment.view.service;

import java.nio.file.Path;

/** IDE-only options. An empty development root uses the open project's ancestors. */
public record ClientConfiguration(boolean debugMode, String developmentProjectRoot) {
    public ClientConfiguration() {
        this(false, "");
    }

    public ClientConfiguration {
        developmentProjectRoot = developmentProjectRoot == null ? "" : developmentProjectRoot.trim();
        if (!developmentProjectRoot.isEmpty()) {
            Path.of(developmentProjectRoot);
        }
    }
}
