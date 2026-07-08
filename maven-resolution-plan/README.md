# Replacing rewrite-maven's custom resolution with Maven APIs

Synthesized plan, 2026-07-07. Produced from seven parallel deep-reads of rewrite-maven,
apache/maven-resolver (2.0.21-SNAPSHOT master), apache/maven 3.9.x, and apache/maven 4 master
(as of 2026-07-07), followed by two independently-authored designs and adversarial critiques of
each. This document merges the two designs and incorporates every critique finding. Source
documents are in `study/` and `design/` alongside this file.

**Status 2026-07-07**: studies, designs, critiques, synthesis, and all four spikes complete
(feasibility ×4 proven, parent-cycle mechanics resolved, comparative benchmarks: raw new core
~2× faster cold / ~4× warm — see `SPIKE-RESULTS.md`). **The canonical detailed design is
[`DESIGN.md`](DESIGN.md)** — it supersedes the two `design/` documents and encodes every
decision (3.9 stack committed; single flip-over, no compat flags; Maven-parity absolutism with
pinning tests; per-scope collects; XML-first model input). **Phase 0 is underway** per
[`PHASE-0-SPEC.md`](PHASE-0-SPEC.md): slice A (snapshot/diff + determinism/identity/serialization
gates, in rewrite-maven test sources) and slice B (corpus + record/replay + ground-truth
tooling, `phase0-tools/`) in progress; slices C (synthetic corpus) and D (benchmark graduation)
follow their review. Open with Jon: HTML-index derivation keep/kill; Moderne CLI cache-format
coordination.

## Recommendation in one paragraph

Do it, on the stack both designs independently chose: **Maven Resolver 2.0.x via
`maven-resolver-supplier-mvn3` + Maven 3.9.16's `maven-resolver-provider`/`maven-model-builder`,
shaded and relocated** into a rewrite-owned artifact. Maven code owns every semantic decision
(effective POM, profile activation, dependency management, collection, conflict mediation,
version ranges, snapshots); rewrite keeps ownership of bytes, caches, transport, ordering,
leniency policy, and the frozen `org.openrewrite.maven.tree.*` API — produced by an adapter
layer that preserves today's instance-identity and wire-format contracts. Estimated effort:
**25–35 person-weeks** across six independently-releasable phases (~4 months with two engineers),
with the estimate tightened by two week-one spikes before commitment.

## Why this stack (and not Maven 4)

1. **Java 8 floor.** rewrite ships Java 8 bytecode. Resolver 2.x core and the mvn3 supplier are
   Java 8; everything Maven-4-flavored (`maven-impl`, `ApiRunner`, supplier-mvn4, transport-jetty)
   requires Java 17.
2. **Parity target.** The existing test suite — the oracle for this migration — and users' actual
   builds encode Maven 3 semantics (`ClassicDependencyManager`, first-import-wins BOMs, no
   MNG-5600 import exclusions). Maven 4's `TransitiveDependencyManager` would *introduce*
   divergence, not eliminate it.
3. **Maturity.** Maven 4's new API is still `@Experimental` at rc-5; `ApiRunner` is test-oriented.

The plan commits to the 3.9 stack outright (decision 2026-07-07: Maven 4 has been in rc for
20 months across five rc's with master already on 4.1.0-SNAPSHOT, and 3.9.x is expected to stay
patched for as long as the ecosystem runs Maven 3 builds). No work is scheduled and no ledger
entries are kept for a hypothetical Maven 4 swap. The `MavenEngine` facade and the two
rewrite-owned interfaces (effective-POM building, dependency collection) earn their place on
containment grounds alone — they keep shaded Maven types out of the rest of rewrite-maven and
make the engine testable in isolation; if a different engine ever becomes compelling, the facade
is where it would land, but nothing extra is built for that. Worst case on the other side —
3.9.x eventually unpatched — the shaded pin means inheriting a frozen, battle-tested
model-builder to maintain inside the shade: a strictly smaller liability than the custom
algorithm it replaces.

### The founding concerns, re-examined against 2026 Maven

The three reasons the custom algorithm exists are now answered upstream, mostly:

