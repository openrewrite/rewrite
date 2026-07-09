# Integrated perf gate — 2026-07-09

Companion to `integrated-2026-07-09.json`. The **integrated** gate (DESIGN §6): both engines now live behind one facade
on one classpath, so the honest benchmark is `MavenParser` end-to-end in a single JVM — `ctx engine=legacy` vs
`engine=maven` over an **identical** reactor input set. This supersedes `spike/benchmark`'s split-JVM OLD/NEW harness,
which predated integration and measured the *raw, unadapted* pipeline (no LST mapping, warm internal caches). The gate is
the **ratio vs legacy on identical inputs**.

Harness: `maven-resolution-plan/phase0-tools` task `integratedBenchmark` (`IntegratedBenchmark`). Warm iterations are
zero-download: each mode is primed once through `RecordingHttpSender` RECORD, then every measured iteration replays from
the store with no network. Warm = median of 5 after 2 warm-ups, fresh `MavenPomCache` per iteration. Loop = warm-cache
re-resolution (one shared warm cache, re-resolve the full reactor repeatedly — the UpdateMavenModel steady state).
Environment: Mac16,7, Temurin 17.0.6, rewrite-maven 8.87.0-SNAPSHOT (worktree, incl. slice-C), resolver 2.0.20 +
model-builder 3.9.16 shaded.

## Ratio table (warm full-reactor median + warm-cache re-resolution loop; identical module set per tier)

| corpus/tier | modules | heap | legacy warm ms | maven warm ms | **legacy/maven** | maven slower | legacy loop ms | maven loop ms | loop slower | legacy heap MB | maven heap MB |
|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| maven full | 15 | 2g | 149 | 690 | **0.22×** | 4.6× | 102 | 196 | 1.9× | 164 | 1021 |
| camel 50 | 50 | 768m | 1,951 | 4,408 | **0.44×** | 2.3× | 1,006 | 2,404 | 2.4× | 564 | 515 |
| camel 150 | 150 | 768m | 2,766 | 7,274 | **0.38×** | 2.6× | 1,749 | 4,122 | 2.4× | 562 | 580 |
| camel 400 | 400 | 768m | 4,647 | 16,872 | **0.28×** | 3.6× | 3,415 | 12,819 | 3.8× | 594 | 669 |

Per-module marginal (warm ms/module): legacy `9.9 → 39.0 → 18.4 → 11.6`; maven `46.0 → 88.2 → 48.5 → 42.2`. Legacy
settles at ~12–18 ms/module at scale; **maven never drops below ~42 ms/module** — there is no crossover.

## Verdict per tier (against DESIGN §6)

DESIGN §6 set the expectation that the adapter layer is "built inside that [2–4×] headroom" the raw pipeline had over
legacy, so the integrated engine should stay at least competitive. **Every measured tier fails that expectation.**

- **maven-15 — FAIL.** maven mode is **4.6× slower** warm (690 vs 149 ms) and allocates **6×** the heap (1021 vs 164
  MB). The warm-cache loop is 1.9× slower, so cold model-building is the larger share here, but not the whole story.
- **camel-50 — FAIL.** maven mode is **2.3× slower** warm and **2.4× slower** on the warm-cache loop.
- **camel-150 — FAIL.** maven mode is **2.6× slower** warm, **2.4× slower** loop.
- **camel-400 — FAIL (worst).** maven mode is **3.6× slower** warm (16.9 s vs 4.6 s) and **3.8× slower** on the loop; the
  gap *widens* at the largest tier rather than converging.

This **contradicts the raw-pipeline baseline** (`baseline-2026-07-08.md`), where NEW-N1 beat OLD at every camel tier
(marginal ~2.3 ms/module). The integrated adapter layer costs ~40 ms/module — it consumes all of the raw headroom and
more.

## Dominant costs (measurement, no speculative optimization)

1. **Per-module adapter + model-building cost (~40 ms/module, vs legacy's ~12–18 ms/module at scale).** The
   warm-cache loop is *also* 2.4–3.8× slower, so this is not merely cold-cache model building: the steady-state
   collect-then-project path is itself heavy. Contributors: Maven 3.9's `ModelCacheTag` deep-clones on every cache hit
   (baseline measured ~8.3 clones/module on camel), the single verbose collect projected per-scope by
   `DependencyGraphMapper`, and the shaded object graph.
2. **Allocation.** maven-15 used **1021 MB** vs legacy's **164 MB** (~6×). This drives GC pressure and, at scale, the FD
   leak below.

## Flip-blocker — file-descriptor accumulation

maven mode **exhausts the OS per-process FD cap (`kern.maxfilesperproc` = 92,160)** within a single large-reactor parse:
camel-40/47/50 crash with `Too many open files` at `-Xmx2g` (both with and without the gradle daemon; shell
`ulimit -n` = 1,048,576, so this is not a ulimit artifact). The handles are **GC-reclaimable `FileInputStream`s** from
the materialized-pom / model-source read path — the same tiers *complete* at `-Xmx768m` because frequent GC reclaims the
FDs before the cap. BOM-heavy modules (the 47 camel pom-packaging parents/BOMs) leak fastest (deep import chains → many
descriptor reads). maven-15 (shallow deps) stays under the cap at 2g.

Consequence for these numbers: the camel rows run at `-Xmx768m` (the smallest heap that survives), so the **maven camel
figures are a GC-pressured upper bound** — the true 2g numbers would be somewhat faster but crash. The ratio within each
tier is same-heap for both engines and therefore fair. Legacy is heap-insensitive (camel-50 warm 1,951 ms at 768m ≈
1,962 ms at 2g).

**An FD-lifecycle fix in the engine's descriptor/model file-handle path is required before maven mode can resolve a
BOM-heavy reactor at production heap sizes.** Not attempted in this measurement slice.

## Reproduce

```
./gradlew :rewrite-core:publishToMavenLocal :rewrite-xml:publishToMavenLocal :rewrite-java:publishToMavenLocal \
  :rewrite-properties:publishToMavenLocal :rewrite-yaml:publishToMavenLocal \
  :rewrite-maven-engine:publishToMavenLocal :rewrite-maven:publishToMavenLocal
./gradlew --no-daemon -p maven-resolution-plan/phase0-tools integratedBenchmark \
  -Pentries=apache-camel-4.4.0,150 -Pbench.warmups=2 -Pbench.iters=5 -Pbench.loop=15 -Pbench.heap=768m
```

First run per tier primes over the network (store-first `RecordingHttpSender`; camel-50's initial legacy prime was ~30
min, subsequent tiers reuse the store); re-runs are network-free.
