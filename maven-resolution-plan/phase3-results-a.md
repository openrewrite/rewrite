# Phase 3 slice A — dependency collection service

2026-07-08, worktree `phase3-collector` (uncommitted). New classes in `org.openrewrite.maven.internal.engine`
(rewrite-maven), composed on the Phase-2 machinery (`EngineEffectivePom`, `CacheBridge`, `ReactorWorkspace`,
`EffectiveSettings`, `ModelParityErrorMapper`) and the shaded engine module. Scoped build green:
`:rewrite-maven:compileJava`, `:rewrite-maven:test --tests …engine.EngineDependencyCollectorTest` (12/12),
full `…engine.*` package (64/64), `:rewrite-maven:licenseMain/licenseTest`. **No facade/tree/parity code touched.**

## B-facing API (what the graph mapper, slice B, consumes)

- `EngineDependencyCollector implements Closeable` — one collect-side `RepositorySystem`
  (`EngineCollectSystemSupplier`) + a shared `RepositoryCache`, built once and reused.
  `close()` shuts the system down.
  - `EngineCollectOutcome collect(Model rootEffectiveModel, Pom requested, List<MavenRepository> requestRepositories,
    EffectiveSettings settings, ReactorWorkspace reactor, Path scratch, ExecutionContext ctx)` — runs **one** verbose
    collect. `rootEffectiveModel` is Phase-2's `ModelBuildingResult.getEffectiveModel()`; its `getDependencies()` seed
    the direct deps (all scopes, versions already DM-injected by model building) and its `getDependencyManagement()`
    seeds `managedDependencies`. Never throws for resolution failures — everything is in the outcome.