1. **"Hard to invoke programmatically (aether/wagon/OSGI baggage)" — gone.**
   `RepositorySystemSupplier` is ~1,200 lines of pure `new` wiring with ~60 `protected create*()`
   override points. No Guice, no Sisu, no Plexus container, no Wagon (its pom bans
   `javax.inject`). The supplier-mvn3 dependency tree is roughly two dozen jars (spike-measured:
   a7's list understated — asm arrives via model-builder, gson via resolver-spi; all Java 8);
   httpclient/transport-apache are excluded outright since `HttpSenderTransporter` is the sole
   network transport. Spike-proven end-to-end on resolver 2.0.20 + Maven 3.9.16.

2. **"Maven doesn't cache inaccessibility" — half right in 2020, and that half still stands.**
   Maven *does* persistently cache not-found (404) per artifact × repo × auth-digest via
   `.lastUpdated` sidecars and `resolver-status.properties` (`DefaultUpdateCheckManager`), and
   dedups checks to once per session. But **transfer errors (dead repos) are never cached across
   invocations by default** — Maven 3.9 and 4 CLIs both hardcode `cacheTransferError=false`
   (MNG-7653 made even `-canf` sticky-off). An embedder controls this with one line
   (`SimpleResolutionErrorPolicy`), and rewrite's own stance is *richer* than Maven's:
   deterministic-4xx-only negative caching in `MavenPomCache`, plus a run-scoped dead
   `host:port` set. We keep rewrite's stance and implement it *through* resolver's policy seams
   (see "failure caching" below).

3. **"No interprocess locking in ~/.m2" — fixed upstream.** Resolver 2.x defaults to
   cross-process `FileChannel` locks per GAV (`FileLockNamedLockFactory`, mapper `file-gaecv`),
   with an IPC lock-server module for high-throughput multi-JVM (mvnd heritage). Tracking-file
   writes take their own OS file locks. `RepositorySystem` and sessions are thread-safe.

## The most important thing the study found

**The custom resolver is not a Maven clone with edge-case bugs — it is a structurally different
algorithm whose *outputs* recipes depend on.** Per-scope pure classpath lists (`{Compile,
Runtime, Test, Provided}` map), BFS with global restart on mediation changes, GA-keyed
`VersionRequirement` chains, lazy `${...}` interpolation at read time, glob exclusions,
requested-vs-effective provenance on every node. A replacement therefore cannot be a transparent
engine swap; it is an **adapter that maps Maven's results into the existing output shape**, plus
a reviewed ledger of deliberate behavior flips where Maven is simply right and we were not.

Equally load-bearing: the compat boundary is stricter than "keep the getters."

- **Wire format**: seven types carry `@JsonIdentityInfo("@ref")`; `ManagedDependency` serializes
  FQCNs (`@JsonTypeInfo(Id.CLASS)`) so classes cannot move packages; `Scope` names are map keys;
  old LSTs must deserialize (null-tolerant getters, `dependencyManagementSorted` re-sort,
  `Pom.getModelVersion()==3` gates the RocksDB cache).
- **Reference identity**: `getResolvedDependency`/`getResolvedManagedDependency` compare with
  `==`; depth-0 `ResolvedDependency.requested` must BE the element instance of
  `Pom.getDependencies()`; DM entries thread `Defined`/`Imported` instances through.
- **Deliberate mutation seams**: `Pom.properties` mutated in place by `MavenParser.property()`
  and `UpdateMavenModel`; `unsafeSet*` used for reactor stitching. 31 recipes drive
  `maybeUpdateModel()` re-resolution mid-mutation.
- **rewrite-gradle is the second-largest consumer** — it constructs tree types from Gradle's own
  resolution and invokes the maven engine headlessly with synthetic `Pom.builder()` poms, so
  public builders/constructors are external API too.

## Architecture

One new internal package `org.openrewrite.maven.engine` (~10 classes), one shaded artifact.
`RawPom → Pom` stays exactly as-is (it is the verbatim/cacheable wire anchor). `ResolvedPom`'s
1,336-line inner `Resolver`, `doResolveDependencies`, and `VersionRequirement`-as-engine are
ultimately deleted (~4.5k lines); `Pom.resolve()` / `ResolvedPom.resolveDependencies()` /
`MavenPomDownloader` keep their signatures as facades (public constructors included —
they are called cross-module today).

```
                    org.openrewrite.maven.tree.*  (FROZEN — wire format + identity contracts)
                                 ▲
                 EffectivePomMapper / DependencyGraphMapper
              (instance threading, per-scope projection, glob
               effectiveExclusions post-pass, error/listener mapping)
                                 ▲
   MavenEngine (bootstrap facade over RepositorySystemSupplier; the only
                class that knows which Maven stack is underneath)
      │ DefaultModelBuilder (locationTracking=true, lenient policies)
      │ BF collector + verbose ConflictResolver + Default(Version|VersionRange)Resolver
                                 ▲
   rewrite-owned SPI implementations:
      HttpSenderTransporter      — sole network transport; owns classify(),
                                   retry, auth→anonymous fallback, dead-endpoint set
      ReactorWorkspaceReader     — 3-tier reactor match; ${revision} raw-GAV-first
      CacheBridge                — MavenPomCache (with new pom-bytes region) above Maven
      SettingsBridge             — MavenSettings stays the model; mirrors applied at
                                   request-repo-list assembly (not session MirrorSelector)
      ParityErrorMapper          — Maven-identical failure surfacing: ModelBuildingException /
                                   collect errors → MavenDownloadingException(s) +
                                   ParseExceptionResult; tolerate exactly where Maven
                                   tolerates, fail exactly where Maven fails
      BomGavAttributor           — stamps bomGav by imported-BOM effective-DM membership
                                   in import order (spike-proven: InputLocation/importer
                                   alone misattribute BOMs that inherit their DM)
```

Shading: packages relocate to `org.openrewrite.maven.internal.shaded.*` (final naming open).
This is non-negotiable, not packaging hygiene: both Maven 3.9's and Maven 4's core realms
force-export `org.eclipse.aether.*` and `maven-model(-builder)` parent-first to every plugin, so
un-relocated embedding inside rewrite-maven-plugin gets the host's resolver 1.9.27 instead of
ours. Relocation makes all four hosts (Maven plugin, Gradle plugin, standalone, server) behave
identically. Drop `transport-apache` from the shade — `HttpSenderTransporter` is the only
network transport (keep `transport-file`), which both slims the jar and makes I/O-bypass
structurally impossible.

### Seam-by-seam notes (critique findings folded in)

**POM cache.** `MavenPomCache` gains a **pom-bytes region** (default methods, keyed identically
to `getPom`): ModelBuilder needs real XML and parsed `Pom` is lossy (drops
distributionManagement, relocations, profile builds). Without it, warm persistent caches
(RocksDB/Composite on the server) would re-download every pom — the single most severe critique
finding. On a bytes hit, parse through to populate the `Pom` region too. Tri-state negative
semantics and RocksDB's never-persist-negatives stance are preserved. Do **not** synthesize
`ArtifactDescriptorResult`s from cached `ResolvedPom`s — that quietly rebuilds a second semantic
engine (warm-vs-cold divergence replacing rewrite-vs-Maven divergence). Cache Maven's own
descriptor/model output under the existing key instead; the modelVersion bump covers the format
change.

**HTTP transport.** `HttpSender` injection is a hard host requirement, not a convenience:
moderne-saas tunnels repository traffic from the SaaS VPC over RSocket to a customer-side
connector by injecting a custom sender, so any engine-internal HTTP client breaks that
deployment outright. `HttpSenderTransporter` is the only network `TransporterFactory` registered
in our supplier override (plus `transport-file`); the stock HTTP transports are excluded from
the shade, so bypass is impossible by construction. The sender is resolved per session — carried
in session config properties, captured from `HttpSenderExecutionContextView` when the facade
builds the session — preserving today's per-`ExecutionContext` injection granularity. Mapping:
GET → sender GET (no range/resume; full fetch), PEEK → HEAD, PUT → unsupported. The session gets
no proxy selector and no TLS config of its own — settings `<proxies>` stay parsed-but-unconsumed
because the injected sender owns the transport path end-to-end (a session proxy selector would
double-route tunneled traffic). Phase 1 guarantee test: recording `HttpSender` + MockWebServer
asserting every byte of engine traffic passed through the sender. All spike-proven (claims 2–3,
`SPIKE-RESULTS.md`), with one addition the studies missed: bare resolver 2.x enables Remote
Repository Filtering sources by default (`DEFAULT_ENABLED = true`, source-verified), fetching
`.meta/prefixes.txt` out of band. Maven 3.9's own resolver does not do this, so disabling the
RRF sources IS parity. The general principle (parity policy): the engine's session template
mirrors Maven 3.9's `DefaultRepositorySystemSessionFactory` for every knob — including honoring
descriptor-discovered repositories. (Precisely: today a transitive pom's `<repositories>` are
used for its own parents/BOMs but not for fetching its children, which use the root's list;
Maven aggregates them into child requests. Divergence → align. An earlier note here
recommending `ignoreArtifactDescriptorRepositories(true)` predates the parity policy and is
superseded.)

**Failure caching.** `Transporter.classify()` is the single bridge into Maven's negative-caching
machinery — map rewrite's exact rule (deterministic 4xx except 408/425/429) onto it. No
`.lastUpdated`-style negative may outlive the run unless it came through `MavenPomCache`'s
policy: persist not-found via our cache, keep transfer-error memory session-scoped (a dead repo
today is retried next process; hosts depend on that — poisoning CI for a day on a blip is a
regression, not a strictness win).

**Local repository manager (the one place Maven requires files).** Resolver refuses to build a
session without an LRM (`DefaultRepositorySystem.validateSession`), and resolved artifacts are
handed around as real filesystem `Path`s. This is not a caching commitment: the LRM is our own
SPI implementation over a private per-run scratch directory (resolver's own demos prove jimfs
satisfies it, if ever needed). Nothing durable lives there — POM bytes, parsed `Pom`s,
descriptors, metadata, and negative entries all flow through the pluggable `MavenPomCache`; a
fully warm resolution touches no filesystem. Jars move only on explicit recipe demand via
`MavenArtifactDownloader`, above the unchanged `MavenArtifactCache`. Because
`.lastUpdated`/`resolver-status.properties` tracking files are per-LRM-directory, the scratch
LRM makes Maven's file-based failure memory die with the run automatically; file-based named
locks likewise only guard LRM access, so private scratch dirs mean no cross-process contention
and no imposed shared `~/.m2`.

**Metadata / versions / snapshots.** Ranges, LATEST/RELEASE, and snapshot resolution inside
collect go through Maven's `VersionResolver`/`MetadataResolver` — decorate these
(`createVersionResolver` override point) to route through `MavenPomCache.getMavenMetadata`,
honor `pinnedSnapshotVersions` for *transitive* snapshots too, and decide explicitly on
cross-repo metadata **merging** (rewrite unions across all repos; Maven is first-wins — ledger
entry either way). Nexus HTML-index metadata derivation and pom-less-jar stub synthesis stay,
as augmentations in `CacheBridge`/`MetadataDownloader`.

**Reactor / re-resolution.** `WorkspaceReader` + `WorkspaceModelResolver` over the existing
project-poms maps, keeping the three-tier match (exact GAV / raw / property-merged) and deleting
our relativePath logic in favor of Maven's `readParentLocally`. Because `UpdateMavenModel`
mutates poms **without changing their GAV**, every GAV-keyed cache (ModelCache RAW/IMPORT,
DataPool, any effective-model memo) must either bypass workspace GAVs or key on a reactor epoch
counter bumped at marker replacement. Add a mutate-parent-then-re-resolve test to the phase gate;
both critiques flagged this as the way edits silently stop propagating.

**Identity threading.** Join Maven output back to `Pom` instances by **`InputLocation`
(line/column)**, not by GACT — `${project.groupId}` is the *default* for sibling deps, and
`ModelNormalizer` de-dupes declarations, so coordinate joins break exactly where recipes need
them. Location tracking is on anyway for provenance.

**Properties.** `ResolvedPom.getProperties()`/`getValue()` keep raw-lineage values (lazy
interpolation is serialized API), built by overlaying: merged raw models ⊕ parser/ctx-injected
properties (which must remain visible in the map, not just as Maven user properties).

**Scope shape.** From one verbose mediated collect, **project per-scope `ResolvedDependency`
trees with scope-filtered children** — instances shared within a scope (the actual contract),
never across scopes (a shared node would leak runtime/test children into compile-list
traversals). Whether single-collect mediation (Maven-exact, cross-scope version consistency) or
four per-scope collects (shape-exact with today) wins is decided on shadow-corpus data, not
argument. `Scope.transitiveOf` packaging, root-scope=Compile seeding, the scope-widening guard,
and glob `effectiveExclusions` attribution remain rewrite code by design — they are output
*shape*, not resolution *semantics*; file them as KEEP_REWRITE ledger entries.

## Verification: the differential oracle

Phase 0 builds the thing that makes every later phase safe:

1. **Double-engine test harness** — a JUnit extension running the existing ~5,200 rewrite-maven
   tests against both engines and diffing normalized `MavenResolutionResult`s. The diff must be
   shape-aware: ordered per-scope lists, nested `.dependencies` graph serialization, instance-
   identity probes, and `ResolutionEventListener` event multisets — a sorted-set diff would pass
   exactly where the API is strictest.
2. **Corpus** — top-N Central POMs *plus* a MockWebServer synthetic corpus specifically covering
   the areas Central can't: mirrors, auth + anonymous fallback, snapshots/classifiers, dead
   repos, HTML-index derivation, gradle-metadata, `file://`. Ground truth captured from real
   Maven: `dependency:tree -Dverbose` *and* `help:effective-pom`.
3. **Benchmarks as ship-blockers — comparative old-vs-new, not absolute targets.** The harness
   runs the SAME workload through both engines on identical inputs and gates on the ratio. To
   measure before the adapters exist, it stands up "just enough of a cutover": the raw
   new-engine pipeline (effective-POM build + one verbose collect per reactor pom, no tree
   mapping — mapping cost added to the measurement once built). Scenarios: cold + fully-warm
   large real reactor (deep parent chains — the Moderne hot path), huge single-pom transitive
   graph, and the warm `UpdateMavenModel`-style re-resolution loop. Metrics: wall clock,
   allocation, network request count (hermetic mirror). Known cost center to watch: 3.9
   `ModelCacheTag` deep-clones on every cache hit, which a memo above ModelBuilder cannot
   amortize across a 300-module reactor's parent chains; planned mitigation is our own
   `ModelCache` storing immutable snapshots. Cold resolution is expected *faster* than today
   (5-thread descriptor prefetch, 4-thread metadata, pooled connections vs. our serial BFS) —
   the benchmark verifies rather than assumes it.
4. **Week-one spikes (pre-commitment gates)**: (a) the comparative benchmark above, unshaded —
   old engine vs. minimal new-engine pipeline on a big real reactor and a huge graph; (b)
   parent-cycle leniency — `DefaultModelBuilder` has its own FATAL cycle check, so the
   stub-parent strategy needs proving on the cycle-tolerance fixtures before Phase 2 is planned
   in detail. (The PoC feasibility spikes are done — see `SPIKE-RESULTS.md`.)
5. **The divergence ledger** — every old-vs-new diff classified `ALIGN_TO_MAVEN` (ship it,
   release-noted) / `KEEP_REWRITE` (adapter must reproduce) / `NEW_BEHAVIOR` (needs sign-off).
   The ledger is the acceptance contract, reviewed like code.

## Phases

Merged from both designs (transport early, from the strangler; dual-engine flag flip, from
full-delegation). Each lands independently green on the existing suite.

| # | Content | Effort |
|---|---------|-------|
| 0 | Oracle: double-engine harness, corpora, benchmarks, two spikes, ledger process | 3–4 pw |
| 1 | Engine bootstrap: shaded artifact, `MavenEngine`, `HttpSenderTransporter`, `SettingsBridge`, `ReactorWorkspaceReader`, pom-bytes cache region | 4–6 pw |
| 2 | Effective POM on `DefaultModelBuilder` behind a default-off flag; dual-engine CI; `InputLocation` threading; property overlay; reactor-epoch cache keying. The 1,000-line `Resolver` dies. Wholesale fixes land: property-based profile activation reads properties not env vars, `<os>`/`<file>` activation, plugin-merge bugs, pluginRepositories | 6–9 pw |
| 3 | Collection/mediation on the verbose BF collector; per-scope projection; metadata/version decoration (pinning, cache routing); exclusion attribution; error/listener mapping; scope-mediation decision from corpus data; re-enable the `@Disabled` `dependencyManagementPropagatesToDependencies` test | 6–9 pw |
| 4 | Cache integration hardening + performance gate (custom `ModelCache` if benchmarks demand) | 2–3 pw |
| 5 | Single flip-over (no user-facing flags), one-release revert-by-release window with the old engine in-tree but unreachable, then deletion; ledger → release notes; ADR | 3 pw |
| | **Total** | **25–35 pw** |

The two designs estimated 24 and 39 pw; the range above reflects the critiques of both (the low
bound omitted real work; the high bound double-counted mapper effort available mid-Phase 2).
Phase 0's spikes tighten this before anything irreversible is built.

## Maven parity policy (decided 2026-07-07)

**Any resolution divergence from Maven's behavior is a bug, not a feature.** There is no lenient
mode. The target is Maven's exact tolerance profile: failing where Maven fails (parent cycles,
self-parents, import cycles — surfaced via the parser's standard per-file `ParseExceptionResult`
containment, so a bad pom fails without aborting the run) and tolerating where Maven tolerates
(e.g. missing transitive descriptors, warn-and-continue per `mvn dependency:tree`). Consequences:

