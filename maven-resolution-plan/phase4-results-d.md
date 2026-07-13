# Phase 4 slice D — make maven mode PASS the performance gate

2026-07-09, worktree `maven-resolution` (uncommitted). Slice C left maven mode 2.3–4.6× slower than legacy at every
tier with an FD leak that crashed BOM-heavy reactors at production heap. This slice root-causes both with a profiler's
discipline (per-phase timers + FD counters added to `IntegratedBenchmark`, JFR allocation profiling, `lsof`/`jmap`
attribution), fixes the lifecycle bugs, and re-runs the gate. **The failure was per-descriptor reconstruction, not
inherent adapter cost** — confirmed by measurement.

## 1. The five fixes (each measured before/after)

All five are lifecycle/config fixes on the maven-mode hot path; the legacy path is untouched. Every hermetic suite is
green after all of them (engine, `internal.engine.*`, `parity.*`, `cache.*`) — the parity fixture differentials are the
correctness net for the cache-sharing changes.

### D-1 — resolver named-lock factory: `file-lock` → `rwlock-local` (THE fix)
`MavenEngine.newSession` + `EngineDependencyCollector.newSession` now set
`aether.syncContext.named.factory=rwlock-local`. The resolver's default named-lock factory is **file-based**: it opens a
`FileChannel`/`FileLock` on a `lrm/.locks/artifact~g~a~pom~v.lock` file for **every artifact** during the verbose
collect. On a BOM-heavy reactor the concurrent collect holds thousands of these channels at once, and because each
fresh-ctx parse builds new (never-closed) `RepositorySystem`s the handles accumulate to the 92,160 per-process cap —
**this is the FD leak**. It is *also* the dominant collect cost: the per-descriptor file-lock acquire/release is pure I/O.
The private per-run scratch LRM has no cross-process contention, so in-JVM `ReentrantReadWriteLock`s are both correct
(they still serialize the collector's worker threads) and file-free.

- **FD:** `lsof` at the moment of crash showed the open set was almost entirely `lrm/.locks/*.lock` channels.
- **Impact (maven-15):** collect phase 468 ms → **80 ms**; FD after warm 655 → **60** (stable).

### D-2 — share the model builder across builds (`DefaultSuperPomProvider` jar-read churn)
`EngineEffectivePom.build` did `new EngineModelBuilderFactory().newInstance()` per build (~9 builds/module). Each new
builder carries its own `DefaultSuperPomProvider`, which re-reads the super-POM resource from the jar (`getResourceAsStream`
→ `JarURLConnection`) and re-parses it on *every* build. Maven treats `DefaultModelBuilder` as a reusable, thread-safe
singleton (per-build state lives on the request); `EngineModelBuilderFactory.shared()` now returns one instance. The
first single-threaded root build warms the super-POM cache before any concurrent collect reads it.

- **Evidence (JFR alloc):** the jar-URL/`ZipFileInputStream` allocations traced 100% to `DefaultSuperPomProvider.getSuperModel`.
- **Impact:** live `ZipFileInputStream`/`JarLoader$2` instances 276/552 → **2/2**.

### D-5 — cache `BomGavAttributor` effective-management keys ctx-wide
`BomGavAttributor.effectiveManagementKeys(bomKey)` builds a directly-imported BOM's effective model to read its managed
`g:a:c:t` set — and did so **per importing module**, on a fresh model cache. On a large reactor where many modules import
the same BOMs this is O(modules × BOMs), and it dominated the root effective phase superlinearly (effective
25→194→779→**8915 ms** across 15→50→150→400). The key set is a pure function of the BOM gav, so it is now cached on the
`ExecutionContext` keyed by `(reactorEpoch, bomGav)` — the epoch guards against a stale reactor-BOM after an in-place
`UpdateMavenModel` re-resolution.

- **Impact (camel-400):** effective phase **8915 → 3049 ms**, `modelBuilds` 6831 → 3615, warm **20.5 s → 12.0 s**, and
  the GC-thrash variance ([20544,16060,27555]) collapsed to [12042,11233,12391].

### D-3 — share the model cache across descriptor reads
`EngineEffectivePom` took a fresh `DefaultRepositoryCache` per build, so **every** shared parent/BOM was rebuilt for
**every** dependent (0 model-cache hits / 248 misses at maven-15). Descriptor-read builds now share the session's
`RepositoryCache` (`session.getCache()`), which Maven's own RAW/IMPORT clone-on-hit makes safe, and gav→repo attribution
is preserved because it is accumulated at the collector level (`cc.getServedBy()`), not per build. The **root** project
build keeps a fresh store (its `servedBy` stands alone) — a `@Nullable RepositoryCache modelCacheStore` ctor param selects.

- **Impact (camel-50):** model-cache hit rate **72 %** (7937 hit / 3052 miss); the redundant parent/BOM rebuilds that
  did not amortize now do.

### D-4 — byte-backed model sources (drop `FileModelSource`/materialize)
`CacheBridge.resolvePom` returned a `FileModelSource` over a **materialized** scratch `.pom` file (a file write + a
`new FileInputStream` per resolution). A repository parent/BOM needs no local file — the model builder reads bytes — so it
now returns a `StringModelSource` (the reactor path already did). Removes the per-resolution file write and the
`FileInputStream` the reader opened on it. (Secondary to D-1 for FD, but removes real I/O and the `materialize`/
`materializeDir` machinery from `CacheBridge`.)

## 2. Phase attribution — maven-15, one warm iteration (before → after)

Instrumented via `EngineProfiler` (`-Dengine.profile=true`), median warm iteration:

| phase | before (ms) | after (ms) | note |
|---|---:|---:|---|
| effective (root pom) | 68 | 26 | root builds |
| **collect** | **468** | **80** | D-1 removed per-descriptor file locking |
| project (mapper) | 8 | 3 | |
| model-build (CPU, all threads) | 898 | 226 | D-2/D-3 |
| model-cache hit/miss | 0/248 | 76/172 | D-3 (shallow reactor → modest reuse) |
| FD after warm | 655 | 60 | D-1/D-4 |

`modelBuilds=136` is unchanged (one effective model per transitive descriptor is the N1 design); the win is that each
build is now far cheaper and the collect no longer pays file-lock I/O per descriptor.

## 3. Final ratio table (all tiers at -Xmx2g — the FD fix makes 2g viable for BOM-heavy reactors)

`legacy/maven` > 1 means maven is faster; "×slower" = maven/legacy. `loopCtx` is the true UpdateMavenModel steady state
(one reused `ExecutionContext`, engine + DataPool warm — a recipe run reuses one ctx). Full numbers:
`benchmarks/integrated-2026-07-09-post.{json,md}`.

| tier | modules | legacy warm ms | maven warm ms | **legacy/maven** | maven ×slower | legacy loopCtx | maven loopCtx | loopCtx ×slower | heap M/L | FD before→after |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| maven full | 15 | 154 | 369 | **0.42×** | 2.40× | 106 | 178 | 1.68× | —† | 57→60 |
| camel 50 | 50 | 1,876 | 2,757 | **0.68×** | 1.47× | 998 | 1,457 | 1.46× | 1.19× | 57→60 |
| camel 150 | 150 | 2,827 | 5,237 | **0.54×** | 1.85× | 1,665 | 3,296 | 1.98× | 1.13× | 57→60 |
| camel 400 | 400 | 7,305 | 12,042 | **0.61×** | 1.65× | 3,620 | 9,546 | 2.64× | 1.37× | 57→60 |

Compare slice C (all FAIL, at -Xmx768m for camel because 2g crashed): maven-15 4.6× / camel-50 2.3× / camel-150 2.6× /
camel-400 3.6× warm; 1.9–3.8× loop; heap up to 6×; FD leak crashed camel at 2g. **After: maven is 1.47–2.4× warm (camel
1.47–1.85×), 1.46–2.64× on the steady-state loop, heap 1.13–1.37× at camel scale, and FD is flat at every tier —
including the BOM-heavy camel reactors that crashed before.** Protocol: maven-15 + camel-50 warmups=2/iters=5;
camel-150/400 warmups=1/iters=3; legacy/maven within a tier are one JVM run.

† maven-15 `peakHeapMb` is `max used heap before GC` on a ~370 ms run under a 2 g heap — reserved-not-live, not a
meaningful allocation ratio at this scale. The camel tiers (heap exercised relative to work) give the representative
figure: **1.13–1.37× legacy**, inside the ~2× target.

**Against the DESIGN §6 targets:** allocation 1.13–1.37× (✅ within 2×); FD flat (✅); loop steady-state 1.46–2.64× — near
legacy at 50/150 (1.46–1.98×), 2.64× at 400. Warm ratio: maven-15 2.4× (misses the ≤1.5× small-reactor target — fixed
per-reactor ModelBuilder overhead amortizes poorly over 15 modules) and camel **1.47–1.85×** — no crossover, but
competitive rather than 2.3–3.6× behind, and *monotone* rather than diverging with scale (slice C widened to 3.6× at 400;
post it is 1.65× at 400).

### Residual warm cost, named with evidence (no speculative micro-optimization)
Two structural costs remain, both measured:
1. **Maven's `DefaultModelBuilder` is heavier per invocation than legacy's hand-rolled `RawPom`→`ResolvedPom`.** The N1
   design builds one effective model per transitive descriptor (`descriptorReads` ≈ 7/module, `modelBuilds` ≈ 9/module on
   camel). D-2/D-3/D-5 made each build far cheaper and lifted model-cache reuse to 60–72 %, but the *count* is structural
   to "resolve each descriptor through Maven's model builder." Closing it would mean caching the built
   `ArtifactDescriptorResult` per gav across the reactor's collects.
2. **The root effective build keeps a fresh model cache** (D-3), so a module's own external parents rebuild per module.
   This is deliberate: `EffectivePomMapper.declaringPom` reads `servedBy` for external parents (only reactor parents are
   special-cased), so a shared cache that skipped `resolvePom` would leave `servedBy` incomplete and mis-attribute
   dependency repositories — the parity fixtures catch exactly this. Sharing it safely needs collector-style ctx-level
   `servedBy` accumulation for the root path.

Both are engine-design changes larger than a lifecycle fix, and out of this slice's scope. **Every per-iteration /
per-module *reconstruction* the gate flagged — engine bootstrap, file locks, super-POM re-reads, redundant BOM rebuilds,
FD accumulation — is now gone;** what remains is the genuine adapter cost of routing each descriptor through Maven's model
builder.

## 4. FD pin

The FD leak is fixed at the source (D-1): the resolver no longer opens per-artifact lock files. Evidence:
- `IntegratedBenchmark` now records `fdBefore`/`fdAfter` (via `com.sun.management.UnixOperatingSystemMXBean`) per tier.
- camel-50 at -Xmx2g: **crashed with `Too many open files` before; now completes with fdBefore=61 → fdAfter=60**
  (stable across the full warm + loop run).
- A `-Dbench.fdwatch=N` diagnostic (dumps an `lsof` basename histogram when open FDs cross N) pinned the leaked handles
  to `lrm/.locks/*.lock` and is retained for regression triage.

## Files changed (uncommitted)

- `rewrite-maven-engine/.../engine/MavenEngine.java` — `rwlock-local` named-lock factory on the session template.
- `rewrite-maven/.../internal/engine/EngineDependencyCollector.java` — same on the collector session template.
- `rewrite-maven/.../internal/engine/EngineModelBuilderFactory.java` — shared singleton `DefaultModelBuilder`.
- `rewrite-maven/.../internal/engine/EngineEffectivePom.java` — shared/fresh model-cache store param; shared builder.
- `rewrite-maven/.../internal/engine/EngineDescriptorReader.java` — descriptor builds share `session.getCache()`.
- `rewrite-maven/.../internal/engine/BomGavAttributor.java` — ctx-wide `(epoch, bomGav)` cache of effective-management keys.
- `rewrite-maven/.../internal/engine/CacheBridge.java` — byte-backed `StringModelSource`; dropped materialize/materializeDir.
- `rewrite-maven/.../internal/engine/{EngineModelCache,MavenEngineResolution}.java` — profiler hooks; root build passes null store.
- `rewrite-maven/.../internal/engine/EngineProfiler.java` — new, opt-in phase attribution.
- `phase0-tools/.../IntegratedBenchmark.java` (+ `build.gradle.kts`) — per-phase profile print, FD counters, `fdwatch`
  diagnostic, and a ctx-reused (`loopCtx`) steady-state loop.
- `benchmarks/integrated-2026-07-09-post.{json,md}` (new).
