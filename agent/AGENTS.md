# Agent module

Maintain the existing Maven-based Java agent. Inspect the existing transformation, hook installation, configuration, and TCP transmission paths before changing them.

- Keep instrumented application threads free of socket I/O and heavy serialization wherever feasible. Prefer asynchronous handoff through the existing queue design.
- Minimize per-event allocations and synchronization costs. Do not replace established concurrency mechanisms without understanding their ordering and ownership contracts.
- Preserve Java 17 compatibility in code loaded into the target JVM.
- Prevent accidental recursive tracing of the tracing machinery using the existing mechanisms; preserve existing transformation exclusions.
- Treat failure isolation, object retention, ordering, shutdown, and queue saturation as correctness concerns. Do not silently suppress failures in ways that conceal lost data.
- Preserve current protocol semantics and the configured collection policy. Read `docs/performance.md` and `docs/protocol.md` for relevant changes.
- Accommodate future collection modes through existing configuration and instrumentation extension points where possible. Do not add speculative mode implementations.
- Preserve the Shade packaging and bootstrap bridge arrangement. Validate with Maven `clean install`.
