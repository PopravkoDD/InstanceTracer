# MessInspection Run Configuration

Create and configure the target Run Configuration separately. Create a
**MessInspection** configuration, select that target, add Java trace targets, and
choose the receiver host, preferred port, and **Enable recording on start**.
Recording on start defaults to false. Use IntelliJ's normal Run or Debug action.

## Development artifacts

Before building the client, set `project.root` in
`client/src/main/resources/dev-java-agent.properties` to the absolute checkout
containing the Maven artifacts. Use forward slashes or Java properties escaping.
`DevJavaAgentResolver` reads the resource at initialization and resolves exactly:

- `agent/target/agent-1.0.jar`
- `hooks/target/hooks-1.0.jar`

Both must be readable regular JAR files containing their expected entry classes.
Run the documented Maven `clean install` before building the client. The resolver
does not copy, distribute, search for, or select alternative artifacts. Its local
development path is not a user setting in a Run Configuration.

## Persistent settings and runtime snapshots

`MessInspectionConfiguration` stores target selection, trace targets, host, port,
and `recordOnStart` through IntelliJ's `readExternal`/`writeExternal` Run
Configuration hooks. Each MessInspection has independent settings. There is no
additional project settings file or persistent state in `TracingService`.

The editor lists all existing `RunManager` settings except MessInspection itself.
It uses the platform configuration ID, with type/name restoration. The supported
IntelliJ version uses names in these IDs, so a `RunManagerListener` updates
references on rename and records deletion. Missing configurations produce an
error and require explicit reselection. The editor does not reproduce or edit
the target configuration's settings.

`TrackerConfigurationDialog` edits a private list with Java PSI completion and
All Places resolution. It stores qualified class names and validates unresolved
classes. Cancel discards the dialog edits. The toolbar's Configure action applies
confirmed targets through an unprepared ViewModel's `updateOptions`, then updates
the owning Run Configuration for subsequent executions; it does not reconfigure
a running agent.

At execution time the client resolves both artifacts before constructing the
complete immutable `InspectorConfiguration`. The ViewModel owns argument
formatting, validation, and `AgentConfigurationChangedEvent`. `updateOptions`
publishes that event only for effective changes before a session is prepared;
constructing the initial restored snapshot emits no configuration-change event.
The Run Configuration remains the persistent source, while each execution keeps
its own runtime snapshot.

## Native execution

`MessInspectionProgramRunner` clones the selected settings using IntelliJ's
settings implementation, preserving its configuration and native runner
settings. The copy is not registered with `RunManager`. Only that copy receives
the fresh, quoted Java agent VM option. `InspectionJavaProgramPatcher` ensures
the same option is present in the actual Java parameters without duplicating it.
The original target configuration is unchanged. Its settings are checked before
preparation; configuration errors must be corrected in the original editor.
MessInspection does not open an editor for the temporary copy.

The target's native Run or Debug runner, execution target, before-run tasks,
console, debugger settings, and content reuse remain in IntelliJ's execution
flow. A small runner decorator forwards native debugger parameter patching and
routes the Run tool window's Rerun through fresh MessInspection preparation.

Supported targets are local `JavaRunConfigurationBase` configurations whose
selected native runner supports Java parameter patching, including the standard
local Application configuration. Other Java configurations depend on those same
APIs and their native runner. The selector remains unrestricted; incompatible
targets fail clearly before launch. Gradle, Maven/external-system configurations,
remote JVM attach, and target environments requiring remote preparation are not
supported by this automatic injection implementation. A compatible custom runner
must publish the standard execution lifecycle callbacks. A configuration that
forks several target JVMs is outside the single-process session model; use a
configuration that launches one local JVM per execution. No manual JVM mode exists.

## Session and TCP ownership

The TCP direction is unchanged: ViewModel is the server; the Java agent makes
its existing outbound client connection. Background preparation binds the
listener before the JVM can launch. `PreparedInspection` owns that bound socket
and the lifecycle of its isolated ViewModel session. `AgentReader` receives the
socket rather than constructing it.

ViewModel starts `ReadingWorker` before launching the JVM. Its blocking
`ServerSocket.accept()` and subsequent framed reads execute on that worker.
There is no EDT connection wait and no agent-side networking or protocol change.
The socket is already listening when the agent makes its single connection.

Concurrent sessions use separate ViewModels, sockets, queues, workers, event
managers, and tool-window tabs. If the preferred port is occupied, preparation
binds an available port and places the actual port in the session snapshot and
agent option. It does not change the persisted preferred port. There is no
shared recording toggle or merged session model.

The delegated execution environment carries its exact `PreparedInspection`.
`ExecutionListener` associates the actual `ProcessHandler` only after
`processStarted`; failed launch and termination close only that session. Cleanup
is idempotent, detaches lifecycle subscriptions, closes listener/accepted socket,
unblocks reads, drains accepted work where possible, and terminates the consumer.
Worker joins and drains run off the EDT. EOF and connection errors also close
the owning session. A failed launch is not marked as a running session.

Completed-session tabs retain their model until project disposal. The current
implementation has no session-history persistence or history-pruning UI.

## Recording

`ReadingWorker` always starts. Its initial volatile mute state is the inverse of
`InspectorConfiguration.recordOnStart`. With recording disabled, `AgentReader`
consumes complete length-prefixed frames into a reusable discard buffer without
enqueuing or deserializing their contents. It continues reading the TCP stream.
The existing wire format and decoding ownership are unchanged.

With recording enabled, raw frames enter the existing bounded queue and
`DataResolvingWorker` performs deserialization and model updates. The consumer
starts during preparation only when `recordOnStart` is true; otherwise the first
Start Recording initializes it once.

`StartRecordingTrackerAction` calls ViewModel coordination to initialize the
consumer if necessary and publish `RecordingStartedEvent` through the session's
existing `EventManager`. `StopRecordingTrackerAction` requests
`RecordingStoppedEvent` and completes its drain on a background thread. The
reader's subscribers to these two events do only their respective boolean mute
assignments. Admission barriers and worker coordination live outside them.

Stopping recording prevents further queue admissions and waits for accepted
frames, including an in-flight model update, to complete. A frame that spans a
stop/resume boundary cannot be admitted to the resumed recording. The same
consumer naturally blocks in `ArrayBlockingQueue.take()` after draining. There
is no explicit pause, worker recreation, or model reset. Resuming appends to the
existing model and leaves the target JVM running throughout.

## Limits and validation

The pre-existing bounded queue overflow policy still drops frames when its
100,000-entry queue fills. This task does not redefine overload behavior; first
saturation and total dropped messages are now reported. Processing failure or
interrupted shutdown reports unfinished accepted work. Recording mute is
intentional exclusion before queue admission.

Build validation follows `docs/build.md`: Maven reactor `clean install`, then
the client's existing Gradle `buildPlugin` task after inspecting its task graph.
Do not launch an IDE, `runIde`, a target JVM, or an improvised fixture. Build
success alone does not establish Run/Debug integration or runtime correctness.
