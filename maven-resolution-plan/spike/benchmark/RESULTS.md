# Maven resolution benchmark: OLD (rewrite-maven) vs NEW (raw Maven pipeline)

Head-to-head, coarse-ratio spike. **OLD** = current rewrite-maven resolution. **NEW** = the unadapted raw Maven
pipeline (Maven Resolver 2.0.20 + Maven 3.9.16 `DefaultModelBuilder`), *no* tree-type mapping onto rewrite's LST.
Numbers are indicative ratios (1× vs 2× vs 4×), not micro-precise; honest over favorable.

## Exact versions

| Component | Version |
|---|---|
| OLD `org.openrewrite:rewrite-maven` (latest.release) | **8.86.1** |
| OLD `org.openrewrite:rewrite-core` | 8.86.1 |
| NEW `org.apache.maven.resolver:maven-resolver-supplier-mvn3` (+ api/util/impl/connector) | **2.0.20** |
| NEW `org.apache.maven:maven-model-builder` / `maven-model` / `maven-resolver-provider` / `maven-artifact` | **3.9.16** |
| NEW `org.openrewrite:rewrite-core` (for `HttpSender`) | 8.86.1 |
| Corpus: `apache/maven` @ tag **maven-3.9.16** (shallow clone) | 15 reactor module poms |
| JDK | Temurin/Zulu 17 toolchain, `-Xms2g -Xmx2g`, separate JVM per engine |

Corpus modules (single reactor): apache-maven, maven-artifact, maven-builder-support, maven-compat, maven-core,
maven-embedder, maven-model, maven-model-builder, maven-plugin-api, maven-repository-metadata, maven-resolver-provider,
maven-settings, maven-settings-builder, maven-slf4j-provider, and the aggregator root. (The brief said "~50 modules";
the real maven-3.9.16 reactor is 15 — reported as measured.)

## Ratio table (headline)

| metric | OLD | NEW-N1 | NEW-N4 | takeaway |
|---|---:|---:|---:|---|
| **cold wall** (full reactor, ms) | 6205 | 3620 | 3656 | NEW ~**1.7× faster** cold |
| **cold requests** (`HttpSender` sends) | 141 | 266 | 266† | NEW issues more rounds (†see caveat) |
| **warm median wall** (full reactor, ms) | 112.2 | 18.2 | 30.1 | NEW **3.7–6.2× faster** warm |
| **re-resolution loop** (1 leaf ×50, ms/iter) | 10.75 | 1.67 | 3.06 | NEW **3.5–6.4× faster** hot loop |

Fair scope-for-scope comparison: OLD always resolves all four scopes, so **OLD ↔ NEW-N4** is the apples-to-apples row
(cold 6205→3656 = 1.70×; warm 112→30 = 3.73×; loop 10.75→3.06 = 3.5×). **NEW-N1** is the lower bound *if* all four
scopes can be mediated from one verbose graph instead of four collects.

Warm spreads are tight (OLD 108.8–114.2; N1 16.2–22.6; N4 27.1–34.6; loop mins/maxes within ~25%), so the warm/loop
ratios are solid. Cold is a single live-network shot — treat as indicative.

## N1-vs-N4 delta (the scope-mediation decision)

rewrite resolves compile/runtime/test/provided as four separate passes. N4 = four `collectDependencies` calls per pom
(one per scope-visibility set); N1 = a single verbose collect of the broadest (test) graph, from which scopes would be
mediated in-memory.

| path | N4 / N1 | why |
|---|---:|---|
| cold collect | **1.01×** (3266 vs 3230 ms) | first (test) pass pays all the network; passes 2–4 hit only the now-populated local repo → ~free |
| warm collect+model | **1.65×** (30.1 vs 18.2 ms) | no network to amortize; per-scope subgraphs still share the session `RepositoryCache` |
| hot loop | **1.83×** (3.06 vs 1.67 ms) | same, per single module |

**Four per-scope collects cost ~1.0× cold and ~1.65–1.83× warm — nowhere near the naive 4×.** Node work confirms the
overlap: N4 walks 2222 nodes vs N1's 680 (3.3× the nodes) but takes only 1.65× the time. So scope mediation from one
graph would save ~40–45% of *warm* collect time; sticking with four passes is not catastrophic.

