# Phase 3 slice C — dependency resolution wired through the facade, SHADOW extended to scopes

2026-07-08, worktree `maven-resolution` (uncommitted). Routes `resolveDependencies` through `ResolutionEngineSelector`,
extends the SHADOW oracle to the full `pom`+`scopes`+`errors` snapshot, fixes the cross-test ordering flake at its
root, and runs the first full `:rewrite-maven:test` SHADOW census with dependency graphs compared. Both halves of
resolution (effective pom AND dependency graphs) now run dual-engine.

## Routing architecture delta

Two new facade entry points in `MavenEngineResolution`, mirroring the Phase-2 `effectivePom` pattern with a **separate**
`DISPATCHING_DEPS` ThreadLocal:

- **`dependencyGraph(ResolvedPom, activeProfiles, downloader, ctx, legacyAllScopes)` → `Map<Scope,List<ResolvedDependency>>`.**
  Routed from `MavenResolutionResult.resolveDependencies` (the all-scopes chokepoint where every scope is computed
  together, per the prompt). LEGACY runs the caller's per-scope legacy pass (extracted as
  `MavenResolutionResult.legacyResolveDependencies`, preserving the per-GA dedup + `partialResult` contract). MAVEN runs
  **one** verbose collect and projects all four scopes (`EngineDependencyCollector` + `DependencyGraphMapper`), rebuilding
  the effective `Model` from the requested pom (served warm from the bytes cache, so it is a model-build only). SHADOW runs
  both, diffs the full snapshot, masks, throws `AssertionError` on any unexplained diff, and returns legacy.
- **`dependencyGraphScope(ResolvedPom, scope, downloader, ctx, legacyScope)` → `List<ResolvedDependency>`.** The external
  per-scope entry (rewrite-gradle calls `resolveDependencies(Scope.Compile,…)` directly). MAVEN projects just that scope
  from the one collect; LEGACY/SHADOW defer to legacy (the all-scopes path carries the census).

