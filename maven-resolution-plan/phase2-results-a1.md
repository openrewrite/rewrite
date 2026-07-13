# Phase 2 slice A1 — POM-bytes cache region

## What was built

A pom-bytes region on `MavenPomCache`, keyed by `ResolvedGroupArtifactVersion` exactly like the
existing parsed-`Pom` region. Bytes are the durable unit because the Maven model builder needs
real XML and the parsed `Pom` is lossy.

- **`MavenPomCache`** — two new **default** methods so every existing host implementation keeps
  compiling unchanged:
  ```java
  @Nullable Optional<byte[]> getPomBytes(ResolvedGroupArtifactVersion gav) throws MavenDownloadingException;
  void putPomBytes(ResolvedGroupArtifactVersion gav, byte @Nullable [] bytes);
  ```
  Shapes mirror `getPom`/`putPom` verbatim (including the `throws`), not the raw-`byte[]` sketch in
  DESIGN §5.1. Defaults return unknown / no-op.
- **`InMemoryMavenPomCache`** — a Caffeine `Cache<ResolvedGroupArtifactVersion, Optional<byte[]>>`
  sized 100k, same as the pom region. Built internally rather than added to the public constructor
  signatures, so host callers of the existing 4-cache constructors keep compiling.
- **`RocksdbMavenPomCache`** — persists positive bytes only; `putPomBytes(gav, null)` returns
  immediately (its negative-drop stance, matching `putPom`). Values are stored as raw bytes (no
  Smile). Keys reuse the pom-region encoding style — `serialize(...getBytes(UTF_8))` — but with a
  `"pomBytes:"` prefix so they never collide with the parsed-`Pom` region in the shared keyspace.
  The `Pom.getModelVersion()` guard covers the whole store as today.
- **`CompositeMavenPomCache`** — L1-backfill-only-on-positive-L2-hit, identical to the pom region;
  a negative L2 answer is returned as authoritative but never backfilled into L1.

## Semantics decisions

- **Tri-state** preserved across all impls: `null` = unknown, `Optional.empty()` = known-absent,
  present = hit. `putPomBytes(gav, null)` records known-absent (→ `Optional.empty()`) in memory,
  mirroring `putPom(gav, null)`. RocksDB drops negatives, so it only ever returns unknown or hit.
- **Key style vs. namespace**: "keyed identically to the pom region" is the logical
  `ResolvedGroupArtifactVersion` key. In-memory that is a separate `Cache` instance (no collision);
  in RocksDB's single keyspace the physical key is prefixed to disambiguate from the parsed-`Pom`
  entries, which key on the bare `gav.toString()`. A colliding key would make `getPom` attempt to
  Smile-deserialize raw XML — a dedicated test asserts the regions stay distinct.
- **`getPomBytes` throws `MavenDownloadingException`** to mirror `getPom` exactly; RocksDB wraps a
  `RocksDBException` read failure the same way `getPom` does. `InMemory` narrows the override to no
  `throws` (legal), matching how it narrows `getPom`.

## Why no modelVersion bump

Adding a new region does not change the serialization of existing `Pom` entries, so persisted
caches remain valid — no bump. DESIGN §5.1's `Pom.modelVersion` → 4 bump is tied to the
**descriptor-region format change** (`getResolvedDependencyPom`), which is deferred pending Moderne
CLI coordination. `Pom.getModelVersion()` stays at 3.

## Tests

New `cache/MavenPomBytesCacheTest` (7 tests, all green; existing `RocksdbMavenCacheTest` still green):
- in-memory tri-state (unknown / known-absent / hit);
- RocksDB persistence round-trip across a reopen (temp dir);
- RocksDB negative-drop (null put reads back as unknown, not known-absent);
- RocksDB bytes region distinct from the pom region (no key collision);
- composite backfill on positive L2 hit; composite non-backfill of a negative L2 answer;
- compile-level guard: an anonymous `MavenPomCache` implementing only the eight pre-existing methods
  compiles and defaults `getPomBytes`/`putPomBytes` to unknown / no-op.

Scope kept to the four cache classes + the new test. `:rewrite-maven:licenseFormat` clean;
`:rewrite-maven:test --tests` for both cache classes passes (7 + 3). Not committed.
