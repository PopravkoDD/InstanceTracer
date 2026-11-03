# MessInspection execution and recording

## Configuration ownership

| Owner | Settings |
| --- | --- |
| `MessInspectionConfiguration` | Reference to an existing project Run Configuration; inspection host and port |
| `TrackerConfigurationDialog` | TraceTargets only, with the existing PSI completion and All Places chooser |
| `TrackerSettingsService` / `ClientConfiguration` | Project trace target persistence and the `recordOnStart` preference |
| `DevJavaAgentResolver` | Build-time development artifact location, read from its properties resource |
| `InspectorConfiguration` | Immutable resolved snapshot: host, port, targets, agentPath, bridgePath |

First create and configure a regular IntelliJ Run Configuration. Create a
**MessInspection** configuration and select that existing configuration by name,
then set the inspection host and port. The selector lists all configurations
registered with the project's RunManager except MessInspection configurations;
it does not filter by Application/Gradle or embed another type's editor. Choose
Run or Debug with the normal IDE executor controls. Host defaults to
`127.0.0.1` and port to `35824`.

MessInspection persists the selected configuration's RunManager unique ID and a
display name inside its own `<inspection>` settings. A project RunManager listener
updates those references when RunManager reports a renamed configuration's old
ID. Deleted configurations remain unresolved and produce an explicit selection
error instead of choosing a different configuration. Reopening the editor
restores the selection without changing the underlying profile. Older
MessInspection settings containing an embedded Application/Gradle profile retain
their endpoint but require selecting an existing configuration; embedded profiles
are no longer read or written and are not automatically registered.

Every launch creates a private native configuration/settings copy. The original
profile, its VM options, before-launch tasks, and runner settings are never edited.
The copy is not added to RunManager, saved, or displayed as another configuration.

Set **Record automatically when an inspection connects** in **Settings | Tools |
InstanceTracer**. The default is false. This preference is captured for the next
execution; changing it does not toggle an already connected session.
`TrackerSettingsService` uses `PersistentStateComponent` with workspace storage
(`.idea/workspace.xml`, component `InstanceTracerSettings`) for this option and
TraceTargets. Host and port now belong to each MessInspection Run Configuration,
not project settings. Old TraceTargets remain loadable; obsolete project host,
port, Debug mode and development-root fields are not retained in new state.

`TracingService` obtains restored settings and resolves artifacts before creating
`InspectorConfiguration` and `InspectorViewModel`. Initialization publishes no
configuration-change event. Later trace edits call ViewModel's atomic
`updateTraceTargets`, which merges with current execution options and delegates
to `updateOptions`. Only effective changes publish `AgentConfigurationChangedEvent`;
its existing settings subscriber saves trace targets. Client preferences are
saved independently without that event. Neither settings UI writes during editing
or Cancel. No feedback loop from settings into an already initialized ViewModel
is introduced.

## Development artifacts

Edit this classpath resource **before building the development plugin**:

```text
client/src/main/resources/dd/pp/interparaiment/view/service/dev-java-agent.properties
```

Set `development.project.root` to the InstanceTracer checkout. The committed value
is empty deliberately; no developer-specific path is committed. Prefer an
absolute path. Relative values retain the earlier resolver's project-base
resolution semantics. Use forward slashes on Windows, or escape backslashes using
Java Properties syntax.

`DevJavaAgentResolver` loads the resource during service initialization and keeps
the configured value for that service's lifetime. `resolve()` validates it and
returns the existing `AgentArtifacts(agent, hooks)` structure, requiring readable
regular files at exactly:

```text
<configured-root>/agent/target/agent-1.0.jar
<configured-root>/hooks/target/hooks-1.0.jar
```

Missing resource/property, invalid directories, invalid paths, and missing JARs
produce actionable diagnostics. There is no fallback to unrelated directories or
artifact versions. Build artifacts with Maven `clean install` in that checkout.
`TracingService.resolveConfiguration` uses this same resolver before constructing
any resolved inspection snapshot. Paths are not client settings and are not
serialized into project or Run Configuration settings. No copying or production
artifact distribution is added. An unconfigured development build reports the
resource problem in the tool window or launch error instead of creating a
ViewModel with fake/unresolved paths.

## Run/Debug integration

