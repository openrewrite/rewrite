# Phase 4 slice E — structural perf: the effective-model memo

2026-07-09, worktree `maven-resolution` (uncommitted). Slice D left maven mode 1.47–1.85× warm and **1.46–2.64× on the
steady-state loop** (worst at camel-400), with two named structural residuals. This slice attacks them with
phase-attribution discipline, lands the one that measurement justified, and delivers the definitive floor for the one it
did not. **The loop residual is eliminated; the warm floor is proven.**

## 1. Method — measure first (camel-150)

Baseline profile of a warm camel-150 parse (`-Dengine.profile=true`, my host, warmups=1/iters=3):

| phase | baseline warm | note |
|---|---:|---|
| effective (graph-path root build) | 617 ms | |
| collect | 1,927 ms | descriptor model builds live here |
| project (mapper) | 119 ms | |
| modelBuilds | 2,566 | ≈ 17/module |
| descriptorReads | 2,258 | ≈ 15/module, deduped across the reactor by aether's DataPool |

The slice-D handoff named two residuals: (#1) each transitive descriptor is built through Maven's `DefaultModelBuilder`;
(#2) the root effective build keeps a fresh model cache. It also flagged the loop as worse than warm. I added a
`PROFILE-LOOP` attribution of the loopCtx steady state and found the loop's dominant cost was a **repeated root rebuild**:
`buildEngineEffective` is called twice per module per resolution (the `ResolvedPom.resolve` facade and the
dependency-graph facade), and the re-resolution loop rebuilds every module every cycle — none of it cached.

## 2. The fix — E-1: effective-model memo above `ModelBuilder`

`MavenEngineResolution.buildEngineEffective` now memoizes the mapped `EngineEffective` (frozen `ResolvedPom` + effective
`Model` + repositories/settings/reactor) on the `ExecutionContext` (Caffeine, `maximumSize(4096)`), keyed by
`(reactorEpoch | activeProfiles | requestedGav | xmlLength:xmlHash)`. A memo hit returns a result built from byte-identical
input under the same profiles and epoch, so it is behaviourally identical to a rebuild by construction. The **global
reactor epoch** (bumped by every `UpdateMavenModel` re-resolution) is in the key, so any reactor mutation invalidates the
whole memo — a mutated parent is never served stale to an inheriting sibling.

Files: `MavenEngineResolution.java` (memo + `effectiveMemoStats` test hook), `EngineProfiler.java` (memo hit/miss
counters + report), `phase0-tools/IntegratedBenchmark.java` (PROFILE-LOOP attribution of the loopCtx iteration).

### Perf-correctness gate (`EngineReResolutionTest`)
- `mutateParentThenReResolveChildUnderMavenPicksUpTheMutation` (pre-existing) — the epoch bump invalidates the memo; the
  child re-resolves the mutated managed version (2.0). **Fresh value visible.**
- `memoServesUnmutatedSiblingsAndInvalidatesTheMutatedParent` (new) — a sibling re-resolved at a stable epoch is served
  from the memo (`CacheStats.hitCount()` grows); after the parent mutation + epoch bump the dependent still picks up the
  fresh value. **Memo actually hit AND invalidation correct.**

## 3. Before → after (all tiers, same-JVM within-tier ratio)

| tier | warm ×slower D → E | loopCtx ×slower D → E |
|---|---:|---:|
| maven 15 | 2.40× → **1.42–1.62×**† | 1.68× → **1.06×** |
| camel 50 | 1.47× → **1.26×** | 1.46× → **1.05×** |
| camel 150 | 1.85× → **1.42×** | 1.98× → **1.10×** |
| camel 400 | 1.65× → **1.49×** | 2.64× → **1.04×** |

† maven-15 warm is noisy at 15 modules (~0.2 s absolute, 233–407 ms; a clean run reads 1.42×); loopCtx 1.06× is stable.

Loop attribution after E-1 (`PROFILE-LOOP`): **`modelBuilds=0` at every tier**, all `buildEngineEffective` calls served
from the memo (camel-400: 800/0 hits). The loopCtx delta over legacy is now 6–166 ms (≤ 0.4 ms/module) — the aether
verbose collect + graph projection that both engines run on every re-resolution. **Item 3 is resolved and named: the
loop's extra work was the non-memoized per-iteration root rebuild.** Heap dropped to **≤ legacy at camel-150/400**
(0.92–0.98×) — the memo also cuts allocation by not re-assembling models. FD stable at 60.

## 4. Item 1 (share the root model cache) — measured unnecessary, not implemented

Item 1 would decouple `servedBy`/declaring-pom attribution from cache-freshness so the root build shares the model cache.
Profiler evidence says it has **no target left on the corpus**, and it carries real attribution-correctness risk the
parity fixtures guard, so it was not implemented:

- **camel (the headline tiers):** every parent/BOM is *in-reactor* — served by `ReactorWorkspace` and, by design
  (`EngineModelCache` skips reactor GAVs, phase2-b1 deviation #1), never model-cached regardless of store sharing. A
  shared root store changes nothing for camel.
- **the transitive path is already shared** (D-3: descriptor reads use `session.getCache()`); item 1 only concerns the
  root build's own `<parent>` chain.
- **the memo already removes the per-module double root build**, which was the larger root-path cost.
- **maven-15** has one external super-parent (`org.apache:apache`) that rebuilds ~15×/warm-parse, but it is a tiny pom
  immeasurable against the ±30 % noise at that scale, and maven-15 already meets its target (loopCtx 1.06×).

Implementing it would mean a durable ctx-lifetime `servedBy` union threaded into `EffectivePomMapper.declaringPom` and
`BomGavAttributor` (with an O(reactor-externals) scan or a new index), risking the exact `servedBy`/`bomGav`/`requestedRef`
exactness the differential suites protect — for zero measurable corpus benefit. Disciplined call: don't gold-plate; ship
the general fix (the memo) and record the measurement.

## 5. Verdict vs DESIGN §6 targets

| target | result |
|---|---|
| loop ≥ legacy | **near met** — loopCtx 1.04–1.10× (from 1.46–2.64×); residual is the verbose collect both engines run |
| ≤ 1.5× at maven-15 | **met** — loopCtx 1.06×; warm 1.42× clean read |
| ≥ legacy warm at camel-150+ | **not met (no crossover)** — warm 1.26–1.49×, floored by the per-descriptor ModelBuilder count |

### The warm floor — one-paragraph assessment
A warm (cold-cache, first-parse) camel-400 build constructs ~3,215 effective models: 400 roots + 2,803 unique transitive
descriptors (already deduplicated across the reactor by aether's `DataPool`). Legacy derives the same
dependency/management data with its lighter hand-rolled `RawPom`→`ResolvedPom`; routing each descriptor through Maven's
`DefaultModelBuilder` (full inheritance assembly, interpolation, profile injection, DM import per build) is inherently
heavier, and that per-descriptor delta is the entire remaining 1.26–1.49× warm gap. No caching layer closes it on the
warm path, because the warm measurement is a fresh `ExecutionContext` per iteration — a ctx-lifetime memo cannot persist,
and a process-lifetime descriptor cache would break resolution isolation and the pinned/epoch semantics. **The only design
change that would close it is replacing the descriptor reader's full `EngineEffectivePom.build` with a lighter
descriptor-only projection** — reading just the effective `<dependencies>`/`<dependencyManagement>` needed to populate the
`ArtifactDescriptorResult`, without materializing the whole effective `Model` (plugins, repositories, build, profiles).
That is a Phase-5-scale rewrite of the collect's model path, not a caching fix, and it trades against the parity guarantee
that a descriptor is resolved by the *exact same* machinery as the root. On the steady-state loop — the axis that matters
for `UpdateMavenModel` recipe runs — maven mode is already at parity.

## Files changed (uncommitted)

- `rewrite-maven/.../internal/engine/MavenEngineResolution.java` — effective-model memo (Caffeine, ctx-lifetime); memo
  key; `effectiveMemoStats` test hook.
- `rewrite-maven/.../internal/engine/EngineProfiler.java` — `effectiveMemo(hit/miss)` counters + report/reset.
- `rewrite-maven/.../test/.../internal/engine/EngineReResolutionTest.java` — new memo perf-correctness test.
- `maven-resolution-plan/phase0-tools/.../IntegratedBenchmark.java` — `PROFILE-LOOP` loopCtx phase attribution.
- `maven-resolution-plan/benchmarks/integrated-2026-07-09-final.{json,md}` (new).

Hermetic gates green (shipping/default config): `:rewrite-maven-engine:test`, `parity.*`, `internal.engine.*`, `cache.*`.
Shadow-global has 25 pre-existing failures unrelated to this slice (identical class/count with the memo stashed) — an
artifact of the global `-D` flipping the legacy-oracle `ParityHarness` resolutions into shadow.
