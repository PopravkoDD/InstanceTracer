# View-model module

Maintain the existing Maven module that connects to the agent, receives protocol data, and distributes it into models.

- Do not depend on IntelliJ, Swing, JavaFX, or other UI APIs.
- Own the client-facing communication abstraction. Keep networking, decoding, and wire-message knowledge out of `client`.
- Favor efficient ingestion and non-blocking handoff while preserving the required data and ordering semantics.
- Make model publication, mutation ownership, and concurrent reading explicit using existing mechanisms where possible.
- Do not invoke slow UI consumers on transport threads or require an IntelliJ event thread to operate.
- Define resource ownership when changing connection or subscription lifecycles; preserve cleanup and error reporting.
- Do not hide overload or disconnects by silently discarding data.
- Preserve the existing communication contract and coordinate wire changes with `agent`.
