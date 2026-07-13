# Integrated perf gate — 2026-07-09 POST (slice D lifecycle fixes)

Companion to `integrated-2026-07-09-post.json`. Re-run of the integrated gate (DESIGN §6) after the Phase-4 slice-D
lifecycle fixes. Same harness (`IntegratedBenchmark`), same corpora, **now all tiers at -Xmx2g** — the FD fix (D-1)
makes 2 g viable for BOM-heavy reactors, which crashed before. Supersedes `integrated-2026-07-09.{json,md}` (the failing
gate).

The five fixes (see `phase4-results-d.md` for full attribution):
- **D-1** resolver named-lock factory `file-lock` → `rwlock-local` — removes the per-artifact `.lock` FileChannel that
  *was the FD leak* and the dominant collect cost.
- **D-2** shared singleton `DefaultModelBuilder` — one super-POM read instead of one per model build.
- **D-3** descriptor-read model cache shared across the collect (`session.getCache()`) — shared parents/BOMs build once.
- **D-4** byte-backed `StringModelSource` — drops the materialized `.pom` file write + `FileInputStream`.
- **D-5** ctx-wide cache of `BomGavAttributor` effective-management keys (pure function of the BOM gav) — collapses the
  superlinear O(modules × BOMs) BOM rebuild in the root effective phase (camel-400 effective 8.9 s → 3.0 s).

## Ratio table (warm full-reactor median + ctx-reused steady-state loop; identical module set per tier; -Xmx2g)

`legacy/maven` > 1 means maven is faster. `loopCtx` = re-resolution loop reusing ONE `ExecutionContext` (engine +
DataPool warm) — the true UpdateMavenModel steady state.

| corpus/tier | modules | legacy warm ms | maven warm ms | **legacy/maven** | maven ×slower | legacy loopCtx ms | maven loopCtx ms | loopCtx ×slower | heap M/L | FD before→after |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| maven full | 15 | 154 | 369 | **0.42×** | 2.40× | 106 | 178 | 1.68× | —† | 57→60 |
| camel 50 | 50 | 1,876 | 2,757 | **0.68×** | 1.47× | 998 | 1,457 | 1.46× | 1.19× | 57→60 |
| camel 150 | 150 | 2,827 | 5,237 | **0.54×** | 1.85× | 1,665 | 3,296 | 1.98× | 1.13× | 57→60 |
| camel 400 | 400 | 7,305 | 12,042 | **0.61×** | 1.65× | 3,620 | 9,546 | 2.64× | 1.37× | 57→60 |

Protocol: maven-15 + camel-50 warmups=2/iters=5; camel-150/400 warmups=1/iters=3. All four tiers include every fix.
Legacy/maven within a tier are one JVM run (fair same-load ratio).

† maven-15 `peakHeapMb` is max-used-before-GC on a ~370 ms run under 2 g — reserved-not-live, not a meaningful ratio at
this scale. Representative allocation is the camel figure: **1.13–1.37× legacy** (within the ~2× target).

## Verdict vs slice C

Slice C (FAIL every tier, camel forced to 768m because 2g crashed): maven 4.6× / 2.3× / 2.6× / 3.6× warm; loop 1.9–3.8×;
heap to 6×; FD leak. **Post:** maven **1.47–2.4× warm** (tight: 1.47× / 1.85× / 1.65× on camel 50/150/400), **1.46–2.64×
steady-state loop**, **heap 1.13–1.37×**, **FD flat at every tier** (the BOM-heavy camel reactors that crashed now
complete at 2 g with a stable descriptor count). The gate's "per-iteration/per-module reconstruction" diagnosis is
confirmed and eliminated. No crossover (maven does not beat legacy warm) — the residual gap is the inherent per-descriptor
Maven `DefaultModelBuilder` cost (N1 builds one effective model per transitive dependency) plus the root effective build
staying on a fresh model cache for `servedBy` completeness; both are named with evidence in `phase4-results-d.md`.

## Reproduce

```
./gradlew :rewrite-core:pTML :rewrite-xml:pTML :rewrite-java:pTML :rewrite-properties:pTML :rewrite-yaml:pTML \
  :rewrite-maven-engine:pTML :rewrite-maven:pTML     # pTML = publishToMavenLocal
./gradlew --no-daemon -p maven-resolution-plan/phase0-tools integratedBenchmark \
  -Pentries=apache-camel-4.4.0,150 -Pbench.warmups=1 -Pbench.iters=3 -Pbench.loop=5 -Pbench.heap=2g \
  -Pengine.profile=true            # add -Pbench.fdwatch=5000 to dump the open-FD histogram near a leak
```
