# TCP protocol

Status: already designed and implemented. The detailed owner-written specification is pending.

Treat the existing encoder, decoder, message definitions, and framing code as the implementation reference. Inspect both endpoints before making changes. This document does not define a replacement protocol.

When assigned a protocol change:
- Identify the current wire format and affected producer/consumer paths.
- Preserve unspecified behavior.
- Update both affected endpoints and any existing shared definitions coherently.
- Establish the intended compatibility requirement before introducing a breaking wire change.
- Update this document with verified details and the assigned change. Clearly distinguish implemented behavior from proposals.
- Run existing allowed validation; do not create a protocol test suite unless separately assigned.

The future detailed specification should record verified framing, encoding, message types and fields, ordering, size limits, connection behavior, errors, and any existing versioning or compatibility mechanism. These are documentation topics, not claims that all such mechanisms already exist.


## Recording mute behavior

The existing agent writes a four-byte big-endian signed frame length, then a
one-byte message type and the payload (`AgentWriter.sendMessage`). The length
includes the type byte. The existing receiver limit of 50,000,000 bytes is
unchanged. This recording refactor changes no wire bytes, sender behavior, or
message decoding format.

The receiver now obtains its recording admission permit after the first header
byte and before the remainder of the header/payload. When muted or admission is
closed, it consumes the full remaining frame without deserializing/forwarding a
recorded payload. Already admitted frames are counted through their read,
enqueue, and processing completion. See lifecycle.md for pause/drain semantics;
muting never stops TCP consumption or leaves a payload to be mistaken for a header.
