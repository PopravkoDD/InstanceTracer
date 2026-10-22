---
name: instance-tracer-protocol
description: Evolve Instance Tracer's existing TCP protocol coherently across agent and view-model. Use for changes to framing, messages, serialization, decoding, or wire compatibility.
---

# Change the existing protocol

1. Read `docs/protocol.md`, `docs/performance.md`, and the affected module instructions.
2. Locate actual protocol definitions, encoding, decoding, and transport boundaries. Do not interpret missing prose documentation as a missing implementation.
3. Determine the requested semantic change and compatibility expectations. If a breaking change needs an unspecified compatibility decision, ask a focused question before implementing that break.
4. Update affected endpoints and shared definitions consistently. Keep wire details out of `client`; expose needed behavior through view-model APIs.
5. Preserve ordering, data ownership, and the configured collection policy. Do not use protocol evolution as an excuse to silently drop data or redesign unrelated transport code.
6. Document verified behavior and the specific change in `docs/protocol.md`, leaving unrelated unknowns explicitly unresolved.
7. Follow the build skill and root validation rules. Use existing allowed tests when available; create new tests only when specifically assigned.
