package dd.pp.interparaiment;

import java.util.List;
import java.util.Objects;
import java.nio.file.Path;
import javax.lang.model.SourceVersion;

/** Immutable, validated options shared by argument generation and the receiver. */
public record InspectorConfiguration(String host, int port, List<String> traceTargets,
                                     Path agentPath, Path bridgePath) {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 35824;

    public InspectorConfiguration {
        validateEndpoint(host, port);
        host = host.trim();
        traceTargets = normalizeTraceTargets(traceTargets);
        agentPath = requireArtifactPath(agentPath, "Agent");
        bridgePath = requireArtifactPath(bridgePath, "Hooks");
        if (agentPath.toString().contains("=")) {
            throw new IllegalArgumentException("The agent path cannot contain '=' in a -javaagent option");
        }
        if (bridgePath.toString().contains(",")) {
            throw new IllegalArgumentException("The hooks path cannot contain ',' in agent arguments");
        }
    }

    public static void validateEndpoint(String host, int port) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host must not be blank");
        }
        host = host.trim();
        if (host.chars().anyMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c)
                || c == ',' || c == '=' || c == '"' || c == '\'')) {
            throw new IllegalArgumentException("Host must be a hostname or IP address without argument delimiters");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be an integer between 1 and 65535");
        }
    }

    public static List<String> normalizeTraceTargets(List<String> traceTargets) {
        if (traceTargets == null || traceTargets.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Trace targets must not be null");
        }
        traceTargets = traceTargets.stream().map(String::trim).distinct().sorted().toList();
        for (String target : traceTargets) {
            if (!SourceVersion.isName(target, SourceVersion.RELEASE_17)) {
                throw new IllegalArgumentException("Invalid fully qualified class name: " + target);
            }
        }
        return traceTargets;
    }

    private static Path requireArtifactPath(Path path, String artifact) {
        if (path == null || !path.isAbsolute() || path.toString().chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(artifact + " path must be an absolute resolved artifact path");
        }
        return path.normalize();
    }

    public InspectorConfiguration withTraceTargets(List<String> targets) {
        return new InspectorConfiguration(host, port, targets, agentPath, bridgePath);
    }
}
