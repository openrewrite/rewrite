# 9. GetObject action field for reliable state synchronization

Date: 2026-02-26

## Status

Accepted (supersedes ADR 8)

## Context

The diff-based GetObject protocol tracks state on both sides: the sender maintains a `remoteObjects` map recording what the receiver last successfully received, and uses this as the baseline for computing diffs on subsequent transfers. This tracking is updated optimistically — the sender assumes the receiver consumed the data successfully once streaming completes.

When the receiver fails mid-deserialization (e.g., `ClassCastException` from an invalid AST node), the two sides go out of sync: the sender thinks the receiver has version N, but the receiver discarded it.

### The Print problem

This manifests concretely with `Print`. After a composite recipe runs, Java computes diffs by printing both the `before` and `after` trees. For RPC-based languages (Python, JavaScript), printing works as follows:

1. Java sends a Print RPC to the remote (Python)
2. Python's `handle_print` calls `get_object_from_java(tree_id)`, sending GetObject back to Java
3. Java's `GetObject.Handler` computes a diff against `remoteObjects[id]` (its belief of what Python has) and streams the result

If a prior Visit failed in the *reverse* direction (Java requesting a modified tree from Python), the cleanup at `RewriteRpc.getObject()` only removes Java's **requester-side** `remoteObjects` entry. Java's **handler-side** `remoteObjects` (used by `GetObject.Handler` when Python requests from Java) may still reflect a state that Python no longer has. The subsequent Print-triggered GetObject computes a diff against the wrong baseline, producing corrupt data or errors.

### Fundamental issue: unilateral state updates

The root cause is that `remoteObjects` is updated unilaterally by the sender without confirmation from the receiver. If the receiver fails to deserialize, the sender has no way to learn this — the stale state persists and affects all subsequent operations in either direction.

## Decision

Add an `action` field to the GetObject request. This nullable string field allows the receiver to send corrective actions back to the handler. When null, the request is a normal data-transfer request.

### The `revert` action

When the receiver fails to deserialize a transferred object, it sends a GetObject request with `action: "revert"`. The handler:

1. Restores `remoteObjects[id]` to the pre-transfer value (stored in an `actionBaseline` map at transfer start)
2. Restores `localObjects[id]` to the same pre-transfer value — this ensures the failed modification is discarded rather than retried with the same broken diff
3. Cancels any in-progress batch send for that ID

This reverts both sides to a consistent, known-good state. The receiver also clears its own `remoteObjects[id]` tracking, so the next transfer starts fresh.

### Optimistic updates with rollback

Unlike a deferred-commit (ACK/NACK) approach, `remoteObjects` is updated optimistically when streaming completes — no extra round-trip is needed on the success path. The `actionBaseline` map stores the pre-transfer value so that `revert` can roll it back on the failure path.

### Extensibility

The `action` field is designed to support future corrective actions beyond `revert`:

- `"clear"` / `"remove"` — tell the handler to drop all tracking for this ID (e.g., when the caller knows the object is no longer needed)
- `"abort"` — cancel an in-progress batched transfer mid-stream
- `"reset"` — force a full re-serialization

### Protocol flow

**Success path** (no extra round-trip):
1. Handler streams batches, optimistically updates `remoteObjects[id] = after`
2. Receiver processes batches, updates its own `remoteObjects` and `localObjects`
3. Done — no confirmation needed

**Failure path** (one extra round-trip):
1. Handler streams batches, optimistically updates `remoteObjects[id] = after`
2. Receiver fails to deserialize
3. Receiver sends `GetObject(id, sourceFileType, action="revert")`
4. Handler restores `remoteObjects[id]` and `localObjects[id]` from `actionBaseline`
5. Handler returns empty list

### Relationship to ADR 8 (`reset` flag)

The `reset` flag from ADR 8 is removed. The `revert` action makes it unnecessary — instead of the receiver hinting "I lost sync" on its *next* request, it explicitly tells the handler to roll back immediately after failure.

## Consequences

**Positive:**
- No extra round-trip on the success path (unlike an ACK-based approach)
- On failure, reverts both `remoteObjects` and `localObjects` to a consistent pre-transfer state, preventing cascading errors
- Fixes the Print problem: the handler's `remoteObjects` is rolled back before any Print-triggered GetObject can observe stale state
- Extensible: the `action` field can carry future corrective actions without protocol changes
- Works for all GetObject consumers (Visit, Print, Generate) in both directions

**Negative:**
- Handler must store pre-transfer baselines (`actionBaseline` map) for potential rollback — one extra object reference per active transfer
- Reverting `localObjects` means the handler discards its local modification on failure, which is a deliberate policy choice: if the receiver can't deserialize it, retrying would just fail again

**Trade-offs:**
- The `actionBaseline` entries persist until overwritten by the next transfer for the same ID, rather than being cleaned up immediately on success. The memory cost is bounded by the number of active object IDs and is comparable to `remoteObjects` itself
- The inline-Visit optimization (bundling tree data with Visit request/response to eliminate GetObject round-trips) remains a complementary performance improvement that could be pursued independently