## Model / effective-POM phase (NEW, measured separately)

`DefaultModelBuilder` (locationTracking=true, VALIDATION_LEVEL_MINIMAL, twoPhaseBuilding=false; a `ReactorModelResolver`
modelled on maven-core `ProjectModelResolver` — reactor poms served locally, external parents/BOMs from Central via the
`RepositorySystem`). Cold model phase = **390 ms for 15 modules** (26 ms/module), only ~11% of the 3620 ms cold N1 wall;
collection (network) dominates. Model builds: 15/15 succeeded, 0 collect failures.

## Maven 3.9 ModelCache clone-on-hit (warm path)

`DefaultModelBuilder` clones on every cache hit: `ModelCacheTag.RAW.fromCache → Model.clone()` and `IMPORT →
DependencyManagement.clone()`. Instrumented via a shared `CountingModelCache`: **per warm 15-module reactor build = 44
RAW `Model.clone()` + 15 `DependencyManagement.clone()` = 59 deep clones** (4 misses total once warm). At this scale the
clones are a small in-memory cost inside the 18 ms warm build, but they scale **linearly with reactor size × shared
parent/BOM depth** — a 300-module reactor sharing the same parents would pay ~20× those clones every warm build. Worth
watching, not a bottleneck here.

## Graph node counts (work-done sanity; deltas expected)

| engine | count | what it counts |
|---|---:|---|
| OLD, all scopes | 904 | winner-only resolved deps, summed over compile+runtime+test+provided (deduped within each scope) |
| OLD, compile only | 193 | winner-only compile-scope deps |
| NEW-N1 (test graph, verbose) | 680 | full verbose graph incl. retained conflict losers, one scope |
| NEW-N4 (sum of 4 passes) | 2222 | same nodes counted up to 4× across overlapping per-scope subgraphs |

Same order of magnitude (hundreds of nodes), confirming both engines did comparable resolution work. The deltas are
structural, not defects: OLD reports winners-only per scope (and I summed four scopes → 904); NEW verbose retains losers
(with a winner pointer) and N4 re-counts shared nodes across the four scope subgraphs. Not directly comparable, by design.

## Corpus parameterization (Phase 0 large-reactor addition)

The harness is parameterized so the same code runs the default apache/maven reactor and large reactors at graduated
module caps (`./gradlew runOld|runNew -Pcorpus=<reactor root> -PmaxModules=N -PleafModule=<dir> -PskipCold=true
-PlocalRepo=<dir> -Pheap=<size>`). Defaults reproduce the original apache/maven run exactly. Subtree selection
(`Corpus.selectSubtree`) always includes every pom-packaging module (parents/BOMs/aggregators) so the selected reactor
stays dependency-coherent, then fills the cap with artifact modules in path-sorted order — larger caps are strict
supersets of smaller ones. Large-reactor (apache/camel) results live in `maven-resolution-plan/benchmarks/`
(`baseline-<date>.json` + interpretation); cold runs are skipped there (live-network over hundreds of poms is
disproportionate; the scaling question is warm).

## Methodology

- **Isolation / fairness.** rewrite-maven and maven-resolver-supplier-mvn3 live in **separate Gradle source sets**
  (`old`, `new`) with separate configurations, launched as **separate `JavaExec` JVMs** with identical heap. They never
  share a classpath, so no maven-model/version conflict arose (nothing to reconcile — the isolation is structural).
- **Same transport in both engines.** Both route HTTP through the *same* OpenRewrite `HttpUrlConnectionSender`
  (30 s connect / 60 s read), wrapped in a counting `RecordingHttpSender`. OLD injects it via
  `HttpSenderExecutionContextView`; NEW via a `RepositorySystemSupplier` subclass installing an `HttpSenderTransporter`
  (cribbed from spike/transport) and disabling the prefixes remote-filter (which otherwise phones Central).
