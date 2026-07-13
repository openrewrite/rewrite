# Integrated perf gate — 2026-07-09 FINAL (slice E structural perf)

Companion to `integrated-2026-07-09-final.json`. Re-run of the integrated gate (DESIGN §6) after the Phase-4 slice-E
structural fix: an **effective-model memo above `DefaultModelBuilder`**. Same harness (`IntegratedBenchmark`), same
corpora, all tiers at `-Xmx2g`. Supersedes `integrated-2026-07-09-post.{json,md}` (slice D).

Absolute times differ from the slice-D-post file (a different host run); the meaningful figure is the **same-JVM
within-tier ratio**, re-measured here for both engines.

## The fix — E-1: effective-model memo

`MavenEngineResolution.buildEngineEffective` now memoizes the mapped `EngineEffective` (the frozen `ResolvedPom` + the
effective `Model` + repositories/settings/reactor) on the `ExecutionContext` (Caffeine, `maximumSize(4096)`), keyed by
`(reactorEpoch | activeProfiles | requestedGav | xmlLength:xmlHash)`. Two call sites build the *same* module's effective
pom every resolution — the `ResolvedPom.resolve` facade and the dependency-graph facade — and a re-resolution loop
rebuilds every module every cycle. The memo serves all of those repeats without touching `DefaultModelBuilder`. The
global epoch (bumped by every `UpdateMavenModel` re-resolution) invalidates the whole memo on any reactor mutation, so a
mutated parent is never served stale to an inheriting sibling (perf-correctness gate: `EngineReResolutionTest`).

## Ratio table (warm full-reactor median + ctx-reused steady-state loop; identical module set per tier; -Xmx2g)

`maven ×slower` = maven/legacy. `loopCtx` = re-resolution loop reusing ONE `ExecutionContext` (the true UpdateMavenModel
steady state). Parenthetical is the slice-D-post ratio.

| tier | modules | legacy warm ms | maven warm ms | maven ×warm | legacy loopCtx ms | maven loopCtx ms | ×loopCtx | heap M/L |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| maven full | 15 | 161 | 261† | **1.62×** (2.40) | 108 | 114 | **1.06×** (1.68) | —‡ |
| camel 50 | 50 | 1,871 | 2,349 | **1.26×** (1.47) | 1,060 | 1,116 | **1.05×** (1.46) | 1.22× |
| camel 150 | 150 | 2,687 | 3,816 | **1.42×** (1.85) | 1,669 | 1,835 | **1.10×** (1.98) | 0.98× |
| camel 400 | 400 | 5,700 | 8,498 | **1.49×** (1.65) | 3,540 | 3,695 | **1.04×** (2.64) | 0.92× |

† maven-15 warm is noisy (absolute ~0.2 s, 233–407 ms spread); a cleaner dedicated run read 213 ms = 1.42×. The
steady-state loopCtx 1.06× is the stable figure. ‡ maven-15 `peakHeapMb` is reserved-not-live on a sub-300 ms/2 g run.

## Loop-path attribution (item 3) — the steady state no longer rebuilds

The `PROFILE-LOOP` line (new this slice) attributes the last loopCtx iteration:

| tier | loop collectMs | loop projectMs | loop modelBuilds | loop memo hit/miss |
|---|---:|---:|---:|---:|
| maven 15 | 6 | 1 | **0** | 30/0 |
| camel 50 | 493 | 43 | **0** | 100/0 |
| camel 150 | 627 | 89 | **0** | 300/0 |
| camel 400 | 1,011 | 283 | **0** | 800/0 |

The slice-D loop diagnosis ("camel-400 loop 2.64× — worse than warm; re-resolution does extra work") is resolved and
named: the extra work was the per-iteration root effective rebuild (`buildEngineEffective` on a fresh model cache,
twice per module). The memo makes **every** loop build a hit — `modelBuilds=0` — leaving only the aether verbose collect
(627–1,011 ms) and graph projection, both of which run on every re-resolution and which legacy mirrors. The loopCtx
delta over legacy is now 6–166 ms (≤ 0.4 ms/module).

## Warm-path attribution — the floor

The memo removes the *second* root build per module (warm `modelBuilds` drop by one-per-module) but cannot help the
*first* build of each module, so warm improves only modestly. The warm cost is the per-descriptor model-building count:

| tier | warm collectMs | warm modelBuilds | descriptorReads | memo hit/miss |
|---|---:|---:|---:|---:|
| camel 50 | 1,488 | 2,130 | 2,075 | 50/50 |
| camel 150 | 1,912 | 2,416 | 2,258 | 150/150 |
| camel 400 | 2,894 | 3,215 | 2,803 | 400/400 |

A warm parse of 400 modules builds ~3,215 effective models — 400 root + 2,803 unique transitive descriptors (already
deduplicated across the reactor by aether's `DataPool`). Legacy derives the same information with its lighter hand-rolled
`RawPom`→`ResolvedPom`. **This is the achievable warm bound** (residual #1, slice D): the inherent cost of routing each
descriptor through Maven's `DefaultModelBuilder`. It is not addressable by a ctx-lifetime memo (the warm measurement uses
a fresh ctx per iteration) nor by sharing the root model cache (camel's parents are in-reactor).

## Verdict vs DESIGN §6 targets

- **loop ≥ legacy** — near met: 1.04–1.10× (from 1.46–2.64×). Effectively at parity; the residual is the verbose collect.
- **≤ 1.5× at maven-15** — met: loopCtx 1.06×; warm 1.42× on a clean read.
- **≥ legacy warm at camel-150+** — not met (no crossover): warm 1.26–1.49×, floored by the per-descriptor ModelBuilder
  count. See the one-paragraph assessment in `phase4-results-e.md` for what design change could close it.

Heap is now **≤ legacy at camel-150/400** (0.92–0.98×) — the memo also cuts allocation by not re-assembling models. FD
stable at 60 every tier.

## Reproduce

```
./gradlew :rewrite-core:pTML :rewrite-xml:pTML :rewrite-java:pTML :rewrite-properties:pTML :rewrite-yaml:pTML \
  :rewrite-maven-engine:pTML :rewrite-maven:pTML
./gradlew --no-daemon -p maven-resolution-plan/phase0-tools integratedBenchmark \
  -Pentries=apache-camel-4.4.0,150 -Pbench.warmups=1 -Pbench.iters=3 -Pbench.loop=5 -Pbench.heap=2g \
  -Pengine.profile=true      # prints PROFILE (warm) and PROFILE-LOOP (steady state)
```
