# Agent configuration

The packaged agent entry point is `InstanceTracerAgent`. It resolves comma-separated
`name=value` arguments into an immutable `AgentConfiguration` before initializing
the bootstrap bridge, installing trackers, or opening the TCP connection.

| Argument | Required | Meaning |
| --- | --- | --- |
| `BridgePath` | Yes | Path to the bootstrap hooks JAR |
| `TraceTarget` | Yes | Fully qualified class name whose constructors are traced; repeat this argument for multiple classes |
| `host` | Yes | TCP receiver hostname or IP address |
| `port` | Yes | TCP receiver port, an integer from 1 to 65535 |

For example:

```text
-javaagent:/path/agent-1.0.jar=BridgePath=/path/hooks-1.0.jar,TraceTarget=com.example.First,TraceTarget=com.example.Second,host=127.0.0.1,port=35824
```

Argument names are case-sensitive and matched exactly. Whitespace around names
and values is trimmed. Only `TraceTarget` can occur multiple times. Values may
contain `=` but cannot contain commas. Unknown named arguments are ignored.

Missing or blank required values, malformed arguments, duplicate singleton
arguments, and invalid ports produce a console diagnostic and stop agent startup
by returning from `premain`; the target JVM can continue without the agent.
There are no default values for the receiver host or port. This validation does
not verify that the hooks JAR exists or that the receiver is reachable.

The separate legacy `FxListeningAgent` is not the packaged entry point and retains
its existing argument handling.

## IntelliJ client responsibilities

`InspectorViewModel.getAgentArgs()` formats repeated `TraceTarget`, `host`, and
`port` from one immutable receiver configuration. The client resolves both JAR
paths, supplies `BridgePath`, and wraps these arguments in a temporary
`-javaagent` JVM option. It does not format receiver options independently.
See [tracking lifecycle](lifecycle.md) for project persistence, development artifact
resolution, supported Run/Debug configurations, and receiver startup ordering.