- `EngineCollectOutcome` (`@Value`, public):
  - `@Nullable DependencyNode getRoot()` — the verbose root (`setRootArtifact`, so no root descriptor read). All
    scopes; losers retained childless with `ConflictResolver.NODE_DATA_WINNER`; premanaged state via
    `DependencyManagerUtils.getPremanaged{Version,Scope,Optional,Exclusions}` + `getManagedBits()`.
  - `Map<ResolvedGroupArtifactVersion, MavenRepository> getServedBy()` — `gav → repository` for every descriptor read
    (the dep's own pom via `CacheBridge`, its parents/BOMs via `EngineEffectivePom`). Reactor members are absent
    (null repository = project/reactor dep).
  - `List<DependencyCycle> getCycles()` — `CollectResult.getCycles()`, recorded + tolerated.
  - `List<MavenDownloadingException> getDirectFailures()` — depth-1 deps whose descriptor Maven tolerated but rewrite
    fails on, plus any hard `DependencyCollectionException`, already shaped with root-GA attribution +
    `repositoryResponses`. **Slice B aggregates these into `MavenDownloadingExceptions` and attaches `partialResult`.**
  - `List<GroupArtifactVersion> getToleratedTransitiveFailures()` — transitive missing/invalid descriptors (warn
    events; never a failure).
  - `int getDescriptorReads()` — effective-model builds this collect performed (0 on a warm re-collect).
- Supporting (not called directly by B): `EngineDescriptorReader`, `PinnedVersionResolver`,
  `EngineCollectSystemSupplier`, `DependencyConversions`, `CollectContext` (package-private; travels on the session
  config property `SESSION_KEY`, mirroring `HttpSenderTransporterFactory`'s sender lookup).

## Session / collector config vs DESIGN §4.2

| §4.2 knob | Set | How |
|---|---|---|
| single verbose collect, all roots (N1) | ✓ | `setRootArtifact(root)` + `setDependencies(all effective deps)` + `setManagedDependencies(effective DM)` — the benchmark's "test"-widest seed |
| `ConflictResolver` `Verbosity.STANDARD` (the enum) | ✓ | `setConfigProperty(CONFIG_PROP_VERBOSE, Verbosity.STANDARD)` |
| `DependencyManagerUtils.CONFIG_PROP_VERBOSE` | ✓ | `setConfigProperty(…, Boolean.TRUE)` |
| `ClassicDependencyManager` | ✓ | inherited unchanged from `SessionBuilderSupplier` (mvn3 → `getDependencyManager(false)`) |
| stock exclusion selector (exact/`*`, no glob) | ✓ | inherited `AndDependencySelector(Scope, Optional, Exclusion)`; `ExclusionDependencySelector.matches` = `"*".equals(pattern) || pattern.equals(value)` |
| session template mirrors Maven 3.9 factory | ✓ | replicates `MavenEngine.newSession` (checksum ignore, `updatePolicy=null`, `SimpleResolutionErrorPolicy(0, CACHE_NOT_FOUND)`, `SimpleArtifactDescriptorPolicy(true,true)`, RRF sources off, USER_AGENT, per-run scratch LRM) |
| descriptor reader over Phase 2 | ✓ | `EngineCollectSystemSupplier.createArtifactDescriptorReader()` → `EngineDescriptorReader` (builds each descriptor with `EngineEffectivePom`) |
| pinned snapshots win transitively | ✓ | `createVersionResolver()` → `PinnedVersionResolver(super)` |

## Maven tolerance decisions (mirrored, with source lines)

Reference: Maven 3.9 `org.apache.maven.repository.internal.DefaultArtifactDescriptorReader.loadPom`
(`:191-315`) + `ArtifactDescriptorReaderDelegate.populateResult` (`:52-131`).

| Case | Maven behavior | `EngineDescriptorReader` | Test |
|---|---|---|---|
| dep's own pom missing (404) | `IGNORE_MISSING` → empty descriptor (`:237-246,396-402`) | tolerate; record in `descriptorFailures(missing)` | `missingTransitiveTolerated` |
| unresolvable **parent/BOM** of a dep | **always throws** `ArtifactDescriptorException` (`:285-291`) | `MavenDownloadingException` from build → throw → collect fails hard | (covered by tolerance-split logic; `EngineEffectivePomTest` proves the model-side map) |
| model otherwise invalid (cycle, bad packaging) | `invalidDescriptor` + `IGNORE_INVALID` → empty descriptor (`:292-298`) | tolerate; record `descriptorFailures(invalid)` | — |
| `<distributionManagement><relocation>` | followed with g:a:baseVersion cycle guard (`:218-314`) | same loop, `DefaultArtifact` for the relocated coordinates | — |
| Model → descriptor conversion | type→stereotype, `systemPath`→`local.path`, exclusions→`g:a:*:*` | `DependencyConversions` (verbatim port of the delegate) | `managed…PremanagedState`, `optional…` |
| dependency cycle | recorded in `CollectResult.getCycles()`, tolerated | surfaced in `EngineCollectOutcome.cycles` | `dependencyCycleRecordedAndTolerated` |
| session policy | `SimpleArtifactDescriptorPolicy(true,true)` (`MavenRepositorySystemUtils.newSession :176`) | same on the collect session | — |

**Direct-vs-transitive split** (KEEP_REWRITE, matches the legacy downloader): Maven tolerates *all* missing/invalid
descriptors during collect. Rewrite additionally fails a **direct** (depth-1) dependency whose descriptor was tolerated
— `EngineDependencyCollector` walks the graph, matches `descriptorFailures` by depth, and emits a
`MavenDownloadingException` for depth-1, a `toleratedTransitiveFailures` entry otherwise. Verified by
`missingDirectFails` / `missingTransitiveTolerated`.

## Deviations / notes for slice B

1. **N1 substrate = the widest-scope graph.** One collect seeded with every scope's direct deps under the **stock**
   selectors, so transitive test/provided and optional-beyond-direct are already dropped (as the benchmark's N1
   measured). Slice B projects the narrower scopes by filtering this graph. The cross-scope version-shift change stays
   ledgered (DESIGN §0).
2. **Separate `RepositorySystem` from `MavenEngine`'s** — the descriptor reader must be wired into the system, so the
   collector builds its own via `EngineCollectSystemSupplier`; its transport + RRF-off overrides duplicate the engine
   module's package-private `EngineRepositorySystemSupplier` (that class cannot be extended cross-module). The session
   template still mirrors `MavenEngine`'s knob-for-knob.
3. **Metadata-region routing not wired.** `PinnedVersionResolver` fully implements pinning (dated snapshot wins with no
   metadata read, transitively — `pinnedSnapshotHonoredTransitivelyWithoutMetadata`). Non-pinned metadata reads keep
   flowing through the resolver's own `MetadataResolver`/LRM (there is no existing `CacheBridge` metadata path to
   compose with); `MavenPomCache`-metadata-region routing is deferred to Phase 4 cache hardening.
4. **Warm re-collect avoids model rebuilds via the DataPool**, not the ModelCache — `EngineEffectivePom` keeps its
   per-build fresh `ModelCache` (the Phase-2 servedBy-completeness requirement), but the shared session
   `RepositoryCache`'s descriptor pool means a second collect never re-invokes `EngineDescriptorReader`
   (`warmReCollectRebuildsNoModels`: `descriptorReads == 0`; `warmReCollectPerformsNoNetwork`: 0 requests).
5. **pom-less-jar stubbing** stays at the `CacheBridge` layer (unchanged), per the deliverable.

## Tests (12, hermetic MockWebServer, no live network)

winner/loser/depth + `NODE_DATA_WINNER` (a); nearest-wins at unequal depth (b); exclusions exact + `*` (c); optional
direct kept / transitive skipped (d); managed version/scope/exclusion as premanaged state (e); pinned transitive
snapshot without metadata (f); reactor member served from workspace as a transitive (g); dependency cycle recorded +
tolerated (h); missing transitive tolerated / missing direct fails (i, two tests); warm re-collect zero network +
zero model rebuilds (j + perf sanity, two tests). Existing 52 `…engine.*` tests still green.
