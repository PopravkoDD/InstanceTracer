package dd.pp.interparaiment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** A complete, UI-independent snapshot of the options for one inspection. */
public record InspectorConfiguration(List<String> traceTargets, String host, int port,
                                     Path agentPath, Path bridgePath, boolean recordOnStart) {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 35824;
    private static final Pattern CLASS_NAME = Pattern.compile(
            "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*"
                    + "(?:\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");

    public InspectorConfiguration {
        Objects.requireNonNull(traceTargets, "Trace targets are required");
        traceTargets = traceTargets.stream().map(InspectorConfiguration::validateTarget)
                .distinct().sorted().toList();
        host = validateValue(host, "Host");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        agentPath = validateArtifact(agentPath, "Agent");
        if (agentPath.toString().contains("=")) {
            throw new IllegalArgumentException("The Agent JAR path cannot contain '='");
        }
        bridgePath = validateArtifact(bridgePath, "Hooks");
    }

    public InspectorConfiguration withPort(int actualPort) {
        return new InspectorConfiguration(traceTargets, host, actualPort, agentPath, bridgePath, recordOnStart);
    }

    private static String validateTarget(String value) {
        String target = validateValue(value, "Trace target");
        if (!CLASS_NAME.matcher(target).matches()) {
            throw new IllegalArgumentException("Invalid Java class name: " + target);
        }
        return target;
    }

    private static Path validateArtifact(Path value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " JAR path is required");
        }
        Path path = value.toAbsolutePath().normalize();
        validateValue(path.toString(), name + " JAR path");
        if (!path.toString().equals(path.toString().trim())) {
            throw new IllegalArgumentException(name + " JAR path cannot have leading or trailing whitespace");
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(name + " JAR is not a regular file: " + path);
        }
        return path;
    }

    private static String validateValue(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        String result = value.trim();
        if (result.indexOf(',') >= 0 || result.indexOf('\n') >= 0 || result.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(name + " cannot contain commas or line breaks");
        }
        return result;
    }
}
