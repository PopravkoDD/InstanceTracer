package dd.pp.interparaiment.view.service;

import java.nio.file.Path;
import java.util.Objects;

public record JavaAgentArtifacts(Path agentPath, Path bridgePath) {
    public JavaAgentArtifacts {
        agentPath = Objects.requireNonNull(agentPath, "agentPath").toAbsolutePath().normalize();
        bridgePath = Objects.requireNonNull(bridgePath, "bridgePath").toAbsolutePath().normalize();
    }
}