- The ledger's KEEP_REWRITE category is confined to non-semantic territory: the frozen tree
  API/output shape, the pluggable seams (caches, `HttpSender`, `MavenSettings` injection), and
  the negative-caching/perf stance (changes how often the network is hit, never what resolves).
  All semantics default to ALIGN_TO_MAVEN.
- **Every discovered divergence lands a pinning unit test in rewrite-maven's suite** asserting
  the Maven-identical behavior, with the fix. The unit suite — not the implementation — is the
  durable spec. Existing tolerance tests (cycle suite etc.) are inverted, not deleted: same
  fixtures, Maven-identical expected outcomes (the parent-cycle spike captured the exact
  exception/problem shapes to assert).
- The `LenientModelResolver`/mutate-stub machinery does not ship; the spike's value was
  capturing Maven's failure shapes for the pinning tests.
- Gradle Module Metadata injection is a parity bug on the Maven parsing path (Maven ignores
  `.module`) and is removed there. Its only real consumer is rewrite-gradle's post-mutation
  marker approximation (`GradleDependencyConfiguration.doResolve` rebuilds per-dependency
  compile closures via the maven engine when a recipe edits a Gradle build; the GradleProject
  model itself comes from Gradle's own resolution at parse time). That path's ground truth is
  Gradle, which honors `.module` platforms — so the injection becomes an explicit opt-in engine
  option set only by that path. No mode in the semantic engine.
- HTML-index metadata derivation (serving version listings where the repo manager omits
  `maven-metadata.xml` — real Maven fails there) is the one remaining beyond-Maven keep/kill
  call for Jon. Pom-less-jar stubbing likely matches Maven's own missing-descriptor tolerance —
  pin by test.

## Known behavior changes (ledger seeds)

**Decided 2026-07-07: single flip-over, no compat flags.** All approved `ALIGN_TO_MAVEN` changes
ship at once in one loudly-versioned release with the ledger as release notes. The gate is
evidence, not flags: no flip ships until proven out against the corpus — the full existing test
suite double-engine, the synthetic MockWebServer corpus, and shadow-mode runs at real-world
scale sizing each change's blast radius. Consequences: (a) corpus representativeness is now the
safety mechanism and gets commensurate investment in Phase 0; (b) the internal default-off
engine flag exists only for dual-engine development/CI and is never a shipped, user-facing
toggle; (c) the Phase 5 safety window is revert-by-release (old engine stays in-tree, unreachable,
for one release), not a runtime switch.

- Host `-D` system properties stop silently overriding POM properties (today's
  `System.getProperty` read inside interpolation makes resolution output depend on host JVM
  flags — this also fixes the standing determinism principle).
- Profile `<property>` activation evaluates Maven properties instead of environment variables;
  `<os>`/`<file>` activation starts working.
- The type filter widens: test-jar/war/aar/maven-plugin dependencies stop being silently dropped.
- Version-range semantics become Maven's (intersection; duplicate-range last-wins divergence
  goes away).
