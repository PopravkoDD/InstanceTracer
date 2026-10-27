package dd.pp.interparaiment.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AgentArgumentsResolver {
    public static final List<String> MANDATORY_ARGUMENTS = List.of("BridgePath", "TraceTarget", "host", "port");

    private AgentArgumentsResolver() {
    }

    public static AgentConfiguration resolve(String agentArgs) {
        final Map<String, List<String>> arguments = resolveArguments(agentArgs);
        return createAgentConfiguration(arguments);
    }

    private static Map<String, List<String>> resolveArguments(String agentArgs) {
        final Map<String, List<String>> arguments = new HashMap<>();
        if (agentArgs != null && !agentArgs.isBlank()) {
            for (String argument : agentArgs.split(",", -1)) {
                final String[] keyValue = argument.split("=", 2);
                if (keyValue.length != 2 || keyValue[0].isBlank()) {
                    throw new IllegalArgumentException("Agent arguments must use the format name=value");
                }

                final String name = keyValue[0].trim();
                final String value = keyValue[1].trim();
                if (MANDATORY_ARGUMENTS.contains(name)) {
                    arguments.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
                }
            }
        }

        checkMissingArgs(arguments);

        return arguments;
    }

    private static AgentConfiguration createAgentConfiguration(Map<String, List<String>> arguments) {
        final String bridgePath = singleValue(arguments, "BridgePath");
        final String host = singleValue(arguments, "host");
        final String portValue = singleValue(arguments, "port");
        final int port;
        try {
            port = Integer.parseInt(portValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Agent argument port must be an integer between 1 and 65535");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Agent argument port must be an integer between 1 and 65535");
        }

        return new AgentConfiguration(bridgePath, arguments.get("TraceTarget"), host, port);
    }

    private static void checkMissingArgs(Map<String, List<String>> arguments) {
        final List<String> missingArguments = new ArrayList<>();
        for (String name : MANDATORY_ARGUMENTS) {
            final List<String> values = arguments.get(name);
            if (values == null || values.stream().anyMatch(String::isBlank)) {
                missingArguments.add(name);
            }
        }
        if (!missingArguments.isEmpty()) {
            throw new IllegalArgumentException("Missing mandatory agent arguments: " + String.join(", ", missingArguments));
        }
    }

    private static String singleValue(Map<String, List<String>> arguments, String name) {
        final List<String> values = arguments.get(name);
        if (values.size() != 1) {
            throw new IllegalArgumentException("Agent argument " + name + " must be specified only once");
        }
        return values.get(0);
    }
}
