# MessInspection execution and recording

## Configuration ownership

| Owner | Settings |
| --- | --- |
| `MessInspectionConfiguration` | Embedded native Application or Gradle settings; inspection host and port |
| `TrackerConfigurationDialog` | TraceTargets only, with the existing PSI completion and All Places chooser |
| `TrackerSettingsService` / `ClientConfiguration` | Project trace target persistence and the `recordOnStart` preference |
| `DevJavaAgentResolver` | Build-time development artifact location, read from its properties resource |
| `InspectorConfiguration` | Immutable resolved snapshot: host, port, targets, agentPath, bridgePath |

Create a **MessInspection** configuration through the standard IntelliJ Run/Debug
configuration UI. Select Application or Gradle, configure its native editor, then
set the inspection host and port. Choose Run or Debug with the normal IDE executor
controls; the former client Debug mode preference is no longer a second executor
selector. Host defaults to `127.0.0.1` and port to `35824`.

The embedded native profile is owned by MessInspection, not a reference to another
saved configuration. Its native `SettingsEditor` handles application/Gradle fields,
and native `readExternal`/`writeExternal` persist them inside the MessInspection
configuration. Before-launch tasks and console/log settings are delegated to that
profile and persisted through IntelliJ's regular Run Configuration mechanism.
Only editor/template transactions use `clone()` to isolate drafts. Executing a
configuration creates no Run Configuration copies and does not modify stored VM
parameters or unrelated configurations.

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

`MessInspectionConfigurationType` and its factory register the independently
persisted type. Its native-editor delegate changes with the selected underlying
type. Four small runner adapters correspond to Application Run/Debug and Gradle
Run/Debug. They retain MessInspection as the execution profile, including console
reruns. Application adapters inherit the native Java Run/Debug implementations;
Gradle Debug inherits the native external-system debugger, and Gradle Run delegates
to the native runner because its implementation is final. There is no custom
process launcher or `ProcessBuilder`.

The adapted `TrackerExecution` now handles execution ownership only:

1. Capture execution options and `recordOnStart`.
2. Resolve both artifacts and construct the immutable snapshot on a pooled thread.
3. Request the complete `-javaagent` option from ViewModel.
4. Reserve/bind a receiver through `prepareInspection(snapshot, recordOnStart)`.
5. Install options into execution-local state and send the existing scoped
   `StartInspectionRequest`. The reader starts accepting in the background before
   the target is created, avoiding the agent's initial connection race.
6. Invoke the native runner, retaining its before-launch tasks, console, debugger,
   execution errors, and cancellation handling.
7. On the matching environment's `processNotStarted`/`processTerminated`, or project
   disposal, close the owned session and remove lifecycle subscriptions/state.

Because Gradle can build for some time before JavaExec starts, the bound listener
has no arbitrary pre-connection timeout. It is owned by the pending execution and
closed on execution failure, cancellation, completion, or project disposal.

Application uses a thin native `JavaApplicationCommandLineState` subclass to add
the option to freshly created `JavaParameters`, not to the configuration's VM
settings. The complete argument is one parameter, including paths with spaces.
For Gradle, `GradleTaskManager.INIT_SCRIPT_KEY` is set only on the execution's
`ExternalSystemRunnableState`. The native plugin carries this init script into
its build execution. It checks the exact requested task in the execution graph,
requires `JavaExec`, and appends the option only to that task's JVM arguments in
`doFirst`. Gradle daemon VM options are not changed. Other JavaExec tasks in the
dependency graph receive no inspection agent from this script.

IntelliJ's `ExecutionEnvironment.getState()` constructs state rather than caching
it. MessInspection therefore holds prepared state in environment user data for
one execution. Every launch/rerun resets that state and option; cleanup removes
only its owner's data. Repeated state lookups reuse the prepared Gradle init
script and cannot silently lose it. Argument insertion is idempotent within the
execution, and persisted settings never accumulate `-javaagent` options.

Gradle support is intentionally limited: one exact JavaExec/application-run task
(such as `:run` or `:app:run`), with native task options such as `--args` preserved.
The configuration rejects test mode, Gradle script/daemon debugging, continuous
execution, dry-run, and configuration-cache mode. The init script also rejects
configuration cache enabled through Gradle properties and non-JavaExec or excluded
requested tasks before launching a target application. Remote targets and WSL
preparation are unsupported. Only Run and Debug are adapted, not Coverage or
profiling executors. The bundled Gradle plugin is the only new plugin dependency.

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
re-instrument an already running agent. Changing the underlying type during
preparation, or rerunning an old console after changing its type, fails with an
instruction to launch again from the toolbar.

Runtime/IDE integration, actual settings restoration, and debugger behavior remain
subject to future functional validation. Build results alone do not establish
the runtime scenarios above; this task permits only build validation.
