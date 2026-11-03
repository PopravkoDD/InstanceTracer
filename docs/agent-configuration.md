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

`IJavaAgentResolver` supplies the existing agent/hooks artifact paths before
`InspectorConfiguration` is constructed. The immutable ViewModel-owned snapshot
contains both absolute resolved paths as well as targets, host, and port.
`InspectorViewModel.getAgentArgs()` constructs the complete `-javaagent` option,
including `BridgePath`. Client execution adapters add that single option to native
Application JavaParameters or a task-scoped Gradle JavaExec init script. They do
not duplicate argument formatting or modify stored VM settings.

The agent still requires at least one target, so the empty target default can be
saved but cannot be launched. Commas in hooks paths and '=' in agent paths are
rejected because the documented/JVM argument formats cannot represent them.
See [execution and recording lifecycle](lifecycle.md) for ownership, configuration
UI, developer properties, supported executions, and recording transitions.
