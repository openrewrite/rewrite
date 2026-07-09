# Phase 4 slice B — pre-flip cleanup (the last four non-ALIGN census categories)

2026-07-09, worktree `maven-resolution` (uncommitted). Charter: close/arbitrate the four non-ALIGN census categories
hermetically (Central rate-limited — no live-network iteration). Net source change is **6 lines** in one mapper method;
everything else is a hermetic pin, a test-infra fix, or a ledger reclassification.

## Per-item disposition

**Item 1 — classifier ""-vs-null threading (~17, netty-tcnative) — FIXED + pinned (L-P4-B-002, KEEP_REWRITE).**
Reproduced hermetically (fixture `empty-classifier`: a transitive `mid` declares `leaf` with an empty `<classifier></classifier>`).
The repro **inverted the assumed direction**: LEGACY threads `""` (RawPom preserves the empty element), the engine threaded
`null` (`transitiveRequested` emptyToNull'd aether's classifier). To match legacy exactly, `transitiveRequested` now threads
the declaring pom's raw declared classifier (`declared.getClassifier()`, mirroring legacy's `dd.getDependency().getClassifier()`),
falling back to `emptyToNull(aether)` only when no lineage pom declares the coordinate. Fails pre-fix, passes post-fix.

**Item 2 — L-P3-E-006 intra-pom duplicate scope (moxy) — REVERTED, hypothesis DISPROVEN, stays OPEN.**
Implemented the surgical scope-only last-wins lookup (`scopeDeclared`, `matchDeclared` untouched → version threading safe). A
hermetic repro (transitive declaring a coordinate compile-then-test) **disproved the premise**: LEGACY resolves the coordinate
INTO compile (first-wins-like), the engine's existing first-wins `matchDeclared` already matches, and last-wins made the engine
*exclude* it — the exact mechanism behind slice F's 81-test regression. Reverted per revert-and-ledger; moxy's exclusion has a
different (unidentified, likely mediation/supersession) cause. Ledger L-P3-E-006 updated with the disproof.

**Item 3 — L-P3-D-004 "recipe re-resolution" residue — RECLASSIFIED as relocation (L-P4-B-001, ALIGN), pinned.**
Ran the `ChangeDependencyGroupIdAndArtifactId` family in SHADOW: the `org.apache.commons:commons-io` vs `commons-io:commons-io`
signature is **not** a threadingEqual bug. `org.apache.commons:commons-io:1.3.2` is a `<distributionManagement><relocation>` to
`commons-io:commons-io` (verified on Central); the engine's real maven-resolver follows it (Maven-correct), legacy never reads
`<relocation>`. Mismatch fires at initial parse, both sides at the same version. Hermetic fixture `relocation` reproduces it;
registered as an expected flip (ALIGN, legacy outlier, flip P5). No engine change — engine is Maven-correct.

**Item 4 — synthetic/shadow-facade infra (~30, 17 live reds) — FIXED (test-infra).**
The MockWebServer mocks are stateless path-keyed dispatchers, so the 17 SHADOW reds are the dual-run oracle surfacing ledgered
*value* divergences (L-P0-005 datedSnapshot, L-P0-006 html-index, L-P3-C-005 message-shape, ctx-injected-repo universe, pom-less
jar synthesis) — not new bugs, and not fixable by per-pass recording-scope isolation. The transport suite validates the LEGACY
downloader (which both engines share for byte-fetch), so `SyntheticHarness.Session` now pins legacy in shadow mode; a MAVEN-mode
pin (`MetaversionResolutionTest`) overrides via customize. Green in both modes, no legacy pin weakened.

**Item 5 — full SHADOW census — DEFERRED on network.** Central answered spot probes, but only warm-cache class runs (0 downloads)
were feasible; a full `:rewrite-maven:test` shadow run risks the slice-A rate-limit degradation. Two representative worst-class
shadow slices ran clean-of-new-divergences (below).

## Hermetic gate results

| gate | LEGACY | SHADOW |
|---|---|---|
| `internal.engine.*` (engine + routing) | **PASS** | facade-driven; reds only ledgered ALIGN |
| `parity.*` comparison/determinism/identity (+ new `empty-classifier`, `relocation` fixtures) | **PASS** | DEFAULT-mode tools (not run under `-Dengine=shadow`; there the facade throws on their ledgered ALIGN flips L-P0-001/L-P2-B2-002/L-P4-B-001) |
| `cache.*` | **PASS** | reds only L-P3-C-004 (kotlin/tree-shape ALIGN) |
| `parity.synthetic.*` | **PASS** | **PASS** (item 4) |
| slice-F regression `AddDependencyTest` (petclinic/logback/guava) | **PASS** | 1–2 flake reds, all guava metaversion/closure ALIGN (L-P3-E-004/C-004), present at HEAD; item-1 is a no-op on classifier-less closures (A/B confirmed) |

## Census (warm-cache shadow slices, 0 downloads)

- `ChangeDependencyGroupIdAndArtifactIdTest` SHADOW: every red is ledgered — relocation (L-P4-B-001, now pinned), kotlin
  `annotations:13.0` (L-P3-C-004), nested tree-shape (L-P3-C-004). No new signature.
- `AddDependencyTest` SHADOW: 1–2 flake reds, guava metaversion/closure ALIGN. No new classifier-"" signature (item-1 confirmed
  net-neutral by revert A/B; the fix only fires on empty-`<classifier>` transitives, absent from these closures).
- Full-suite census: **deferred on network** (Central rate-limited), per the slice-A precedent; hermetic suites carry the gate.

## Files changed (uncommitted)
- `internal/engine/DependencyGraphMapper.java` — `transitiveRequested` threads declared raw classifier (L-P4-B-002, 6 lines).
- test `parity/EngineDependencyGraphComparisonTest.java` — `relocation` expected flip (L-P4-B-001).
- test `parity/synthetic/SyntheticHarness.java` — shadow-aware (pin legacy in shadow mode).
- new fixtures `parity/fixtures/{empty-classifier,relocation}/`.
- `doc/maven-resolution-ledger.md` — L-P4-B-001/002 added; L-P3-E-006 hypothesis-disproof; synthetic test-infra note.
