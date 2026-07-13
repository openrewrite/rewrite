# Proof-of-concept spike results

2026-07-07. Two hermetic, standalone Gradle projects under `spike/` empirically test the four
load-bearing claims the plan previously rested on source-reading alone. Built by two parallel
implementation agents; independently reviewed (test suites force-rerun, load-bearing test code
read, the one surprising claim verified against resolver source).

Versions exercised: `maven-resolver-supplier-mvn3:2.0.20` → resolver 2.0.20 + Maven 3.9.16
provider/model-builder; `rewrite-core:8.86.1`. Everything compiled at `options.release = 8`.

## Verdicts

| # | Claim | Verdict | Evidence |
|---|-------|---------|----------|
| 1 | supplier-mvn3 bootstraps with plain `new`, collects a graph, POM-only I/O | **PROVEN** | `spike/transport` T1: app→lib-a→lib-b collected via MockWebServer repo; no `.jar` requests |
| 1b | Whole stack honors the Java 8 bytecode floor | **PROVEN** | T4: 55 jars / 10,457 base classes on the resolved runtime classpath, max classfile major = exactly 52 |
| 2 | Per-session `HttpSender` injection with zero bypass | **PROVEN** | T2: custom `TransporterFactory` reads sender from `session.getConfigProperties()`; two sessions → two recording senders; `server.requestCount == recorderA + recorderB`; `peek()`→HEAD; `classify()` 404→NOT_FOUND, 500→OTHER |
| 3 | `ArtifactDescriptorReader` decoration can short-circuit all network | **PROVEN** | T3: GAV-keyed decorator cache + second session with fresh empty local repo → zero new server requests. This is the seam `MavenPomCache` lives behind |
| 4a | `InputLocation` attributes effective values to the contributing model | **PROVEN** | `spike/provenance` P1: parent-contributed managed version → `InputSource.getModelId() == parent` |
| 4b | `bomGav` reconstructable on 3.9 | **PROVEN-WITH-CAVEAT** | P2: importer override installs via `DefaultModelBuilderFactory.newDependencyManagementImporter()`; correct for single-level BOMs; **multi-level BOMs (BOM inherits its DM from a parent) attribute to the defining parent, not the imported BOM** — importer receives GAV-stripped `DependencyManagement` lists, so the seam cannot fix it |
| 4c | Verbose collect exposes winner/loser/premanaged/depth | **PROVEN** | P3/P4: `Verbosity.STANDARD` + `DependencyManagerUtils.CONFIG_PROP_VERBOSE`; `NODE_DATA_WINNER`, `getPremanagedVersion/Scope/Exclusions`, depth via child traversal, managed-exclusion pruning observable |

## Plan impacts (already folded into README.md)

1. **Disable Remote Repository Filtering at bootstrap.** Resolver 2.x registers
   `GroupId`/`PrefixesRemoteRepositoryFilterSource` with `DEFAULT_ENABLED = true`
   (source-verified). Active, they fetched `.meta/prefixes.txt(.sha1/.md5)` out of band —
   including on Maven Central's `/maven2` path that no request configured (Central enters via
   super-POM-injected descriptor repositories). Engine bootstrap must override
   `createRemoteRepositoryFilterSources()` (or set
   `aether.remoteRepositoryFilter.{groupId,prefixes}.enabled=false`). Related: set
   `ignoreArtifactDescriptorRepositories(true)` to match rewrite's root-repositories-only
   transitive fetching and keep super-POM Central out of the repo set (mechanism to re-confirm
   in Phase 1 — the spike observed the traffic, not the full causal chain).
2. **`bomGav` needs membership stamping, not the importer.** `EffectivePomMapper` resolves each
   `scope=import` BOM's effective model (through the cached pipeline) and stamps `bomGav` by
   membership in import order; `InputLocation` distinguishes parent-inherited from BOM-imported
   entries. (`BomGavAttributor` in the README architecture.)
3. **Footprint corrections to a7**: `asm` arrives via maven-model-builder 3.9.16, `gson` +
   `error_prone_annotations` via resolver-spi — the ~20-jar list was understated (all Java 8,
   harmless). `transport-apache` + httpclient/httpcore/commons-codec are confirmed excludable
   once `HttpSenderTransporter` is the sole network transport.
4. **Test-infra note**: `HttpUrlConnectionSender` issues real HEADs (fine); MockWebServer must
   not send bodies on HEAD responses or keep-alive is poisoned.
5. Minor study correction: `ConflictResolver` verbosity accepts the `Verbosity` enum directly
   on 2.0.20.

## Parent-cycle leniency spike (added same day, `spike/parent-cycle/`, 9 tests green)

The critique's prediction was **confirmed**: a stub parent served under the revisited GAV still
trips `DefaultModelBuilder`'s FATAL `parentIds` check (the compared id comes from the child's
`<parent>` element, not the served bytes). The working strategy, spike-proven:

- **Multi-pom cycles**: cycle-aware `ModelResolver` seeded with the root GAV(s), tracking served
  GAVs; on a would-be repeat it mutates the passed `Parent`'s artifactId to a sentinel and serves
  a parentless stub under the synthesized GAV. Build succeeds; full real ancestry inheritance
  survives (parent DM present, `InputLocation` provenance intact) — equal or richer than
  rewrite's current silent degrade. Mappers must filter the sentinel id from provenance.
- **Self-parent** (`A.parent=A`): rejected by `validateRawModel` before the resolver is ever
  consulted — handled by pre-supplying `request.setRawModel(...)` with the degenerate parent
  stripped.
