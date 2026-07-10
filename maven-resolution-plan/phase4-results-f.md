# Phase 4 slice F — descriptor-grade transitive model builds (bounded experiment)

2026-07-09, worktree `maven-resolution` (uncommitted). Slice E left maven mode **1.26–1.49× warm** at the camel tiers and
**~parity on the steady-state loop** (1.04–1.10×), with the warm floor named as "the inherent cost of routing each
descriptor through Maven's `DefaultModelBuilder`." This slice tests one bounded hypothesis: **is `InputLocation`
bookkeeping a meaningful part of that per-descriptor cost?** Maven's own descriptor reader builds with location tracking
**off**; the engine built every descriptor with it **on**. Split the grade and measure.

## Verdict (first)

**Descriptor-grade recovered no warm gap resolvable above the measurement noise floor — location tracking was not the
cost.** The crossover question is **not** closed. The change is correctness-neutral (proven identical) and is **kept**
because it aligns the transitive path with Maven's own `DefaultArtifactDescriptorReader.loadPom` (which the engine's
descriptor reader explicitly mirrors) and strictly removes allocation on the transitive path — but it is **not** a
performance lever. The warm floor remains exactly what slice E named: the `DefaultModelBuilder` inheritance-assembly +
interpolation + DM-import work per descriptor, not `InputLocation` allocation.

| tier | D warm × | E warm × | F warm × | E loopCtx × | F loopCtx × |
|---|---:|---:|---:|---:|---:|
| camel 150 | 1.85 | 1.42 | **within noise of E** (same-host A/B: E 1.60 ↔ F 2.34*) | 1.10 | ~1.16 (grade-independent) |
| camel 400 | 1.65 | 1.49 | **within noise of E** (same-host A/B: E 1.40 ↔ F 1.12) | 1.04 | ~0.90 (grade-independent) |

\* The F camel-150 arm caught a GC storm (warm `[4393, 5561, 7194, 5929, 11728]`, one 2.6× tail). E and F ratios **bracket
each other in both directions** across the A/B — the descriptor-grade delta is smaller than the per-run variance, so no
recovery is attributable. D/E columns are the published reference-host ratios (slice D/E); the F column is a **same-host**
E-vs-F A/B because this box would not reproduce the reference-host absolutes (see "Measurement caveat").

## 1. The change — a configuration split (not a refactor)

`EngineEffectivePom` gains a `locationTracking` flag (5-arg constructor; the existing 4-arg constructor keeps the
provenance-grade `true`, so every other caller is unchanged):

