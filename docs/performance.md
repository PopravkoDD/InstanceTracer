# Collection and ingestion requirements

## Current priority

Try to preserve all collected data while minimizing interference with the target JVM. Favor non-blocking queues, minimal allocation, efficient transmission, and efficient view-model ingestion.

No lossy collection mode is authorized by default. Do not add silent drops, sampling, truncation, or lossy aggregation as an optimization.

## Future collection policies

A second mode for particularly high-load cases may be introduced after testing and investigation. Keep architecture and interfaces capable of supporting both data-preserving and explicitly reduced-data collection policies. Agent arguments and instrumentation decisions are expected to select collection behavior. Exact mode semantics will be defined in individual tasks.

Distinguish events intentionally not produced under a configured collection policy from events lost after being collected. Do not treat an instrumentation-level reduction policy as an implicit transport overflow policy.

Prefer existing extension points. This document does not require speculative mode enums, new interfaces, or a complete second implementation.

## Overload is an unresolved policy decision

Loss-free delivery, finite memory, and a producer that never waits cannot all be guaranteed under indefinitely sustained overload. Instrumentation-time policy selection can reduce event production but does not by itself settle disconnect, queue saturation, or consumer-stall behavior.

For changes touching these cases, inspect the existing policy. Preserve it unless the task authorizes a change; report conflicts with the loss-preservation goal. Do not silently choose unbounded memory growth, disk spooling, target-thread blocking, or event dropping as a new policy. Ask for a focused decision when the task cannot be completed without one.

## Implementation review

For affected paths, inspect producer/consumer ownership, capacity, event ordering, buffer ownership, object lifetime, shutdown, and error propagation. Avoid unnecessary strong references to observed application objects. Do not reuse a mutable event or buffer before all relevant consumers have finished with it.

Keep socket I/O and heavy serialization off instrumented application threads wherever feasible. Keep UI work off ingestion threads.

No numeric overhead budget has been defined. Explain expected cost changes honestly; do not claim zero allocation, lock freedom, or faster execution without evidence. Benchmark and test development require a separate task.