- **Timing.** `System.nanoTime()` around well-defined phases; NEW times the model phase and each collect pass separately.
- **Cold** = fresh session, empty temp local repo, empty `RepositoryCache`, fresh `ModelCache` (NEW) / fresh
  `InMemoryMavenPomCache` + empty temp local repo (OLD). Run once per engine (live Central). NEW runs the four scope
  passes in `test`-first order so the test pass's cold time is the honest N1 cold and passes 2–4 reuse the local repo;
  cold requests are therefore shared across N1/N4.
- **Warm** = same `RepositorySystem`, shared `RepositoryCache`+`ModelCache`, populated local repo, fresh session per
  iteration (NEW) / shared warm `InMemoryMavenPomCache`, fresh `ExecutionContext` per iteration (OLD). ≥2 warmup, 7
  measured; median + min/max.
- **Re-resolution loop** = one leaf module (`maven-core`) re-resolved 50× against warm caches, fresh session each time —
  approximates rewrite's `UpdateMavenModel` hot loop. 3 warmup, 50 measured, median per-iteration.

## Caveats (honest numbers over favorable)

1. **NEW is the *unadapted* pipeline — these are a floor, not the delivered engine.** NEW builds effective models and
   collects graphs but does **no mapping** to rewrite's `MavenResolutionResult`/LST. OLD builds the full rich tree. So
   NEW's speed advantage is partly "does less downstream work." The mapping layer's cost is **not measured here** and
   must be added before claiming an end-to-end win.
2. **† Cold request counts are not apples-to-apples.** OLD 141 vs NEW 266: Maven Resolver additionally fetches `.sha1`/
   `.md5` checksum sidecars and issues HEAD/existence peeks that rewrite's downloader does not. That fully accounts for
   the ~1.9× and does **not** mean NEW downloads ~2× more POMs. Per the quality bar, lean on **wall time** (identical
   `HttpSender` transport in both) as the comparable network axis; the request delta is a transport-policy artifact,
   tunable via the resolver checksum policy.
3. **Cold is single-shot over live Central** — high variance; the 1.7× cold ratio is indicative. The warm and loop
   ratios (tight spreads, no network) are the trustworthy ones.
4. **Scope model in N4 is approximate.** Per-scope direct-dep filtering uses the standard Maven scope→classpath
   visibility rule; it is close enough for a coarse ratio, not a scope-mediation reference implementation.
5. **Fixed versions only.** The `ReactorModelResolver` skips version-range resolution (the maven-3.9.16 corpus has none);
   a range would add a metadata round-trip per occurrence.

## What this means for the migration's perf plan

- **The resolution core is not a regression risk — it's an upgrade.** On identical inputs the raw Maven pipeline is
  ~1.7× faster cold and **3.7–6.4× faster in the warm and hot-loop paths** that dominate real recipe runs. The warm
  full-reactor build drops 112 ms → 30 ms (all-scope) and the `UpdateMavenModel`-style re-resolution 10.7 ms → 3.1 ms.
  There is real headroom to spend on the adapter.
- **Budget the adapter against that headroom.** The measured NEW numbers exclude resolver-graph → rewrite-LST mapping.
  The migration's perf risk lives entirely in that mapping layer, not in resolution. Next spike should measure mapping
  cost and confirm the end-to-end warm/loop numbers still beat OLD.
- **Prefer one verbose collect + in-memory scope mediation (N1) over four passes (N4).** It's ~40–45% cheaper warm and
  essentially identical cold, and the verbose graph already carries winner/loser + premanaged data (per spike/provenance)
  needed to derive all four scopes. If four passes are simpler to adapt, the ~1.65–1.83× warm penalty is tolerable.
- **Watch ModelCache clone-on-hit at scale.** 59 deep clones per 15-module warm build is negligible now but grows
  linearly with reactor size × shared-parent depth; for very large reactors, consider a clone-free ModelCache (the clone
  is a Maven 3.9 correctness measure, but rewrite controls the cache impl and the mutation discipline around it).
- **Set the resolver checksum policy deliberately.** NEW's extra cold requests are checksum/HEAD sidecars; if network
  rounds matter for a host, `ChecksumPolicy`/`ArtifactDescriptorPolicy` tuning removes them without touching resolution.
```
