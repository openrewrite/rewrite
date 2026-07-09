# Phase 4 slice C — integrated perf gate + the two engine debt items

2026-07-09, worktree `maven-resolution` (uncommitted). The last construction slice before flip-readiness review:
the honest end-to-end perf gate now that both engines share one facade on one classpath, plus the two
phase3-results-a deviations (#2 supplier-override duplication, #3 metadata-region routing). Hermetic gates green in
both modes.

## 1. Integrated perf gate — the ratio table

`MavenParser` end-to-end, one JVM, `ctx engine=legacy` vs `engine=maven`, identical reactor input set. Harness:
`phase0-tools` task `integratedBenchmark` (`IntegratedBenchmark`) — supersedes `spike/benchmark`'s split-JVM OLD/NEW
design, which predated integration and measured the *raw, unadapted* pipeline. Warm iterations are zero-download
(RecordingHttpSender record-once/replay); warm = median of 5 after 2 warm-ups, fresh cache per iteration; loop =
warm-cache re-resolution of the full reactor. Full numbers: `benchmarks/integrated-2026-07-09.{json,md}`.

| corpus/tier | modules | heap | legacy warm ms | maven warm ms | **legacy/maven** | maven slower | legacy loop ms | maven loop ms | loop slower | heap MB (L / M) |
|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| maven full | 15 | 2g | 149 | 690 | **0.22×** | 4.6× | 102 | 196 | 1.9× | 164 / 1021 |
| camel 50 | 50 | 768m | 1,951 | 4,408 | **0.44×** | 2.3× | 1,006 | 2,404 | 2.4× | 564 / 515 |
| camel 150 | 150 | 768m | 2,766 | 7,274 | **0.38×** | 2.6× | 1,749 | 4,122 | 2.4× | 562 / 580 |
| camel 400 | 400 | 768m | 4,647 | 16,872 | **0.28×** | 3.6× | 3,415 | 12,819 | 3.8× | 594 / 669 |

**Verdict per tier (against DESIGN §6, which expected the adapter to fit inside the raw 2–4× headroom):**

- **maven-15 — FAIL.** maven mode is **4.6× slower** warm and allocates **6×** the heap (1021 vs 164 MB).
- **camel-50 — FAIL.** maven mode is **2.3× slower** warm, 2.4× on the warm-cache loop.
- **camel-150 — FAIL.** maven mode is **2.6× slower** warm, 2.4× loop.
- **camel-400 — FAIL (worst).** maven mode is **3.6× slower** warm, **3.8× loop** — the gap *widens* at the largest
  tier, no crossover.

Per-module marginal (warm ms/module): legacy `9.9 → 39.0 → 18.4 → 11.6`; maven `46.0 → 88.2 → 48.5 → 42.2`. This
**contradicts the raw-pipeline baseline** (NEW-N1 won every camel tier at ~2.3 ms/module). The integrated adapter layer
costs ~40 ms/module and consumes all of the raw headroom and more.

**Dominant costs named (measurement first, no speculative optimization):** (1) per-module adapter + model-building
work — the warm-cache *loop* is also 2.4–3.8× slower, so this is not only cold-cache model building but the
steady-state collect-then-project path (ModelCache deep-clones ~8.3/module on camel per the baseline, the verbose
collect projected per-scope, the shaded object graph); (2) allocation — ~6× legacy at maven-15.

**Flip-blocker discovered by the gate — FD accumulation.** maven mode exhausts the OS per-process FD cap
(`kern.maxfilesperproc` = 92,160) within a single large-reactor parse: camel-40/47/50 crash with `Too many open files`
at `-Xmx2g` (not a ulimit artifact — shell limit is 1,048,576). The handles are GC-reclaimable `FileInputStream`s from
the materialized-pom / model-source read path; the same tiers *complete* at `-Xmx768m` because frequent GC reclaims
them (hence the camel rows run at 768m — the maven camel figures are a GC-pressured upper bound; the ratio within each
tier is same-heap and fair; legacy is heap-insensitive). BOM-heavy modules leak fastest. **An FD-lifecycle fix in the
descriptor/model file-handle path is required before maven mode can resolve a BOM-heavy reactor at production heap
sizes** — not attempted this slice.

## 2. Supplier-override dedup (deviation #2) — L-P4-C-001, KEEP_REWRITE, FIXED

`EngineCollectSystemSupplier` duplicated the engine module's transport-monopoly (`HttpSenderTransporterFactory` as the
sole http/https transport, no Apache transport) and RRF-off overrides because `EngineRepositorySystemSupplier` was
package-private and could not be extended cross-module. Unified by making `EngineRepositorySystemSupplier` **public** and
having the collect supplier **extend** it, declaring only its three collect-specific components (descriptor reader,
version resolver, metadata resolver). The engine module is now the single owner of the transport/RRF overrides *by
construction*; the engine module's jar-scan / no-bypass / RRF-off tests are untouched and green.