- Cross-scope version shifts under single-collect mediation (reinstated with the N1 decision):
  rare cases where test/provided roots conflict with compile transitives resolve to the
  single-mediation answer; blast radius sized on the Phase-3 shadow corpus before the flip.
- `ResolutionEventListener.clear()` never fires (no restart loop); event ordering shifts.
- The seeded `resolveDependencies(…, Map<GroupArtifact, VersionRequirement>)` overload becomes a
  deprecated no-op-seed facade.

- Cycle/self-parent/import-cycle tolerance removed: Maven-identical failures via per-file
  `ParseExceptionResult`; existing tolerance tests inverted into Maven-parity pinning tests.
- Repository ordering aligns to Maven's where the oracle shows an observable difference.

Not changing (deliberately, all non-semantic): negative-caching/perf stance, `MavenSettings` as
the settings model, settings `<proxies>` remaining the injected `HttpSender`'s concern, the
frozen tree API and per-scope output shape.

## Open questions for Jon

1. **Cache format coordination** — pom-bytes region + `Pom.modelVersion` bump need host-side
   (Moderne CLI) rollout coordination.
2. **Scope-mediation strategy** — settled by the large-reactor baseline
   (`benchmarks/baseline-2026-07-08.md`): **single verbose collect + per-scope projection**.
   The small-reactor data had favored per-scope collects; at camel scale four collects are
   slower than the old engine at 50–150 modules and scale ~3.5× steeper — N1 wins every tier.
   Cross-scope version shifts return to the ledger, sized on the Phase-3 shadow corpus.