- **provenance-grade (`true`)** — the ROOT effective build (`MavenEngineResolution.buildEngineEffective`) and the
  imported-BOM attribution builds (`BomGavAttributor`, which shares the root's service instance). `EffectivePomMapper`
  joins managed entries to their declaring pom by `InputLocation` line/column, so these must track.
- **descriptor-grade (`false`)** — the transitive descriptor path (`EngineDescriptorReader`), mirroring Maven's
  `DefaultArtifactDescriptorReader.loadPom` (which sets `VALIDATION_LEVEL_MINIMAL`, `processPlugins=false`,
  `twoPhaseBuilding=false`, and never enables location tracking). The descriptor projection reads only
  `<dependencies>`/`<dependencyManagement>`/`<repositories>` into an `ArtifactDescriptorResult`.

Only `locationTracking` was split. Maven's reader also differs in profile/user-property handling, but those change
resolved *content* (versions via profile activation, interpolation) — flipping them would fail the correctness net by
design. `locationTracking` is the one setting that (a) Maven's reader disables and (b) is provably unconsumed downstream,
so it is the only content-safe lever.

Files: `EngineEffectivePom.java` (flag + 5-arg constructor), `EngineDescriptorReader.java` (passes `false`).

## 2. Safety check — nothing on the transitive path consumes `InputLocation`

Verified by inspection, not assumed. `grep getLocation|InputLocation` over the engine package: the **only** consumer is
`EffectivePomMapper` (root provenance build). `DependencyGraphMapper` and `EngineDependencyCollector` — the entire
transitive/collect path — have **zero** location reads. The descriptor projection (`populateResult`,
`recordDeclaredDependencies`) reads coordinates/scope/type only.

The two grades are also **cache-partitioned by construction**, so a location-stripped RAW model can never leak into a
provenance build: the descriptor path shares `session.getCache()`; the root and BOM builds use a fresh `null`-store cache
per build (`EngineEffectivePom` line 87). No provenance build ever reads the session model cache. The slice-E effective
memo (`EFFECTIVE_MEMO_KEY`) is provenance-only by construction — only `buildEngineEffective` populates or reads it, and it
always uses the provenance-grade root service; the descriptor path calls `EngineEffectivePom.build` directly and never
touches that memo. No memo key change is needed (and a constant grade token would be inert noise).

## 3. Correctness net — IDENTICAL, no field lost fidelity

`locationTracking` governs only whether `InputLocation` objects are attached during model building; it changes no
resolved dependency, management, version, or repository. The change is content-neutral **by construction**, and the net
confirms it:

- **Hermetic unit net (LEGACY default):** `parity.*` (incl. `EngineResolvedPomComparisonTest`,
  `EngineDependencyGraphComparisonTest`), `internal.engine.*`, `cache.*` — **265 tests, 0 failures, 0 errors, 1 skipped.**
  The parity comparison and fixture-SHADOW suites diff engine output against legacy with the ledgered masks; only the
  known ALIGN pins remain. No new diff.
- **Structural invariance:** `modelBuilds`/`descriptorReads` are byte-identical across provenance (E) and descriptor (F)
  grade at every tier (camel-150: 2416/2258; camel-400: 3215/2803). The split changes nothing structural — only
  per-build bookkeeping.
- **Corpus determinism on large real reactors (maven mode, descriptor-grade, hermetic twice-run):**
  `apache-maven-3.9.16` **green + byte-deterministic**; `apache-dubbo-3.2.10` (~190 modules) **green + byte-deterministic**
  (8209 ms two full resolutions, no SIGTERM). Both green in legacy mode too.

## 4. Measurement — the recovery is below the noise floor

### Measurement caveat
This run was on a **shared dev box under heavy ambient load** (15-min load avg peaked ~40, other non-owned JVMs at
100%+ CPU; it calmed to ~9 over the session). Absolute warm times ran 1.3–5× the slice-E reference host, and **maven mode
is markedly more GC/allocation-sensitive under memory pressure than legacy** — the legacy arm was rock-stable
(camel-150 `[2539, 2439, 2446, 2571, 2565]`) while the maven arm swung 2–3× iteration-to-iteration. The reference-host
absolutes could not be reproduced, so F is reported as a **same-host E-vs-F A/B** (revert → publish → measure → reapply →
publish → measure, adjacent in time), which isolates the descriptor-grade delta from the ambient load that both arms
share.

### Same-host A/B (camel-150/400, warmups=2/iters=5 at 150, 1/3 at 400; -Xmx2g; profiling on)

| tier | arm | legacy warm ms | maven warm ms | maven/legacy | maven warm array |
|---|---|---:|---:|---:|---|
| camel 150 | E (provenance) | 3035 | 4851 | 1.60 | [5367, 4851, 4317, 5275, 4335] |
| camel 150 | F (descriptor) | 2539 | 5929 | 2.34 | [4393, 5561, 7194, 5929, **11728**] |
| camel 400 | E (provenance) | 7198 | 10099 | 1.40 | [9818, 10099, 10394] |
| camel 400 | F (descriptor) | 8735 | 9754 | 1.12 | [12788, 9754, 8592] |

E and F straddle each other — F is *worse* at 150 (a GC storm) and *better* at 400 — so the sign of the "recovery" flips
with the noise. **No systematic descriptor-grade delta is resolvable.** The `modelBuildMs` phase counter (thread-summed,
last-iteration only) tells the same story: camel-400 E 8833 ↔ F 5578; camel-150 E 4426 ↔ F 14650 — swings of ±60% in
both directions from a one-line change that cannot possibly cause them. They are GC/last-iteration lottery, not signal.

### The loop is grade-independent (as theory predicts)
The steady-state loop does `modelBuilds=0` — every build is served from the slice-E effective memo — so descriptor grade
**cannot** touch it. Confirmed: loopCtx maven/legacy landed ~1.16× (150) and ~0.90× (400) in **both** arms, unchanged
from slice E's 1.10×/1.04×.

## 5. Per-build attribution (the fallback) — where the descriptor cost actually goes

Since the recovery was small, the plan's fallback: name where the per-descriptor time goes. From the cleanest camel-150
maven warm profile (E arm): `collectMs=2154`, `modelBuildMs=4426` (thread-summed across the parallel collect),
`projectMs=136`, `effectiveMs≈1` (memo-served after the first), warm total ≈ 4851 ms. Model building dominates: ~4426 ms
of thread-time over 2416 builds ≈ **1.8 ms per effective model**, and the collect wall (2154 ms) is essentially that
model-building running ~2-way parallel. That 1.8 ms/build is Maven's `DefaultModelBuilder` doing, per descriptor: RAW
parse → parent-inheritance assembly → property interpolation → `import`-scope DM resolution → effective merge.
`InputLocation` allocation is a small fraction of that pipeline — turning it off does not move the wall clock. **This is
slice E's floor, re-confirmed by direct attribution:** the warm gap is the model-builder pipeline itself, and closing it
needs the Phase-5 lighter descriptor-only projection (read just the DM/deps needed for `ArtifactDescriptorResult` without
materializing the full effective `Model`), not a build-request toggle. No further optimization in this slice.

## Verdict vs DESIGN §6 targets (unchanged from slice E)

- **loop ≥ legacy** — met/near-met: loopCtx ~1.0×, grade-independent.
- **≤ 1.5× at maven-15** — met (slice E; not re-measured cleanly here due to host load).
- **≥ legacy warm at camel-150+** — **still not met; no crossover.** Descriptor-grade did not recover it. The floor is
  the per-descriptor `DefaultModelBuilder` cost.

## Files changed (uncommitted, retained)

- `rewrite-maven/.../internal/engine/EngineEffectivePom.java` — `locationTracking` flag + 5-arg constructor (4-arg stays
  provenance-grade `true`).
- `rewrite-maven/.../internal/engine/EngineDescriptorReader.java` — transitive path builds descriptor-grade (`false`).
- `maven-resolution-plan/benchmarks/integrated-2026-07-09-descriptor-grade.{json,md}` (new).

Reproduce (same-host A/B): edit `EngineDescriptorReader`'s last constructor arg `false`↔`true`,
`:rewrite-maven:publishToMavenLocal`, then
`./gradlew --no-daemon -p maven-resolution-plan/phase0-tools integratedBenchmark -Pentries=apache-camel-4.4.0,150
-Pbench.warmups=2 -Pbench.iters=5 -Pbench.loop=5 -Pbench.heap=2g -Pengine.profile=true`.