- Files: `rewrite-maven-engine/.../EngineRepositorySystemSupplier.java` (public + doc), `rewrite-maven/.../internal/engine/EngineCollectSystemSupplier.java` (extends, overrides trimmed).
- Pin: `parity/.../internal/engine/SupplierOverrideParityTest` — asserts the collect supplier inherits the owner and
  exposes exactly `{file, openrewrite-http}` transporter factories (no Apache) with RRF sources empty.

## 3. Metadata-region routing (deviation #3) — L-P4-C-002, KEEP_REWRITE, FIXED

Non-pinned collect-time metadata reads (RELEASE/LATEST metaversions, ranges, non-pinned snapshots) previously flowed
only through the resolver's `MetadataResolver`/LRM, bypassing `MavenPomCache`'s metadata region. New
`RegionMetadataResolver` decorates the collect `DefaultMetadataResolver` (wired via the collect supplier's
`createMetadataResolver`), routing reads/writes through the region exactly like the pom-bytes flow, with the same
tri-state, deterministic-4xx-negative semantics:

- **region hit (present):** reconstruct the `maven-metadata.xml` the version resolver reads — verified that
  `DefaultVersionResolver.readVersions` consumes `metadata.getFile()`, so a reconstructed file into the scratch dir is
  read with **zero network**. Reconstruction is a minimal hand-written serializer over `MavenMetadata`'s versioning
  fields (latest/release/versions/snapshot/snapshotVersions/lastUpdated) — the read side stays the robust
  `MavenMetadata.parse`. Version *selection* remains Maven's (the resolver reads the file); rewrite only owns where the
  bytes come from.
- **region known-absent (`Optional.empty`):** replay `MetadataNotFoundException`, no network.
- **region miss (`null`):** delegate, then parse the resolver's own metadata file into a `MavenMetadata` and populate
  the region — a deterministic not-found caches a negative, a transfer error caches nothing.

`PinnedVersionResolver` behavior is preserved: it short-circuits pinned snapshots *before* any metadata request, so the
region path only ever sees non-pinned reads.

- Files: new `rewrite-maven/.../internal/engine/RegionMetadataResolver.java`; `EngineCollectSystemSupplier` adds the
  `createMetadataResolver` override; test `MockMavenServer` gains a `metadata(...)` responder.
- Pin: `parity/.../internal/engine/MetadataRegionRoutingTest` — a cold collect primes the region from the network; a
  fresh collector/session/LRM resolves the same `RELEASE` metaversion from the warm region with zero metadata network
  (fails without routing).

## Final gates

| gate | LEGACY | MAVEN | notes |
|---|---|---|---|
| `rewrite-maven-engine:test` (jar-scan, no-bypass, RRF-off, bootstrap, transport) | **PASS** | n/a | unaffected by slice C |
| `internal.engine.*` (engine + collector + new `SupplierOverrideParityTest`, `MetadataRegionRoutingTest`) | **PASS** | **PASS** | hermetic (MockWebServer/file) |
| `parity.synthetic.*` | **PASS** | **PASS** | hermetic |
| `cache.*` | **PASS** | — | hermetic |
| integrated perf gate (4 tiers) | measured | measured | verdicts above |

## Files changed (uncommitted)

- `rewrite-maven-engine/.../engine/EngineRepositorySystemSupplier.java` — `public`; single owner of transport/RRF overrides.
- `rewrite-maven/.../internal/engine/EngineCollectSystemSupplier.java` — extends the engine supplier; adds `createMetadataResolver`.
- `rewrite-maven/.../internal/engine/RegionMetadataResolver.java` — new (metadata-region routing).
- tests `internal/engine/{SupplierOverrideParityTest,MetadataRegionRoutingTest}.java` (new), `internal/engine/MockMavenServer.java` (`metadata(...)`).
- `phase0-tools/.../corpus/IntegratedBenchmark.java` (new) + `phase0-tools/build.gradle.kts` (`integratedBenchmark` task).
- `benchmarks/integrated-2026-07-09.{json,md}` (new).
- `doc/maven-resolution-ledger.md` — L-P4-C-001 / L-P4-C-002 added.

## For the flip-readiness review

Two items block a flip and are **not** in the slice-C mandate (measurement slice):

1. **maven mode is 2.3–4.6× slower than legacy end-to-end at every measured tier**, with no crossover — the opposite of
   the raw-pipeline projection. The steady-state (warm-cache) path is itself 2.4–3.8× slower, so this is not fixable by
   cache warming alone; it needs the adapter/ModelCache work DESIGN §6 flagged as optional to become mandatory.
2. **FD accumulation** exhausts the 92k per-process cap on BOM-heavy reactors at production heap — a resource-lifecycle
   bug in the descriptor/model file-handle path.
