# Hooks module

Maintain the existing Maven bootstrap bridge used inside the target JVM.

- Keep dependencies minimal and compatible with the bridge's actual loading context.
- Do not introduce dependencies on IntelliJ, UI APIs, or application-classloader-only classes.
- Do not assume a type is bootstrap-visible because it belongs to this repository or to `common`.
- Inspect existing injection, packaging, and module-access arrangements before changing bridge signatures or dependencies.
- Prefer JDK/bootstrap-visible types at cross-loader boundaries, consistent with existing contracts. Check both type visibility and type identity.
- Avoid adding expensive work, blocking I/O, or unnecessary allocations to hook entry points.
- Preserve Java 17 compatibility and the existing safeguards against recursive instrumentation.
- Coordinate bridge contract changes with their callers in `agent`.
