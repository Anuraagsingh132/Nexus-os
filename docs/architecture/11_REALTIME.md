# Realtime and Collaboration

Spring WebSocket/STOMP provides authenticated events; Redis distributes events across instances. Persist domain state before publishing. Each event has ID, type, tenant/workspace, entity, sequence, occurredAt, actor, and minimal payload.

Clients subscribe only after authorization and resynchronize through REST on reconnect or sequence gap. Typing and presence are ephemeral with TTLs. Read receipts are persisted but batched. Apply per-user/channel rate limits and payload ceilings.

P0 document collaboration is autosave plus optimistic concurrency and version recovery. P2 CRDT/live cursors must be behind an adapter and feature flag; do not call ordinary autosave “real-time collaborative editing.”