`MessInspectionConfigurationType` registers the wrapper. One
`MessInspectionProgramRunner` handles standard Run and Debug. It resolves the
saved reference against `RunManager.getAllSettings()`, then uses the platform's
`RunnerAndConfigurationSettingsImpl.clone()` to copy both the native profile and
its per-runner settings. Before-launch tasks are cloned separately because the
native profile clone can retain the original task objects. The pinned IDEA
2023.3.6 settings implementation exposes this clone; other settings
implementations fail explicitly rather than receive a shallow copy.

The native program runner is discovered with `ProgramRunner.getRunner()` for the
copied profile and requested executor. `ExecutionEnvironmentBuilder` receives the
copied settings and the native runner's settings. The environment keeps
MessInspection's small dispatch runner so standard console reruns resolve the
current reference and create another fresh copy/session. The native runner still
executes the native profile, preserving its process state, before-launch work,
console, and debugger. Only inspection copies carry the private rerun marker;
ordinary Run/Debug executions are unaffected. There is no custom process launcher
or `ProcessBuilder`.

`TrackerExecution` retains its execution/session ownership responsibilities:

1. Capture the endpoint and `recordOnStart`.
2. Resolve both artifacts and construct the immutable snapshot on a pooled thread.
3. Request the complete `-javaagent` option from `InspectorViewModel.getAgentArgs(snapshot)`.
4. Apply that option to the private execution copy and check its native settings.
5. Reserve/bind a receiver through `prepareInspection(snapshot, recordOnStart)` and
   send the existing scoped `StartInspectionRequest`. Accept runs in the background
   before the native target starts, preserving startup-race handling.
6. Check the native execution state and invoke the selected native runner.
7. On the actual copied environment's `processNotStarted`/`processTerminated`, or
   project disposal, close the owned session and remove its subscriptions/options.

The process listener compares environment identity against the environment passed
to the native runner, not the outer wrapper's environment. Recording actions and
`PreparedInspection` are unchanged. Because Gradle can build before JavaExec
starts, the bound listener has no arbitrary pre-connection timeout; failure,
cancellation, completion, and disposal retain the existing cleanup path.

Java configurations must expose `CommonJavaRunConfigurationParameters` and
produce a `JavaCommandLineState`. The complete generated option is appended as
one correctly quoted VM parameter on the copy, preserving the original VM option
text. The code does not reproduce Application or other Java state implementations.
Each launch starts from the saved original, so earlier injected options cannot
accumulate; an identical existing option is not appended again.

Gradle retains the existing small target-task injection helper. The generated
script is set on the copied configuration's `GradleTaskManager.INIT_SCRIPT_KEY`,
which the native external-system implementation copies to each newly requested
`ExternalSystemRunnableState`. No wrapper state cache is needed. An existing
runtime init script is retained. The script checks the exact requested task in
the execution graph, requires `JavaExec`, and appends the agent only to that task's
JVM arguments in `doFirst`. Gradle daemon VM options are unchanged, and other
JavaExec tasks in the dependency graph receive no inspection agent from this
script. Cleanup restores only the private copy's previous options.

Selection does not imply execution compatibility. Non-JVM profiles, compound
profiles, attach-only configurations, profiles without target VM options, and
unsupported execution states fail with a clear error. Remote targets and WSL
preparation remain unsupported. IDE execution providers which replace the copied
profile/runner (for example build-tool delegation) are rejected; select a direct
Java configuration or a compatible Gradle JavaExec configuration instead. Only
Run and Debug are supported, not Coverage/profiling. Native pre-launch settings
validation runs before binding the inspection receiver. The copy disables the
optional native edit-before-run dialog, including on console rerun, so the IDE
cannot automatically open/save an injected draft. Configure the underlying
application beforehand; its stored edit-before-run preference remains unchanged.

Gradle support remains limited to one exact JavaExec/application-run task (such
as `:run` or `:app:run`), preserving native task options such as `--args`.
Test mode, script/daemon debugging, continuous execution, dry-run, and
configuration-cache mode are rejected. The init script also rejects configuration
cache enabled through Gradle properties and non-JavaExec or excluded requested
tasks before launching an application. The bundled Gradle dependency is unchanged.

## Recording lifecycle

Launching a JVM is separate from starting/stopping recording. Renamed
`StartRecordingTrackerAction` and `StopRecordingTrackerAction` never execute or
terminate a process and never clear the collected `MessModel`.

`ReadingWorker` starts as part of receiver startup and continuously consumes the
framed TCP stream after connection. At connection readiness, `recordOnStart=true`
starts the resolver before the first payload is forwarded and initializes
`isReadingMuted=false`. With false, reading is muted and the resolver is initially
inactive (its Thread is not started). Muting consumes complete payloads without
allocating/decoding a payload for the recording pipeline.

