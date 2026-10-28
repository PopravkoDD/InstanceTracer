package dd.pp.interparaiment;

import java.util.List;
import java.util.Objects;
import javax.lang.model.SourceVersion;

/** Immutable, validated options shared by argument generation and the receiver. */
public record InspectorConfiguration(String host, int port, List<String> traceTargets) {
    public InspectorConfiguration() {
        this("127.0.0.1", 35824, List.of());
    }

    public InspectorConfiguration {
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
        if (traceTargets == null || traceTargets.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Trace targets must not be null");
        }
        traceTargets = traceTargets.stream().map(String::trim).distinct().sorted().toList();
        for (String target : traceTargets) {
            if (!SourceVersion.isName(target, SourceVersion.RELEASE_17)) {
                throw new IllegalArgumentException("Invalid fully qualified class name: " + target);
            }
        }
    }
}
