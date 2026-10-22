# Architecture

## Established project context

All five module cores already exist. This document records intended responsibilities supplied by the owner, not an audit of the current dependency graph. Inspect the implementation before changing it. Report material discrepancies instead of silently rewriting the project.

| Module | Build | Responsibility |
| --- | --- | --- |
| agent | Maven | Instrument target JVM code, collect information, transmit it over TCP |
| hooks | Maven | Bootstrap bridge for interaction within the target JVM |
| view-model | Maven | Agent communication, data ingestion, distribution into UI-independent models |
| client | Gradle | IntelliJ integration, forms, and model presentation |
| common | Maven | Shared utilities |

## Boundaries

The design resembles MVVM without being a strict implementation. The agent and its host process act as the observed model/source of state. The view-model receives and organizes that information. The client presents it and accesses agent communication exclusively through view-model interfaces.

TCP connects the agent process to the view-model side. This runtime connection does not prescribe a compile-time dependency between those modules. Preserve existing shared protocol ownership until a task explicitly changes it.

Bootstrap bridge code must remain loadable in its actual classloader/module context. Dependencies usable by other modules are not automatically usable by hooks.

## Compatibility

The supported target-JVM baseline starts at Java 17. Keep target-loaded artifacts and their dependencies compatible with that baseline. Build-tool and IntelliJ runtime requirements are separate concerns; derive them from the existing build configuration. Do not promise compatibility with every later JVM without evidence.

## Evolution

Prefer focused extensions to existing code. Preserve the distinction between current implementation, agreed architectural constraints, and future plans. The launch lifecycle is documented separately; the protocol's detailed specification will be supplied later.
