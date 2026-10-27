package dd.pp.interparaiment.configuration;

import java.util.List;

public final class AgentConfiguration {
    private final String bridgePath;
    private final List<String> traceTargets;
    private final String host;
    private final int port;

    AgentConfiguration(String bridgePath, List<String> traceTargets, String host, int port) {
        this.bridgePath = bridgePath;
        this.traceTargets = List.copyOf(traceTargets);
        this.host = host;
        this.port = port;
    }

    public String getBridgePath() {
        return bridgePath;
    }

    public List<String> getTraceTargets() {
        return traceTargets;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