StartRecording obtains the active project inspection, starts/resumes its existing
resolver first, then publishes `RecordingStartedEvent(inspection)` through the
existing EventManager. ReadingWorker's matching subscriber only assigns
`isReadingMuted=false`. Repeated starts cannot create another thread.

StopRecording closes admission synchronously and publishes
`RecordingStoppedEvent(inspection)`; its matching subscriber only assigns
`isReadingMuted=true`. Neither event subscriber manages a queue, starts/stops
workers, emits more events, or changes models. Identity checks route events only
to the owning reader, and closing a reader unsubscribes its listeners.

Admission is accounted for after the first header byte arrives and before reading
the remaining header/payload. Stop therefore includes partially read admitted
frames without waiting for an idle socket. An admission permit spans framing,
payload reading and enqueueing; the resolver separately counts queued plus
currently processing messages. Already admitted frames may finish enqueueing
after the stop request. New frames are still consumed but not admitted. Drain
completion requires both counters to reach zero, not merely an empty queue.
Failures of the last active message complete the drain exceptionally, not as a
successful stop.

The resolver drains admitted work and changes `DRAINING -> PAUSED`. A started
worker waits on a monitor while idle/paused; StartRecording resumes that same
thread. Resume during DRAINING is rejected and the Start action remains disabled
until draining finishes. No busy loop, thread restart, queue clear, model reset,
or EDT join/wait is used. A slow/incomplete admitted frame can delay drain
completion until it finishes or the connection closes, without blocking the IDE.
The original bounded queue capacity and overflow policy remain; saturation can
still drop frames and now reports overload in the console. Muted frames are
intentionally discarded by the requested recording policy, not buffered for later
recording. No loss-free overload guarantee is claimed.

Execution termination is different: `ExecutionListener.processTerminated` closes
ReadingWorker and its sockets immediately. Admitted/queued processing drains
asynchronously, then the session resolver terminates. EOF/communication failure
uses the same idempotent session cleanup. StopRecording only pauses/drains; it
does not close the reader or end the inspection. Previously collected data is
retained through recording cycles and remains in the existing project ViewModel
when an inspection ends, preserving the earlier model ownership.

## PreparedInspection and session limits

Previously `PreparedInspection` held the prepared AgentReader and prevented a
failed/stale launch from closing another reader. It did not store a Run
Configuration or select one. Its ownership purpose is retained: it now owns that
listener/reader plus the session's DataResolvingWorker, connection state, and
recording transitions. It is not a second launcher or a process handle.

Its complete lifecycle is:

1. ViewModel reserves its existing preparation slot under a short lock and version.
2. The receiver is bound outside the ViewModel lock; a cancellation/disposal during
   binding closes the new token rather than publishing a stale session.
3. The token is installed as the project's current inspection and is carried by
   StartInspectionRequest and the execution owner's callbacks.
4. Connection readiness makes it active and optionally starts automatic recording.
5. Recording actions refer to this token; pause/resume does not replace it.
6. Process failure/end, EOF, or disposal closes it once, removes reader event
   subscriptions, closes communication, and drains/terminates its resolver.
7. Only when that resolver finishes is the ViewModel slot cleared, guarded by
   token identity so late cleanup cannot clear a different session.

The project ViewModel still has one `inspection` slot, one preparation reservation,
one EventManager and one collected model. **Simultaneous independent sessions in
the same project are not supported**, and a new launch is rejected while its
previous session is draining. Different projects have separate services/tokens
and may inspect concurrently with distinct host/port bindings. No application-wide
session registry or multi-session redesign is added. Supporting multiple sessions
in one project needs explicit model/session selection and ownership work later.
Trace-target changes affect future JVM launches; recording toggles do not
re-instrument an already running agent. Edits to the selected original affect the
next launch/rerun, while an already prepared execution owns its independent copy.

Runtime/IDE integration, actual settings restoration, and debugger behavior require
functional validation. For this refactoring, source/API review covered native
Application, JAR Application, and Gradle JavaExec paths; no Run Configuration
implementation was executed. The execution environment was unavailable, so Maven
and Gradle builds could not run. No IntelliJ SDK was downloaded.

Pending IDE checks include selection/restoration and rename/deletion; source
configuration immutability across Run/Debug/rerun; exact target JVM agent options;
incompatible-profile errors; recording pause/resume; and cleanup after process
failure/termination. No new test harness or session-management design is introduced.
