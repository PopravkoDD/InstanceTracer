# Tracking configuration and execution

## Initialization and persistence

`TracingService` is a project service. Its constructor first requests
`TrackerSettingsService`, whose `PersistentStateComponent` state is loaded by the
IntelliJ platform before the service is returned. It then constructs
`InspectorViewModel` with the restored immutable `InspectorConfiguration` and
subscribes to `AgentConfigurationChangedEvent`. Restoring settings emits no event.

`TrackerSettingsService` stores host, port, trace targets, Debug mode, and the
optional development project root in the project's workspace storage
(`.idea/workspace.xml`, component `InstanceTracerSettings`). Missing settings use
`127.0.0.1`, port `35824`, no targets, Run mode, and automatic development root
discovery. Invalid persisted receiver settings produce an IDE log warning and
fall back to defaults. An invalid persisted development root falls back to
discovery without resetting Debug mode.

The configuration dialog edits private copies. OK validates both configurations
before applying them; Cancel leaves active settings untouched. ViewModel updates
publish `AgentConfigurationChangedEvent` through the existing `EventManager` only
when normalized options differ. Its subscriber updates persisted receiver state.
Client options are saved separately and publish no agent configuration event.
There is no settings-to-ViewModel feedback loop. External live edits to workspace
XML are not a supported way to update an already initialized ViewModel.

Trace targets use PSI reference fragments for completion and the platform class
chooser with `GlobalSearchScope.allScope(project)` (All Places, including library
classes). Adding, editing, and removing entries affects only the dialog's list.
Resolved classes are saved as JVM-qualified names (`$` for nested classes) for the
agent's exact class-name matcher. Syntactically valid unresolved names are kept,
including while indexing. Invalid names are rejected. Target order and duplicates
do not change the effective configuration.

## Development artifacts

The project service `IJavaAgentResolver` returns `AgentArtifacts(agent, hooks)`.
Only `DevJavaAgentResolver` is installed. With an explicit development root it
uses that directory (relative paths are resolved against the open project's
base directory). Otherwise it walks the open project's ancestors until it finds
the root, agent, and hooks POMs. It does not search unrelated directories or fall
back to another version of an artifact.

The resolver requires readable regular files at:

```text
<development-root>/agent/target/agent-1.0.jar
<development-root>/hooks/target/hooks-1.0.jar
```

Build these with Maven `clean install` at the development root. When tracing a
different project, specify the InstanceTracer checkout in Configure Tracking.
No artifact copying, production packaging, or distribution is implemented.

## Launch

1. `TrackerStartAction` reads `RunManager.getInstance(project).getSelectedConfiguration()`,
   the selection used by the main Run/Debug toolbar. It does not maintain a
   separate or last-used selection. Missing selections fail with an explanation.
2. It selects `DefaultRunExecutor` or `DefaultDebugExecutor` from current client
   settings, honoring the original configuration's edit-before-run preference
   before injection. It validates the resulting selection again.
3. It captures the immutable options and calls `InspectorViewModel.getAgentArgs()`
   under the same monitor. The agent requires at least one target, so the empty
   default configuration can be saved but cannot be launched.
4. `RunnerAndConfigurationSettingsImpl.clone()` copies the configuration and
   runner-specific settings. The copy is never added to RunManager. The
   environment is built with the current execution target and action data context.
5. `TrackerExecution` resolves artifacts and prepares the ViewModel listener on a
   pooled thread, using the captured receiver configuration. Failure stops this
   launch without closing another launch's receiver. The agent is the TCP client;
   binding before process creation avoids its initial connection race without
   changing the agent or reversing the protocol.
6. The client adds `BridgePath` and the `-javaagent` wrapper to the fresh ViewModel
   arguments. IntelliJ's `ParametersListUtil` preserves the existing logical VM
   options and quotes the additional option, including paths containing spaces.
   Commas in the hooks path and `=` in the agent path are rejected because the
   documented argument/JVM syntax cannot represent them safely.
7. The selected standard `ProgramRunner` executes the environment. This retains
   standard before-launch tasks, console integration, debugger, working directory,
   environment, and process lifecycle. Configuration validation failures are
   reported instead of silently launching without the agent. No `ProcessBuilder`
   is used. Runtime configuration warnings are treated as launch failures and can
   be corrected in the normal Run Configuration editor.
8. On the matching execution's `ExecutionListener.processStarted`, the copied VM
   options are restored and the existing EventManager receives
   `StartInspectionRequest` with the prepared receiver's ownership token. The
   `ReadingWorker` accepts and reads in the background; there are no blocking
   accept/connect operations on the IDE event thread. The accept timeout is
   30 seconds from reader startup, not from the start of before-launch tasks.

Startup failure closes the owned listener and resets action state. Lifecycle
subscriptions are scoped to that execution ID and disposed on failure, stop,
termination, or project disposal. Stop disconnects communication without killing
the target JVM. Normal target termination lets the reader consume buffered frames
through EOF. If the agent never connected, a 250 ms accept poll lets the reader
notice target termination and close the listener; a pending connection is still accepted and
drained. The data resolver is started once per ViewModel and retained across
connections; project disposal stops it. The original no-argument
`StartInspectionRequest` still prepares and starts a reader asynchronously.

## Supported executions and limits

Only the platform's local Java **Application** and **JAR Application** types are
supported. Subclasses supplied by other plugins, compound configurations, test
runners, Maven/Gradle configurations, remote targets, and WSL execution are rejected
explicitly. Build-tool configurations may launch multiple JVMs and do not expose
the target JVM's options through these APIs; adding support requires a dedicated
adapter. The settings-copy implementation is tied to the pinned IntelliJ SDK and
must be checked when upgrading it.

One receiver can be prepared/running per ViewModel. Two projects cannot bind the
same host/port simultaneously. Changes made during a launch apply to subsequent
launches; the current launch retains its captured options. A running target keeps
its agent even after communication is stopped. The existing bounded data queue's
overflow policy is unchanged. No end-to-end runtime or performance claims follow
from compilation; IDE and target-process functional validation require a separate
explicit assignment under the repository's validation rules.