Resolved 2026-07-07: JDK-floor/Maven-4-timing — committed to the 3.9 stack, no scheduled Maven 4
work (see "Why this stack"). Behavior-change budget — single flip-over, no compat flags, gated
on corpus proof (see "Known behavior changes"). Maven-parity absolutism — divergence = bug, no
lenient mode, pinning unit tests for every divergence found (see "Maven parity policy").
New from that policy: keep/kill call on HTML-index metadata derivation (gradle-metadata
injection resolved: removed from Maven parsing as a parity bug, kept as an opt-in engine option
used only by rewrite-gradle's marker-approximation path).

## Document index

- **`DESIGN.md` — the canonical detailed design (start here for implementation)**
- `SPIKE-RESULTS.md` — verdicts and corrections from the four runnable spikes (`spike/`)
- `study/a1-resolution-algorithm.md` — behavioral spec of the custom algorithm (the
  must-reproduce list is §5)
- `study/a2-seams.md` — the five pluggable seams + failure-caching semantics table
- `study/a3-api-surface.md` — compat surface: wire format, identity contracts, consumer sweep,
  21-item MUST checklist
- `study/a4-divergence-catalog.md` — ~94 pinned behaviors with test/commit anchors; 15 ranked
  acceptance tests
- `study/a5-resolver-apis.md` — resolver extension points mapped to rewrite needs
- `study/a6-model-building.md` — ModelBuilder 3.9 vs Maven 4, provenance, embeddability
- `study/a7-embeddability.md` — footprint, JDK, shading facts, locking, failure-cache defaults
- `design/design-full-delegation.md` + `design/critique-full-delegation.md`
- `design/design-incremental-strangler.md` + `design/critique-incremental-strangler.md`