- **Partial-result fallback is a backstop only**: on abort, `getResult()` carries just the root
  raw model (no inheritance) — strictly poorer than rewrite's guarantee.
- **Flagged for Phase 2**: `scope=import` BOM cycles hit a *separate* FATAL (`importIds`) that
  rewrite also tolerates today (`circularImportDependency`) — needs an analogous break.
- Ledger note: rewrite breaks cycles silently today; the new mechanism emits a warning at the
  break point (desired, but a visible change).

**POLICY CHANGE (Jon, 2026-07-07, after this spike ran)**: divergence from Maven = bug; no
lenient mode. The mutate-stub strategy does NOT ship — cycles/self-parents now fail
Maven-identically (per-file `ParseExceptionResult`). This spike's production value is the
captured Maven failure shapes (E1/E2/E5 exception/problem details), which become the assertions
of the inverted cycle-tolerance pinning tests. See README "Maven parity policy".

## Comparative benchmark spike (`spike/benchmark/`, OLD vs raw NEW pipeline)

apache/maven @ 3.9.16 reactor (15 poms), identical inputs, same `HttpUrlConnectionSender`,
separate JVMs. NEW = unadapted pipeline (effective models + verbose collect; **no tree mapping —
a floor, not the delivered engine**). N1 = one verbose collect/pom; N4 = four per-scope
collects/pom (apples-to-apples with OLD, which always resolves four scopes). Agent's run and an
independent re-run agreed; re-run numbers:

| metric | OLD | NEW-N1 | NEW-N4 |
|---|---:|---:|---:|
| cold wall, full reactor (ms) | 5646 | 2748 | 2781 |
| cold requests | 141 | 266 | 266 |
| warm median wall (ms) | 109.5 | 14.5 | 26.8 |
| re-resolution loop (ms/iter) | 10.8 | 1.3 | 2.7 |

- **The resolution core is an upgrade, not a regression**: ~2× faster cold, ~4× warm, ~4× on the
  `UpdateMavenModel`-style hot loop (N4, apples-to-apples) — leaving real headroom for the
  adapter/mapping layer, where the remaining perf work lives.
- **Scope-mediation data**: N4 costs only ~1.8× N1 warm (subgraphs share the session cache; 3.3×
  the nodes for 1.8× the time), not the feared 4× — and N4 is still ~4× faster than OLD. The
  perf objection to per-scope collects is gone; per-scope (shape-exact, no cross-scope
  version-shift behavior change) is now the recommendation, confirmed on Phase-3 corpus data.
- **ModelCache clone-on-hit confirmed and quantified**: 59 deep clones per warm 15-module build
  (44 RAW `Model.clone()` + 15 IMPORT). Linear in reactor size × shared-parent depth — the
  custom `ModelCache` mitigation stays planned for large reactors, with this as its baseline.
- Cold request delta (141 vs 266) is checksum sidecars (`.sha1`/`.md5`) + HEAD peeks — a
  tunable transport policy, not extra POM downloads; wall time is the fair network axis.
- Caveats: 15-pom corpus (small; rerun at larger scale in Phase 0), cold is single-shot live
  network (indicative), warm/loop spreads are tight (trustworthy).

## Large-reactor baseline (Phase 0 close-out, `benchmarks/baseline-2026-07-08.{json,md}`)

apache/camel@4.4.0 at 50/150/400/548-module tiers, identical module sets + pinned leaf per tier,
sleep-tainted measurements audited and re-taken (methodology notes in the baseline .md):

| tier | OLD | NEW-N1 | NEW-N4 | OLD/N1 |
|---|---:|---:|---:|---:|
| maven 15 | 103.6 | 13.7 | 24.8 | 7.6× |
| camel 50 | 1,176.7 | 1,036.3 | 2,157.3 | 1.14× |
| camel 150 | 2,016.4 | 1,355.1 | 2,689.8 | 1.49× |
| camel 400 | 4,016.1 | 1,620.6 | 3,057.8 | 2.48× |
| camel 548 | ~5.2s extrapolated | 2,167.2 | 4,450.7 | ~2.4× |

Two verdicts, both folded into DESIGN.md §0:

1. **ModelCache clone-on-hit is linear — mitigation NOT needed ≤548 modules.** Flat ~8.3–8.6
   clones/module on camel (parent-depth-dependent coefficient); ≤0.27 ms/clone pessimistic;
   revisit beyond ~1–2k modules or if Phase-4 profiling shows the model phase dominating.
2. **Scope mediation reversed to single verbose collect (N1).** On BOM-heavy camel, four
   per-scope collects are *slower than the old engine* at 50–150 modules (0.55–0.75×) and scale
   ~3.5× steeper (~8.2 vs ~2.3 ms/module marginal); N1 wins every tier. The 15-module result
   that favored N4 did not survive scale — the reason the large-reactor benchmark existed.
   Cross-scope version shifts return to the ledger, sized in Phase 3.

## What this buys

The four claims that determine architecture feasibility — no-DI bootstrap on Java 8, per-session
HttpSender with structural no-bypass (the moderne-saas requirement), the descriptor-reader cache
seam, and provenance sufficiency — are now demonstrated, not asserted. Remaining pre-Phase-0
work is specification, not feasibility: consolidate the two designs into one canonical detailed
design, spec Phase 0 to the file level, and run the two *performance/leniency* spikes (deep
reactor `ModelCache` cost; parent-cycle handling vs `DefaultModelBuilder`'s FATAL check), which
are unrelated to these claims and still open.
