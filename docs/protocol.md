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