The old `withoutEngine(...)` guard on `ResolvedPom.resolveDependencies(scope,…)` is **removed** and replaced with real
routing; the `withoutEngine` wrapper is retained only around the *legacy lambda* so its nested BOM/parent `Pom.resolve`
calls stay legacy. **Re-entrancy**: `DISPATCHING_DEPS` makes only the outermost `dependencyGraph`/`dependencyGraphScope`
dispatch — the SHADOW legacy pass's per-scope `pom.resolveDependencies(scope)` calls (4×) and the engine's own internal
resolves run legacy directly, never double-collecting or double-comparing. The engine collector uses its own
`RepositorySystem` (never the facade), so the effective-pom `DISPATCHING` guard is untouched. Snapshots pass `ctx=null`
so the `resolveNoChange` probe never re-enters the facade. The collector is cached on the ctx (`CollectorHandle`, its
shared `RepositoryCache` keeps a reactor's modules warm).

Direct-failure aggregation preserves L-P0-004: the engine mapper throws `MavenDownloadingExceptions` with a partial
projection; `MavenResolutionResult.resolveDependencies` re-wraps `partialResult` with the full marker (id/parent/modules/
settings) on either path. Engine effective-pom rebuild throwing a Maven-strictness error (parent-packaging, self-parent,
missing-version, …) during dependency resolution is caught and mapped to the same ledgered outcome mask the effective-pom
shadow uses (so those don't crash the dependency shadow).

The SHADOW dependency diff snapshots each side with **its own** `ResolvedPom` (legacy deps ← legacy pom, engine deps ←
engine pom) so `requestedRef` identity threading is meaningful; it filters to `$.pom`/`$.scopes`/`$.errors` and **excludes
events** (L-P3-C-006: the event-stream contract changes at cutover per DESIGN §5.6).

## Ordering-flake root cause (gate hygiene)

`UpdateMavenProjectPropertyJavaVersionTest`'s `…ExternalParentPom…` tests failed non-deterministically when the class ran
together. **Root cause: it is not engine state at all** — the class runs under `junit.jupiter.execution.parallel.mode.default
= concurrent`, and `Assertions.withLocalRepository` publishes to the *process-global* `~/.m2/repository`. Two tests in the
class (`shouldUpdatePropertyWhenParentIsExternalPomButIncludedInProjectPoms` and `verifyExternalParentPomHandling`) publish
the **same GAV** `com.example.test:test-parent:2.7.18` with **different bodies**; run concurrently they clobber each other's
`.pom` file and each other's cleanup, so whichever resolves second reads the other's parent (or an empty one) — the recipe
then makes the wrong change. Reproduced identically in **LEGACY** mode (proving it is not engine-related).
**Fix (state lifecycle, not reordering):** a process-wide `ReentrantLock` around `withLocalRepository`'s publish → run →
cleanup window (`Assertions.java`), so the on-disk pom always matches the running test. Verified: 3× green isolated runs
and green in the full concurrent LEGACY suite (the flake is gone from the 1580-test run).

## Census disposition (full `:rewrite-maven:test` in SHADOW)

Effective-pom (`$.pom`) parity is unchanged from Phase 2 (the ledgered masks absorb it). Dependency-graph (`$.scopes`/
`$.errors`) parity is the new surface. `MavenParserTest` shadow went **72 → 43 failing** through real mapper/facade fixes;
the full suite has **~381 failing**, but each failing test typically carries **several** divergence classes at once, so
per-class fixes rarely flip a whole test — the residual is dominated by a few deep mapper/collector gaps.

| class → root cause | count* | disposition |
|---|---|---|
| **repo URI spelling** — legacy `~/.m2/repository` vs engine `file:///…/.m2/repository/` (same repo) | (all) | **FIX** `SnapshotNormalizer` collapses every local-repo form to `<localrepo>` (normalization, not a mask) |
| **outcome throw during dep resolution** — engine effective-pom rebuild throws a Maven-strictness error (parent-packaging, `'version' is missing`, …) legacy tolerates | ~10 | **FIX (wiring)** dependency shadow reuses the effective-pom outcome-mask classification (`strictnessCategory`, broadened for the bare `'version' is missing` form) → returns legacy |
| **E2: transitive `requested` version** — legacy threads the declaring pom's raw declared version (null when managed); engine filled the effective version | 40 | **FIX (partial, L-P3-C-001)** `transitiveRequested` now consults the declaring pom in `servedBy` (remote-declarer case fixed); reactor/uncached-declarer residual OPEN |
| **E1: `requestedRef` root[] shift** — Maven's `ModelNormalizer` dedups duplicate `<dependencies>`; legacy keeps both, so the requested-list index differs | 45 | OPEN (requested-list shape; `matchRequested` also made last-wins to mirror legacy for genuine multi-candidate cases) |
| **C: `effectiveExclusions` completeness** — engine's `ExclusionAttributor` under-reports (wildcard expansion, DM-managed exclusions, reactor-crossing) — an engine DROP | 107 | **LEDGER L-P3-C-002 (OPEN, unmasked)** — a mask would hide a drop |
| **D: reactor-served transitive attribution** — dep reached through a reactor member lacks a `servedBy` entry → `repository`/`dated`/`licenses` null | 101 | **LEDGER L-P3-C-003 (OPEN, unmasked)** — engine DROP |
| **B: transitive-closure graph divergence** — engine vs legacy resolve a different closure (exclusion-application / mediation) | 81 | **LEDGER L-P3-C-004 (OPEN)** — per-case ground truth needed |
| **F: `$.errors` message text** — engine vs legacy failure-message wording | ~3 | **FIX (partial, L-P3-C-005)** engine collector now embeds the GAV (`"Unable to download POM: <gav>."`); normalized-text residual OPEN |

*Diff-block counts from the full-suite log (a multi-module test contributes several); not test counts.

**Honest verdict on the census:** it is **not at zero.** Reaching zero requires completing the mapper/collector for the
real-world corpus — `effectiveExclusions` attributor completeness (C), reactor-transitive `servedBy` attribution (D), and
resolving genuine closure divergences (B). Those are the majority of the residual, they are engine DROPs / value
divergences, and per the parity discipline they are **left red and unmasked** (the L-P2-E-003 precedent) rather than
hidden behind a mask that could conceal a drop. Each is root-caused and ledgered (L-P3-C-001..005) as the next slice's
work. No parity-correct behavior was weakened and no mask that could hide a drop was added.

## Gate verdicts

| gate | result |
|---|---|
| Ordering flake root-caused + fixed at the state-lifecycle level | **PASS** (`Assertions.withLocalRepository` lock; gone from the full concurrent run) |
| LEGACY full `:rewrite-maven:test` green (minus environment flakes) | **PASS** — 1580 tests, 3 failed, all the known `repo.jenkins-ci.org` 403 network flakes (identical on `main`); no functional regression |
| rewrite-gradle maven-engine path undisturbed (LEGACY) | **PASS** — `RemoveRedundantDependencyVersionsTest` + `AddDependencyTest` green |
| Routing modes ×3 + re-entrancy + L-P0-004 under MAVEN | **PASS** — `MavenEngineResolutionTest` (routing ×3, dispatch-guard, MAVEN-skips-legacy-lambda), `PartialFailureCompleteModelTest` green in MAVEN **and** LEGACY |
| Parity fixtures + engine suites (LEGACY) green after mapper/normalizer changes | **PASS** |
| SHADOW full `:rewrite-maven:test` zero unexplained | **NOT MET** — ~381 open across 5 root-caused dependency-graph divergence classes (L-P3-C-002/003/004 the bulk), left red and unmasked |

## New ledger rows / masks

Ledger (`doc/maven-resolution-ledger.md`): **L-P3-C-001** (transitive requested version, partial FIX), **L-P3-C-002**
(effectiveExclusions completeness, OPEN drop), **L-P3-C-003** (reactor-transitive attribution, OPEN drop), **L-P3-C-004**
(closure divergence, OPEN), **L-P3-C-005** (errors message shape, partial FIX), **L-P3-C-006** (event stream excluded per
DESIGN §5.6, NOTE), plus the **type-filter removal** row (ALIGN_TO_MAVEN, mapper applies no type filter). **No new masks**
added to `parity/masks.txt` — the local-repo collapse is a `SnapshotNormalizer` rule, and the open dependency-graph
classes are DROPs/divergences that must not be mask-hidden.

## Files changed (all `rewrite-maven`, uncommitted)

Main: `internal/engine/MavenEngineResolution.java` (dependency-graph routing, `DISPATCHING_DEPS`, shadow scope/errors
diff, outcome-mask reuse, `buildEngineEffective` extraction), `tree/MavenResolutionResult.java` (routed
`resolveDependencies`, `legacyResolveDependencies`), `tree/ResolvedPom.java` (routed per-scope `resolveDependencies`),
`internal/engine/DependencyGraphMapper.java` (`transitiveRequested` declaring-pom lookup, `matchRequested` last-wins),
`internal/engine/EngineDependencyCollector.java` (GAV in the not-found message), `internal/parity/SnapshotNormalizer.java`
(local-repo normalization), `Assertions.java` (concurrent-`withLocalRepository` lock). Tests:
`internal/engine/MavenEngineResolutionTest.java` (routing ×3 + guard + MAVEN-skips-lambda). Docs:
`doc/maven-resolution-ledger.md`.
