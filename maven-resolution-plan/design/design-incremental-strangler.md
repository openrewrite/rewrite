> **SUPERSEDED (2026-07-07)** by `../DESIGN.md`, which merges this design with full-delegation,
> both critiques, four spike results, and Jon's decisions (parity absolutism — the "intentional
> divergences" preserved here are now bugs to fix; no compat flags; XML-first model input).
> Retained for the record.

# Replacing rewrite-maven's custom resolution with embedded Maven
## Design: the Incremental Strangler

Author basis: research reports a1–a7 (`scratchpad/study/`). All behavioral claims below are
grounded in those reports; file:line citations refer to the worktree at `75ffca60d3` and the
apache clones studied in a5–a7.

---

## 1. Summary and goals

rewrite-maven reimplements Maven resolution (~4,500 lines across `ResolvedPom`,
`MavenPomDownloader`, `VersionRequirement`, `RawPom`) and has accumulated a 94-entry
edge-case catalog (a4) of behaviors pinned by tests, roughly half of which are attempts to
*match* Maven and keep drifting. This design replaces the semantic core with the real Maven
stack — maven-model-builder for effective-POM construction, maven-resolver (Aether 2.x) for
version machinery, collection, and mediation — while:

- keeping `org.openrewrite.maven.tree.*` byte-for-byte API- and wire-compatible (a3 checklist);
- keeping `MavenPomCache` / `MavenArtifactCache` pluggable with today's semantics;
- keeping the warm hot path at parity by preserving today's L1 caches at the same seams;
- preserving the intentional divergences (cycle tolerance, warn-and-continue, dead-repo
  memory, Gradle module metadata injection) as *policy wrappers around* Maven, never as a
  parallel semantic engine.

The migration is a strangler: four production slices, each releasable and green against the
existing 10k+ lines of resolution tests, preceded by a differential-oracle phase that turns
"parity" from an aspiration into a measured quantity before any production code changes.

**End state:** Maven code owns every semantic decision (effective pom, profile activation,
inheritance, BOM import precedence, mediation, scopes, ranges, snapshots). What survives
custom, permanently: the frozen tree API and its `RawPom → Pom` requested-model parser; a
facade (`MavenPomDownloader` keeps its public constructors/methods); ~10 small adapters at
documented Maven SPI seams; two mappers (effective model → `ResolvedPom`, dependency graph →
`ResolvedDependency`); and a leniency policy that decides what to do when Maven would abort.
None of that re-decides Maven semantics.

---

## 2. Chosen stack

**`org.apache.maven.resolver:maven-resolver-supplier-mvn3:2.0.x`** — resolver 2.x
(BF collector, file-lock interprocess locking by default, `ResolutionErrorPolicy`) wired to
**Maven 3.9.16's** `maven-resolver-provider` + `maven-model-builder`.

Why this and not Maven 4's `maven-impl`:

1. **Java 8.** rewrite ships Java 8 bytecode (a7 §0). Only the mvn3 supplier stack runs on
   Java 8; the entire Maven 4 stack (and supplier-mvn4) requires Java 17. This alone decides
   the near term.
2. **Parity target.** "Divergence elimination" means matching the Maven users actually run.
   In 2026 that is overwhelmingly 3.9.x (`ClassicDependencyManager`, first-wins BOM imports,
   no MNG-5600 import exclusions). Targeting 4.x semantics would *introduce* divergence
   against most user builds.
3. **No DI baggage.** The supplier is plain-`new` wiring with ~60 `protected create*()`
   override points; zero sisu/guice/plexus-container/wagon at runtime; ~20 Java 8 jars
   (a7 §1a). Jon's founding reason #1 is solved upstream.
4. **Escape hatch designed in.** All bootstrap goes through one rewrite-owned factory
   (`MavenEngine`, §3). Swapping to supplier-mvn4 / `maven-impl` later (Maven 4 GA + JDK-17
   floor) changes that factory and two adapters (`RecordingDependencyManagementImporter`
   becomes `importedFrom` reads; `RequestCacheFactory` replaces `ModelCache`), nothing above it.

The stack is **shaded and relocated once, at the library boundary** (§9), so all four
embedding environments see only `org.openrewrite.maven.shaded.*` classes.

---

## 3. End-state architecture

### 3.1 Component diagram

```
                       recipes / MavenVisitor / rewrite-gradle / hosts
                                          │
        ┌─────────────────────────────────┴──────────────────────────────────┐
        │                 org.openrewrite.maven.tree.*  (FROZEN API)         │
        │  MavenResolutionResult · ResolvedPom · Pom · ResolvedDependency    │
        │  Dependency · ManagedDependency · MavenRepository · Scope · ...    │
        └───────────────┬───────────────────────────────┬────────────────────┘
                        │ entry points (unchanged)      │
        Pom.resolve / ResolvedPom.resolveDependencies   │ MavenParser / UpdateMavenModel
                        │                               │
        ┌───────────────▼───────────────────────────────▼────────────────────┐
        │        MavenPomDownloader (public facade, API unchanged)           │
        │        + org.openrewrite.maven.engine  (new, internal)             │
        │                                                                    │
        │  MavenEngine ──────────── one per ExecutionContext                 │
        │   ├─ SettingsAdapter          MavenSettings/ctx → session          │
        │   ├─ ReactorWorkspaceReader   project poms, relativePath tiers     │
        │   ├─ CachingDescriptorReader  MavenPomCache bridge + capture       │
        │   ├─ LenientModelBuilder      cycle-breaking, warn-not-abort       │
        │   ├─ RecordingDepMgmtImporter bomGav / requestedBom provenance     │
        │   ├─ PinnedVersionResolver    ctx pinned snapshots                 │
        │   ├─ ResolutionListenerBridge aether events → ResolutionEventListener
        │   ├─ DownloadingExceptionMapper → MavenDownloadingException(s)     │
        │   ├─ EffectivePomMapper       Model+lineage → ResolvedPom          │
        │   └─ DependencyGraphMapper    DependencyNode graph → ResolvedDependency
        └───────────────┬────────────────────────────────────────────────────┘
                        │ Maven SPI seams
        ┌───────────────▼────────────────────────────────────────────────────┐
        │   SHADED  org.openrewrite.maven.shaded.{org.eclipse.aether,        │
        │           org.apache.maven.model[.building], ...}                  │
        │                                                                    │
        │   DefaultModelBuilder (3.9.16)      RepositorySystem (resolver 2.x)│
        │   profile activation · inheritance  BfDependencyCollector + skipper│
        │   interpolation · BOM import        ConflictResolver (verbose)     │
        │   depMgmt injection                 VersionResolver/RangeResolver  │
        │                                     UpdateCheckManager · ErrorPolicy
        └───────┬──────────────────────┬──────────────────────┬──────────────┘
                │                      │                      │
     ┌──────────▼─────────┐ ┌──────────▼──────────┐ ┌─────────▼──────────────┐
     │ HttpSenderTransporter│ │ Local repository    │ │ Session RepositoryCache│
     │ (bridges to rewrite's│ │ head: ~/.rewrite/   │ │ (DataPool descriptor   │
     │  HttpSender; owns    │ │  cache/repository   │ │  pool, ModelCache) held│
     │  retry, auth-fallback│ │ tail: ~/.m2 (RO)    │ │  in MavenExecutionCtx  │
     │  unreachable-endpoint│ │ file-lock named     │ │  across parse runs     │
     │  set)                │ │ locks (interprocess)│ └────────────────────────┘
     └──────────┬─────────┘ └─────────────────────┘
                │
          HttpSender (host-injected: OkHttp / JDK / instrumented)

   Pluggable caches (interfaces unchanged):
     MavenPomCache  ── L1 for Pom / metadata / normalized repo / ResolvedPom-per-GAV
     MavenArtifactCache ── binary jars (MavenArtifactDownloader path)
```

### 3.2 New package and class inventory (readability contract)

One new package, `org.openrewrite.maven.engine` (internal, never serialized into LSTs):

| Class | Role | Approx size |
|---|---|---|
| `MavenEngine` | Owns the shaded `RepositorySystem` + a session template built from `MavenExecutionContextView`/`MavenSettings`. Cached on the context. The only place bootstrap happens. | 300 |
| `HttpSenderTransporter(Factory)` | Aether `Transporter` over rewrite's `HttpSender`. Owns `classify()` (404 → NOT_FOUND, else OTHER), Failsafe retry (timeouts only, max 5, 500ms+jitter), authenticated→anonymous fallback, per-server headers/timeouts, run-scoped `host:port` unreachable set. | 500 |
| `ReactorWorkspaceReader` | `WorkspaceReader` + `MavenWorkspaceReader` + `WorkspaceModelResolver` over the project-pom map; three-tier GAV matching + relativePath rules (incl. empty `<relativePath/>` opt-out, `${revision}` raw-GAV-first). | 350 |
| `CachingDescriptorReader` | Decorates `DefaultArtifactDescriptorReader`; consults/populates `MavenPomCache` (tri-state, negative 4xx-only) before Maven runs; captures per-GAV effective `Model` + source repository for the mappers; applies Gradle-module-metadata BOM injection and pom-less-jar synthesis. | 400 |
| `LenientModelBuilder` | Wraps `DefaultModelBuilder`: ancestry-tracking `ModelResolver` that breaks parent/BOM cycles with a stub model (today's silent-stop), maps `ModelProblem` severities to warn-and-continue, refuses obsolete `pomVersion` poms. Policy only — no merging logic. | 300 |
| `RecordingDependencyManagementImporter` | Wraps `DefaultDependencyManagementImporter` to record which BOM contributed each managed entry → `requestedBom`/`bomGav`. | 100 |
| `PinnedVersionResolver` | Decorates `DefaultVersionResolver`: `ctx.getPinnedSnapshotVersions()` wins; classifier-aware selection parity. | 100 |
| `EffectivePomMapper` | effective `Model` + raw lineage + activation results → `ResolvedPom` fields, threading original `Dependency`/`ManagedDependency` instances. Emits `parent`/`property`/`dependencyManagement`/`bomImport` listener events. | 500 |
| `DependencyGraphMapper` | verbose `DependencyNode` graph → per-scope `List<ResolvedDependency>` with depth, shared instances, `effectiveExclusions` recomputation, requested-instance threading. Emits `dependency` events. | 500 |
| `ResolutionListenerBridge` | `RepositoryListener`/`TransferListener` → the 13 `ResolutionEventListener` events (mapping table §5.6). | 200 |
| `DownloadingExceptionMapper` | aether/model exceptions → `MavenDownloadingException(s)` with per-repository responses and root-GAV attribution (table §5.7). | 250 |
| `SettingsAdapter` | `MavenSettings` + ctx knobs → repo list (rewrite ordering), `MirrorSelector`, `AuthenticationSelector`, `ProxySelector` (proxies finally honored), external profiles, user properties. | 300 |
| `PomToModelConverter` / `ModelSources` | `Pom` → maven `Model` for synthetic poms (rewrite-gradle headless path) and printed-XML `ModelSource`s for project poms. | 250 |
| `shadow.ResolutionShadowComparator` / `shadow.ResolutionDiff` | old-vs-new normalization + structured diff; test fixture and opt-in runtime shadow mode. | 400 |

Total new code ≈ 4.5k lines replacing ≈ 4.5k lines of semantic algorithm — but the new lines
are adapters and mappers (mechanical, testable in isolation), not a resolution algorithm.

Seam count: 7 Maven SPI implementations + 2 mappers + 1 facade. Every adapter implements a
documented upstream interface; nothing reaches into shaded internals beyond the supplier's
`create*()` overrides.

---

## 4. Entity mapping

### 4.1 Overview

| Maven API object | rewrite tree type | Direction |
|---|---|---|
| `org.apache.maven.model.Model` (effective, per lineage) | `ResolvedPom` | mapped by `EffectivePomMapper` |
| `Model` (raw) | `Pom` | **not mapped** — `Pom` keeps coming from `RawPom` (frozen requested model) |
| `org.eclipse.aether.graph.DependencyNode` (verbose) | `ResolvedDependency` | mapped by `DependencyGraphMapper` |
| `model.Dependency` (effective) | matched to existing `tree.Dependency` instance | identity threading, never new instances at depth 0 |
| `model.DependencyManagement` entry | `ResolvedManagedDependency` | with recorded BOM provenance |
| `model.Repository` / aether `RemoteRepository` | `MavenRepository` | id/uri/releases/snapshots; credentials never serialized |
| aether `Metadata` (files in LRM) | `MavenMetadata` | existing `MavenMetadata.parse` on the file bytes, existing `mergeMetadata` across repos |
| `ArtifactDescriptorException` / `ArtifactResolutionException` / `ModelBuildingException` / `VersionRangeResolutionException` | `MavenDownloadingException(s)` / `MavenParsingException` | §5.7 |
| `RepositoryListener` / `TransferListener` events | `ResolutionEventListener` events | §5.6 |

### 4.2 `ResolvedPom` field by field

| `ResolvedPom` field | Source in the Maven stack | Notes / traps |
|---|---|---|
| `requested` (Pom) | unchanged — `RawPom.toPom` | verbatim, placeholders unresolved, profiles reversed, mutable `properties` map (MavenParser/UpdateMavenModel mutate it in place — a3 MUST #8) |
| `activeProfiles` | request input | unchanged |
| `properties` | **merged from raw lineage models** (`ModelBuildingResult.getModelIds()` + `getRawModel(id)`), child-first, per-model active-profile properties before body, using **Maven's** activation decisions (`getActivePomProfiles(modelId)`) | Deliberate: the effective `Model.getProperties()` is *interpolated*; today's field holds *raw* merged values with lazy interpolation at `getValue()`. Merging raw lineage keeps the observable map raw while Maven owns *which* profiles/parents contribute. `getValue`/`getProperty` implementations unchanged (they are API — a3 §1.2). |
| `dependencyManagement` | effective `Model.getDependencyManagement()` + `RecordingDependencyManagementImporter` | Per entry: `gav` = interpolated GAV (as today); `scope` = `Scope.fromName`; `requested` = the `ManagedDependency.Defined` **instance** from the contributing pom's `Pom.dependencyManagement`, located via `InputLocation.getSource().getModelId()` (location tracking ON) then GACT match; `requestedBom`/`bomGav` = the `Imported` instance + resolved BOM GAV from the recorder. Lazy-sort + binary-search contract untouched (lives in the tree type). |
| `requestedDependencies` | effective `Model.getDependencies()` matched back to `Dependency` instances in the lineage `Pom`s (child GA wins, same as today's merge) | Instance threading is the point: seeds for collection are these instances, so `ResolvedDependency.requested == pom.getRequestedDependencies().get(i)` holds (a3 MUST #6). |
| `repositories` | effective `Model.getRepositories()` interpolated, **ordered by today's merge rules** (initial → child profile → child body → parents; id-dedup first-wins) | Repo *order* is embedder input, not Maven semantics; tests pin it (`repositoryOrder`, a4 #49). Central appended last if no `central` id — unchanged. |
| `initialRepositories` | unchanged semantics (frozen after first resolve) | |
| `pluginRepositories` | effective `Model.getPluginRepositories()` | **Behavior change (improvement)**: today they are dropped on resolve (a1 §2.6). Ledger entry, announced. |
| `plugins` / `pluginManagement` | effective `Model.getBuild().getPlugins()/getPluginManagement()`; `Xpp3Dom` → Jackson `JsonNode` converter | Fixes the parent-phase-overrides-child bug and HashSet goal reordering (a1 §2.7) — Maven's merge is now authoritative. `pluginManagement` remains *not applied onto* `plugins` (Maven applies it in phase 2 injection; we capture the pre-injection plugins list to preserve today's "consumers join the two lists" contract). |
| `subprojects` | raw model modules+subprojects | unchanged (from `Pom`) |
| identity contract | `resolve()` compares new vs old field-by-field and returns `this` when unchanged | mapper feeds the same comparison as today (a3 MUST #9) |

### 4.3 `ResolvedDependency` field by field

Input: one **verbose** collect per scope in `RESOLVE_SCOPES` order
(`ConflictResolver.CONFIG_PROP_VERBOSE=STANDARD`, `DependencyManagerUtils` verbose ON),
`ClassicDependencyManager`, `FatArtifactTraverser` off (rewrite descends everywhere),
custom glob-capable `ExclusionDependencySelector` subclass (preserves rewrite's glob
superset — implemented via Maven's own strategy seam, a4 #74).

| `ResolvedDependency` field | Source | Notes / traps |
|---|---|---|
| `gav.groupId/artifactId` | `node.getArtifact()` | |
| `gav.version` | `artifact.getBaseVersion()` | base `-SNAPSHOT` form — identity invariant (a1 §5.8) |
| `gav.datedSnapshotVersion` | `artifact.getVersion()` when ≠ baseVersion, else null | `PinnedVersionResolver` guarantees ctx pins win; timestamped *input* versions folded to base by the existing `handleSnapshotTimestampVersion` logic in the facade |
| `gav.repository` | source repository captured by `CachingDescriptorReader` per GAV (`ArtifactDescriptorResult.getRepository()`); `null` for workspace/reactor hits | rewrite semantics: "repository it was *resolved from*" |
| `requested` | depth 0: the seed `Dependency` instance (mapper keeps seed↔root-node map). Depth > 0: match GA+classifier+type against the declaring pom's `requestedDependencies` (declaring pom = parent node's GAV → `MavenPomCache.getResolvedDependencyPom`) | Preserves in-run `==` contract for `getResolvedDependency` (a3 §2.1). Transitive `requested` remains the *uninterpolated original* as today. |
| `dependencies` | children of the winning node, losers dropped; a GACT→`ResolvedDependency` identity map ensures the same instance is shared across all parents and the flat scope list (a3 MUST #7) | Verbose STANDARD graphs are "analysis only" (a5 §4.4) — we never resolve artifacts from them; jar downloads stay on the separate `MavenArtifactDownloader` path. |
| `licenses` | declaring GAV's `Pom.licenses` (cache lookup) | unchanged classification (`License.fromName`) |
| `type` / `classifier` | requested values (getter defaults "jar") | |
| `optional` | `Boolean.valueOf(requested.getOptional())` | quirk preserved (a1 §4.20) |
| `depth` | BFS depth in the mapped tree | |
| `effectiveExclusions` | recomputed by the mapper: accumulate requested+managed exclusions along the winning path, attribute to the shallowest declaring ancestor | pure post-processing on the result graph — no custom mediation. Matches a1 §5.5 / #8117. |
| scope-list membership | per-scope collect result; roots included per `dScope == scope || transitiveOf` rule at seed time (mapper applies today's seed filter and root-scope=Compile convention) | keeps the four *pure* non-cumulative lists (a1 §3.7) — this packaging is rewrite API, derived from Maven-mediated graphs |

### 4.4 Requested side (unchanged by construction)

`Pom`, `Parent`, `Profile`, `ProfileActivation`, `Dependency`, `ManagedDependency`,
`Plugin`, `License`, `Prerequisites` keep coming from `RawPom` exactly as today. The
requested model is *provenance*, not semantics; keeping it custom preserves wire format,
profile reversal, string trimming, and every null-tolerant getter without any mapping risk.
(One consequence: `ProfileActivation` still doesn't model `<os>`/`<file>` — those now
*work* during resolution because Maven reads the real XML, but the requested-side type
doesn't grow fields until we choose to add nullable ones.)

---

## 5. Seam-by-seam adapter designs

### 5.1 POM cache (`MavenPomCache` — interface unchanged)

The four sub-caches keep their exact tri-state semantics and keys:

1. **Raw `Pom` by `(repoUri,g,a,v,datedSnapshot)`** — consulted by the facade before any
   engine call (warm hit = no aether). On miss, the engine resolves the pom **file** into the
   LRM, `RawPom.parse`s it, and populates the cache. Negative entries: written only for
   deterministic 4xx (except 408/425/429) — the transporter's `classify()` + the mapper
   preserve this; RocksDB continues to drop negatives (a2 §1.8).
2. **Metadata by `(repoURI, gav)`** — L1 over aether's `MetadataResolver`; per-repo results
   merged with the existing `mergeMetadata` (union of versions, `Semver.max`, a2 §1.3).
3. **Normalized repository** — retained for the preserved https-preference probe (§5.3).
4. **`ResolvedPom` per GAV (`getResolvedDependencyPom`)** — becomes the L1 *above the model
   builder*: on hit, `LenientModelBuilder` is never invoked. This is the single most
   important hot-path preservation (see §6). Known key-contamination caveat (GAV only,
   ignores profiles/repos — a1 §4.19) is retained as-is; fixing it is a separate,
   announced improvement.

Where does the model builder get XML? From the **LRM file** (remote poms land as files in
`~/.rewrite/cache/repository` via Phase 1; project poms are fed as printed-XML
`ModelSource`s; synthetic poms via `PomToModelConverter`). No `MavenPomCache` interface
change is required. `Pom.getModelVersion()` bumps to 4 in Phase 2 (cheap cache rebuild;
LST wire format untouched).

Two-layer failure memory, documented: rewrite's tri-state negative cache (in-memory,
process-lifetime) sits above aether's persistent, TTL'd, auth-keyed `.lastUpdated` /
`resolver-status.properties` machinery with `SimpleResolutionErrorPolicy(CACHE_ALL)`
(a5 §2.2, a7 §4). Net effect strictly stronger than today: dead repos are remembered
*across processes* with credential-change invalidation — the precise thing Jon built the
custom layer for, now provided and owned upstream.

### 5.2 Artifact cache + local repository

- **`MavenArtifactCache` interface unchanged.** `MavenArtifactDownloader` keeps its API and
  cache-first hot path; its cold path delegates to the engine's `ArtifactResolver` (file in
  LRM) and then `putArtifact`s into the pluggable cache, preserving the observable layout
  (`<g>/<a>/<v>/<a>-<versionOrDated>[-classifier].jar`) and the non-dated-SNAPSHOT
  always-refetch rule (a2 §2.5–2.6).
- **Local repository** = resolver split/chained LRM: head = `~/.rewrite/cache/repository`
  (writable, private), tail = `~/.m2/repository` **read-only** (`withLocalRepositories`) —
  reproducing today's `ReadOnlyLocalMavenArtifactCache.mavenLocal()` composition without ever
  writing to `~/.m2`. Interprocess safety via the resolver-2.x default `file-lock` named
  locks + `file-gaecv` mapper on the head (a7 §3): founding reason #3 satisfied natively.
- `MAVEN_LOCAL_USER_NEUTRAL` substitution for cache-key stability preserved in the facade.
- No custom `LocalRepositoryManager` is needed in the base design; the SPI
  (`LocalRepositoryManagerFactory`, priority-selected) remains the documented extension
  point if a host needs a content-addressed store later.

### 5.3 HTTP transport (`HttpSenderTransporter`)

All network I/O continues to flow through the context-provided `HttpSender` (a2 §5, hard
host requirement). The transporter is a "dumb byte pump with an error classifier" (a5 §3.3),
~500 lines:

- `classify(Throwable)`: `HttpSenderResponseException` 404 → `ERROR_NOT_FOUND`; everything
  else → `ERROR_OTHER`. This single method feeds all of aether's not-found vs transfer-error
  caching — it gets its own exhaustive unit test.
- `get/peek`: GET/HEAD via `HttpSender`, basic auth from the repo's aether `Authentication`
  (populated by `SettingsAdapter` from settings servers / ctx credentials), per-server
  `httpHeaders` and connect/read timeouts, Failsafe retry (SocketTimeout/Timeout only, max
  5, 500 ms + 10 % jitter), authenticated→anonymous fallback on non-408/425/429 4xx with
  original-exception rethrow on 401–403 (a2 §1.6) — all verbatim ports of today's behavior,
  now living *below* Maven instead of beside it.
- **Run-scoped unreachable-endpoint set**: connect-level failure records `host:port` into
  `ctx.getUnreachableEndpoints()`; subsequent tasks to that endpoint fail fast and the
  bridge emits `repositoryAccessFailedPreviously` — today's exact behavior and event (a2
  §1.5), preserved because *we own the transporter*.
- `resolutionTime` accounting: wall-clock accumulated per request into
  `ctx.recordResolutionTime`, as today.
- Preserved augmentations that live at this layer: **https-preference probe** (one-time URL
  normalization per repo per run, backed by the normalized-repo cache sub-cache #3),
  **derived metadata** (HTML-index scrape fallback when `maven-metadata.xml` is 404 —
  implemented in the facade's metadata path, not inside aether), **pom-less-jar synthesis**
  (peek sibling `.jar` on pom 404 in `CachingDescriptorReader`; synthesize minimal jar
  `Pom`/descriptor), **Gradle module metadata BOM injection** (descriptor reader
  post-processing on `published-with-gradle-metadata` poms — load-bearing for
  rewrite-gradle, a4 #65). The hardcoded spring-milestone version filter (a4 #46) is
  **dropped** with a ledger entry.

### 5.4 Settings (`SettingsAdapter`)

`MavenSettings` remains the only settings model (a2 §7.5); the adapter translates per
session:

- servers → `AuthenticationSelector` (+ decrypted passwords via existing
  `MavenSecuritySettings`); unresolved `${` credentials → no credentials (a4 #57).
- mirrors → repo-list rewriting **using rewrite's existing `MavenRepositoryMirror`
  matching** applied while assembling the request repository list (not the session
  `MirrorSelector`, which only applies to descriptor-discovered repos — a5 §2 table). This
  keeps mirror semantics (`*`, `external:*`, `!repo`, local-never-mirrored, first-match)
  bit-identical to today's tested behavior.
- proxies → `ProxySelector` (**new capability**: settings proxies finally honored; today
  parsed-but-ignored, a2 §3 / a4 #68 — announced).
- settings/ctx profiles → external profiles on the `ModelBuildingRequest` + repo-list
  contributions; `MavenParser.builder().property()` and ctx user properties → request
  **user properties** (Maven's `-D` semantics — fixes the env-var profile-activation
  divergence and the system-props-beat-pom-props divergence at resolution time, a4 #28/#38;
  `getValue()`'s read-time `System.getProperty` fallback is API and stays).
- Repository list assembly keeps today's order and tri-states: `addLocalRepository` /
  `addCentralRepository` per-constructor defaults, `knownToExist`, ctx-injected repos
  merged with settings-profile repos enriched by same-id credentials (a2 §4).
- `effectiveSettings(mrr)` merge and `withServers(null)` marker sanitization untouched.

### 5.5 Workspace / reactor (`ReactorWorkspaceReader`)

Implements aether `WorkspaceReader` (+ `MavenWorkspaceReader` to serve `Model`s without
re-parsing) and model-builder `WorkspaceModelResolver`, backed by the same
`projectPoms`/`projectPomsByGav` maps the downloader holds today:

- exact resolved-GAV match → raw g:a + raw-or-property-merged version match → relativePath
  file match (default `..`, `/pom.xml` appended for directories, `:`-path skip, GA(V)
  verification with relaxed `${` version) — the three tiers verbatim (a2 §1.2, a4 #1–#6).
- Parents whose version still contains `${...}` are tried against the reactor with the raw
  GAV **before** interpolation (CI-friendly versions, a4 #6/#12) — implemented in
  `findVersions`/`resolveRawModel`.
- `.m2`-relative parents are never treated as reactor members (a4 #13).
- Consulted before LRM and any remote I/O by `DefaultArtifactResolver` and by the model
  builder's `readParentLocally` (a5 §3.2) — the same short-circuit guarantee
  `UpdateMavenModel` relies on for mid-mutation re-resolution.

`UpdateMavenModel` keeps its contract: it mutates the requested `Pom` (properties map in
place, etc.) and re-resolves; the engine feeds the model builder the **printed XML of the
mutated document** where available, falling back to `PomToModelConverter` for synthetic
poms (rewrite-gradle's `Pom.builder()` headless path, a3 MUST #20). Note: printed-XML input
means `<build><plugins>` edits are now also visible to re-resolution — a strict superset of
today's re-read set, recorded in the ledger as an announced improvement.

### 5.6 Event listeners (`ResolutionListenerBridge`)

| `ResolutionEventListener` event | New emission point |
|---|---|
| `download(gav)` | `artifactResolving` for `.pom` artifacts (bridge filters by extension) |
| `downloadSuccess(resolvedGav, containing)` | `artifactResolved` with file present |
| `downloadError(gav, attemptedUris, containing)` | `DownloadingExceptionMapper` — attempted URIs accumulated from `TransferResource` records per request trace |
| `downloadMetadata(gav)` | `metadataResolving` |
| `repository(repo, containing)` | facade, once per repo per request, when the request repo list is materialized (same point as today) |
| `repositoryAccessFailed(uri, t)` | transporter connect-level failure |
| `repositoryAccessFailedPreviously(uri)` | transporter unreachable-set hit; also emitted when aether replays a cached failure (`UpdateCheckManager`-synthesized exception detected by the mapper) |
| `parent(parentPom, containing)` | `EffectivePomMapper`, walking `getModelIds()` lineage |
| `property(key, value, containing)` | `EffectivePomMapper` during raw-lineage property merge |
| `dependencyManagement(managedDep, containing)` | `EffectivePomMapper` per mapped entry |
| `bomImport(gav, containing)` | `RecordingDependencyManagementImporter` |
| `dependency(scope, resolved, containing)` | `DependencyGraphMapper`, exactly once per resolved node per scope |
| `clear()` | **never fires** — aether has no restart loop. See §11. |

### 5.7 Error mapping (`DownloadingExceptionMapper`)

| Maven exception | rewrite exception / behavior |
|---|---|
| `ArtifactNotFoundException` (pom) | `MavenDownloadingException` with per-repository responses; cached replays phrased "Did not attempt to download because of a previous failure…" (message parity — pinned by `MavenMetadataFailures`, a4 #91) |
| `ArtifactTransferException` | `MavenDownloadingException`, repo responses from transfer records; never negatively cached in `MavenPomCache` |
| `ModelBuildingException` on a **dependency/parent** pom | degrade per `SimpleArtifactDescriptorPolicy(true,true)` + `LenientModelBuilder`: warn-and-continue, matching "POM is invalid, transitive dependencies will not be available" (a4 #82 — explicitly verified against `mvn` output today) |
| `ModelBuildingException` on the **project** pom | non-fatal problems → resolution proceeds; fatal (that Maven would abort on) → leniency policy: cycle-break for parent/BOM cycles (a4 #8/#16/#83), circular-property sentinel behavior, else `MavenDownloadingException` → `ParseExceptionResult` marker + `e.warn(doc)`, parse never fails (a3 MUST #15) |
| `VersionRangeResolutionException` / `UnsolvableVersionConflictException` | `MavenParsingException` carrying the range text (message parity with `invalidRange`, a4 #42) |
| per-node collect failures | accumulated into `MavenDownloadingExceptions` with root-GA attribution via `RequestTrace`/`CollectStepData` (a5 §3.4); jar/ejb-typed failures accumulate, other types report to `ctx.getOnError` and continue (a1 §3.9); per-GA dedup across scopes in `MavenResolutionResult.resolveDependencies` unchanged |
| depth-0 versionless dependency | pre-collect validation in the facade → `MavenDownloadingException("No version provided…")` (a4 #80) |
| unresolved `${…}` in final GAV | pre-collect validation → `MavenDownloadingException` "Could not resolve property" (a4 #81) |

### 5.8 Leniency policy — where rewrite must be *more* forgiving than Maven

This is the one custom layer that survives on principle, and it is policy, not semantics:

- **Parent/BOM cycles**: ancestry-tracking `ModelResolver`; on GAV repeat, return a stub
  minimal model → traversal stops exactly like today's silent break. Maven would FATAL.
- **Self-recursive dependencies**: graph mapper cycle-guard (identity-visited set) — Maven
  aborts; rewrite must produce an acyclic tree (a4 #83).
- **Circular properties / `${project.version}` chains**: interpolation problems downgraded;
  the `"error.circular.project.version"` sentinel behavior retained in `getProperty` (which
  is unchanged API anyway).
- **Warn-and-continue everywhere Maven aborts the build**: rewrite parses what exists in the
  wild; the LST must always materialize, with markers.

---

## 6. Hot-path walkthrough and performance

Scenario: fully warm caches, 300-module reactor, ~2,500 distinct dependency GAVs, no
network.

**Today (baseline):** per module — Jackson-XML `RawPom.parse`; `Pom.resolve` runs three
ancestry walks (parents from the project map, no I/O); `resolveDependencies` × 4 scopes:
BFS where each node hits `MavenPomCache.getResolvedDependencyPom` (Caffeine, in-memory) and
`VersionRequirement` map ops. Zero network. Cost ≈ XML parse + map merges + BFS.

**End state (warm):**

1. `RawPom.parse` — unchanged (requested `Pom` still built the same way).
2. Effective pom: `MavenPomCache.getResolvedDependencyPom` L1 hit for every **dependency**
   pom → `LenientModelBuilder` not invoked (identical to today's warm path). For the ~300
   **project** poms, the model builder runs with `ModelCache` RAW hits for shared parents.
   Known 3.9 wart: `ModelCacheTag` deep-clones on read; lineage depth ≈ 3–5 → ~1k model
   clones total, estimated tens of ms across the reactor. Mitigation if benchmarks demand:
   memoize assembled parent lineages per (GAV, activation-record) at our layer — the same
   trick Maven 4 ships (`readAsParentModel` caching, a6 §3) — still without re-deciding
   semantics.
3. Collection: 4 verbose collects per module against `DataPool` descriptor-pool hits (the
   shared session `RepositoryCache` is held in `MavenExecutionContextView` across the whole
   parse run and across `UpdateMavenModel` re-resolutions). BF skipper prunes foregone
   conflict losers before descriptor work. Zero I/O.
4. Mapping: `DependencyGraphMapper` allocates the `ResolvedDependency` graph — comparable to
   today's node construction; instance-dedup map keeps allocation proportional to distinct
   GACTs, and `InMemoryMavenPomCache.deduplicate()` discipline is retained.

**Expected warm delta: 1.0–1.5× baseline**, dominated by model-clone overhead on project
poms; **cold path is expected to be *faster* than today** — aether prefetches descriptors
5-way parallel and metadata 4-way parallel where today's downloader is fully serial
(a5 §4.1, a7 §5), and POM-only collection is first-class ("it will download the POMs only",
a7 §5).

**Benchmark gate (part of Phase 0, enforced every phase):** JMH suite in
`rewrite-benchmarks` — (a) warm 300-module synthetic reactor, (b) warm big-graph single pom
(spring-boot closure), (c) cold-with-replay-server variants. Regression budget: warm ≤ 15 %
per phase, ≤ 40 % cumulative until Phase 4 tuning, end-state target ≤ 15 % warm and ≥ 25 %
cold improvement. Numbers are gates, not aspirations: a phase does not ship red.

Additional speed levers unavailable today, unlocked for hosts: `UPDATE_POLICY_NEVER` +
session once-per-check dedupe; Remote Repository Filtering (prefix/groupId) to stop
N−1-wrong-repo leakage (the exact slow-build pathology Jon observed, a7 §4); HTTP/2 via a
future JDK-transport toggle.

---

## 7. The strangler path

Ordering rationale up front:

- **Oracle before everything** — the existing test suite is the parity oracle for *rewrite's*
  behavior, but only a differential harness measures *Maven's*. Building it first converts
  every later slice from "we believe" to "we measured", and it is the only phase with zero
  production risk.
- **Transport before model building** — not for divergence value (transport divergences are
  mostly intentional keepers) but because it (a) lands the shaded stack, bootstrap, and
  classpath story in production with minimal semantic change, (b) creates the **file
  substrate** (poms in the LRM) that the model builder and collector consume natively, and
  (c) immediately delivers founding-reasons #2/#3 (persistent failure caching, interprocess
  locking) from upstream code.
- **Model building before collection** — the collector consumes `ArtifactDescriptorReader`,
  whose semantics *are* model building; doing collection first would require throwaway glue
  (a descriptor reader over the custom effective-pom code — a torn abstraction built to be
  deleted). Model building is also where the divergence catalog concentrates (properties,
  profiles, DM/BOM precedence: entries 14–39 of a4).

### Phase 0 — Differential oracle + harness (no production change)

**Replaces:** nothing. **Seam:** test fixtures + opt-in runtime flag.

- Add supplier-mvn3 (unshaded) as a `testFixtures` dependency of rewrite-maven.
- `ResolutionShadowComparator`: run legacy and Maven engines on the same inputs; normalize
  both `MavenResolutionResult`s (sorted per-scope `gav:scope:depth:repo` sets, managed
  GACTV set, properties, repos) → structured `ResolutionDiff`.
- Corpus: (a) every pom fixture in the existing test suite; (b) a recorded real-world corpus
  (top-N Central poms + known-nasty reactor builds) served by an HTTP **record/replay
  server** so corpus runs are hermetic and fast. The replay server is also the substrate for
  the cold-path benchmarks.
- Opt-in runtime shadow mode: `MavenExecutionContextView.setResolutionComparisonEnabled`
  emits diffs to a data table — lets Moderne run the comparison across customer-scale corpora
  without behavior change.
- Output: the **divergence ledger** — every diff classified `ALIGN_TO_MAVEN` (bug we will
  fix by switching), `KEEP_REWRITE` (intentional; needs a preservation mechanism, listed in
  §5), or `NEW_BEHAVIOR` (announced improvement). The ledger is the acceptance contract for
  Phases 2–3; a4's 94 entries seed it.
- Cheap known fixes to the *custom* engine found/validated by the oracle land here if they
  shrink the future delta (candidate: none forced; env-var profile activation is deferred to
  Phase 2 where Maven fixes it wholesale).

**Green criterion:** harness runs in CI; ledger published; benchmarks recorded as baseline.
**Effort: 4 person-weeks.**

### Phase 1 — Transport, local repository, failure caching (under `MavenPomDownloader`)

**Replaces:** the HTTP/retry/normalization/negative-caching internals of
`MavenPomDownloader.download`/`downloadMetadata` and its snapshot-metadata fetch plumbing.
**Seam:** `MavenPomDownloader`'s public API (frozen); inside it, aether
`ArtifactResolver`/`MetadataResolver` via `MavenEngine`.

- Land the shaded artifact (`rewrite-maven-resolver` module, §9) and `MavenEngine` bootstrap.
- `HttpSenderTransporter`, split LRM (private head + read-only `~/.m2` tail), file-lock
  named locks, `SimpleResolutionErrorPolicy(CACHE_ALL)`, `SettingsAdapter` (repos, mirrors,
  auth, proxies).
- Facade behaviors preserved verbatim: reactor short-circuits (three tiers), LATEST/RELEASE
  named versions, classifier-aware dated-snapshot selection + ctx pinning, metadata merge
  across repos, derived-metadata scrape fallback, jar-without-pom synthesis,
  pom-without-jar rejection for file repos, Gradle module metadata injection,
  `MAVEN_LOCAL_USER_NEUTRAL`, https-preference probe, unreachable-endpoint set.
- Dropped (ledger): spring-milestone hardcoded filter; `0.0.0.0` substring block replaced by
  the same check at repo-list assembly.

**What stays custom at this stage:** all of `ResolvedPom` (effective pom + mediation),
`VersionRequirement`, `RawPom`. The custom algorithm now runs on Maven plumbing.

**Green criterion:** `MavenPomDownloaderTest` (1,784 lines) green except tests asserting
retired internals (each retirement is a reviewed ledger entry); `MavenParserTest` +
`ResolvedPomTest` untouched and green; warm benchmark within budget.
**Effort: 7 person-weeks** (transporter 2, LRM/bootstrap/shading 2, facade rewiring +
test triage 3).

### Phase 2 — Effective-POM construction on maven-model-builder

**Replaces:** `ResolvedPom.Resolver` (the ~1,000-line inner class: property/repository
merging, profile activation, parent traversal, depMgmt/BOM merging, plugin merging).
**Seam:** `ResolvedPom.resolve(ctx, downloader)` — signature, identity semantics, and all
getters unchanged.

- `LenientModelBuilder` (cycle-breaking `ModelResolver` + severity policy) over
  `DefaultModelBuilderFactory().newInstance()` with: `ReactorWorkspaceReader` as
  `WorkspaceModelResolver`; `ModelResolver` bridged onto the Phase-1 downloader; `ModelCache`
  bridged to the session `RepositoryCache`; location tracking **ON**;
  `RecordingDependencyManagementImporter`; external profiles + user properties from
  `SettingsAdapter`.
- `EffectivePomMapper` builds the `ResolvedPom` per §4.2, including instance threading and
  the same-instance-when-unchanged comparison.
- `MavenPomCache.getResolvedDependencyPom` stays the L1 for dependency poms (model builder
  only on miss). `Pom.getModelVersion()` → 4.
- Semantics now owned by Maven and *changing where rewrite was wrong* (all ledger-tracked,
  each with a dedicated test flip): property-based profile activation reads properties not
  env vars (a4 #38); `<os>`/`<file>` activation now works; system properties stop silently
  overriding pom properties at resolution time (user properties do, matching `-D`);
  plugin execution merge child-wins + stable goal order; profile `<build><plugins>`
  honored; `pluginRepositories` inherited; repository-order/interpolation asymmetries gone.
- Preserved divergences via the leniency layer: cycles, obsolete-pom refusal,
  circular-property sentinel, activeByDefault approximation *removed* (Maven's exact rule
  now applies — ledger).

**What stays custom at this stage:** graph collection/mediation (`doResolveDependencies` +
`VersionRequirement`) — they consume `ResolvedPom`'s unchanged API (`getValues`,
`getManagedVersion/Scope/Exclusions`) and don't notice the new producer.

**Green criterion:** full `ResolvedPomTest` + `MavenParserTest` + recipe suites green
(with reviewed ledger flips); oracle diff rate on the corpus for effective-pom fields
(properties, DM, repos, plugins) = 0 unexplained.
**Effort: 12 person-weeks** (leniency+bridges 4, mapper+instance threading 4, ledger-driven
test reconciliation 3, perf 1).

### Phase 3 — Collection and mediation on the aether collector

**Replaces:** `doResolveDependencies` (BFS + restart fixed-point), `VersionRequirement`
mediation, exclusion/optional/scope propagation, `contains`/dedup logic.
**Seam:** `ResolvedPom.resolveDependencies(scope, downloader, ctx)` /
`resolveDirectDependencies` — signatures and output shape unchanged.

- `CachingDescriptorReader` becomes the collector's `ArtifactDescriptorReader`
  (Phase-2 model builder underneath; per-GAV effective models already cached).
- Four collects per pom (verbose STANDARD, `ClassicDependencyManager`, glob-capable
  exclusion selector, scope selectors matching `RESOLVE_SCOPES` semantics), mapped by
  `DependencyGraphMapper` per §4.3. `resolveDirectDependencies` maps depth-0 only (no
  recursion) — fast path kept.
- `DownloadingExceptionMapper` + `ResolutionListenerBridge` complete the contract
  (exactly-once `dependency` events; `clear()` retired — §11).
- Mediation semantics now Maven's: real range intersection and
  `UnsolvableVersionConflictException` where rewrite picked highest-of-nearest (a1 §3.4 —
  mapped to `MavenParsingException` for message parity); DM of a dependency's own lineage
  applies to its own directs — **re-enable the `@Disabled`
  `dependencyManagementPropagatesToDependencies` test (#376), the flagship acceptance
  criterion** (a4 #23); type filter follows Maven's artifact handlers (test-jar/war now
  resolve — ledger, biggest observable change, quantified by the Phase-0 corpus run).
- `VersionRequirement` remains as a deprecated shim so the public
  `resolveDependencies(scope, Map<GroupArtifact,VersionRequirement>, …)` overload keeps
  linking (it converts seeds; internal package, near-zero usage).

**What stays custom at this stage:** nothing semantic. Facade, adapters, mappers, leniency.

**Green criterion:** the 15 highest-value acceptance tests of a4 §"15 highest-value" all
green (cycle suite via leniency, mediation suite via Maven); corpus diff rate 0 unexplained;
benchmarks within budget.
**Effort: 11 person-weeks** (collector wiring + scope-list derivation 3, graph mapper +
identity threading 4, error/listener parity 2, reconciliation 2).

### Phase 4 — Retirement and hardening

Delete `VersionRequirement` internals, `ResolvedPom.Resolver`, dead `MavenPomDownloader`
code (file shrinks from 1,314 lines to a ~300-line facade); performance tuning against the
Phase-0 baselines (parent-lineage memoization if needed); divergence-ledger release notes;
docs (`doc/adr/` entry for the architecture + the ledger as an appendix); delete shadow
harness from production paths (keep in tests permanently as the regression oracle).
**Effort: 5 person-weeks.**

### Phase 5 (optional, later) — Maven 4 line

When Maven 4 GA + a JDK-17 floor (or a multi-release packaging decision) allows: swap
supplier-mvn3 → mvn4 inside `MavenEngine`; `RecordingDependencyManagementImporter` →
`importedFrom` reads; `ModelCache` → `RequestCacheFactory`; decide `ClassicDependencyManager`
vs `TransitiveDependencyManager` per-target-Maven (possibly a parser option mirroring the
user's actual Maven version). Out of scope for estimates.

---

## 8. Shadow-comparison strategy (how we know it works)

1. **Existing test suite as oracle** — every phase must be green against `MavenParserTest`
   (5,249 lines), `MavenPomDownloaderTest`, `ResolvedPomTest`, `MavenDependencyFailuresTest`,
   `VersionRequirementTest`, settings/mirror/metadata tests, and all recipe suites. A test
   changes only with a ledger entry reviewed as `ALIGN_TO_MAVEN` or `NEW_BEHAVIOR`.
2. **Differential corpus in CI** (from Phase 0) — hermetic record/replay; both engines run;
   `ResolutionDiff` must be empty modulo the ledger. Kept green through Phases 1–3; at
   Phase 4 the "legacy engine" is frozen as a test-only artifact and the corpus becomes a
   pure regression suite for the new engine.
3. **Runtime shadow mode** — opt-in ctx flag; hosts (Moderne) run old+new across
   customer-scale corpora and emit diffs as a data table; this is the scale test the unit
   corpus cannot provide.
4. **Reference-identity assertions** — dedicated tests locking `==` contracts
   (`getResolvedDependency`, `getResolvedManagedDependency`, resolve()-returns-this,
   graph sharing) and an old-format serialized-LST fixture round-trip (pre-`@ref` payloads),
   per a3 §3.
5. **Benchmark gates** — §6.

---

## 9. Classpath strategy per environment

One decision covers all four: **publish `rewrite-maven-resolver`, a shaded jar relocating
`org.eclipse.aether`, `org.apache.maven.model(.building)`, `org.apache.maven.repository.internal`,
`org.apache.maven.artifact`, `org.codehaus.plexus.{util,interpolation}`, and Apache HTTP**
under `org.openrewrite.maven.shaded.*`; rewrite-maven depends on it. Relocation also strips
`META-INF/sisu` indexes, neutralizing container auto-registration (a7 §1a/§2).

| Environment | Situation | Handling |
|---|---|---|
| **Inside Maven** (rewrite-maven-plugin) | Maven's core realm force-exports `org.eclipse.aether.*` and `maven-model(-builder)` parent-first to every plugin (a7 §2) — unrelocated embedding is impossible (version skew: host 1.9.27 vs ours 2.0.x). | Relocated packages never collide with exported ones. We deliberately do **not** use the host's `RepositorySystem` — per-host-version behavior is exactly the divergence being killed. |
| **Inside Gradle** (rewrite-gradle-plugin) | Flat per-project buildscript classpaths; other plugins may carry their own resolver. | Same relocated jar; no interaction. |
| **Standalone / CLI** | Full classpath control. | Plain dependency on the shaded artifact. |
| **Server-side** (Moderne, long-lived JVMs) | Multiple parse workers, shared caches. | Shaded artifact; one `MavenEngine` per ExecutionContext; shared `RepositoryCache`/`MavenPomCache` injection unchanged; file-lock LRM makes concurrent workers on one cache dir safe — better than today's unsynchronized `LocalMavenArtifactCache`. |

Costs accepted: ~6–8 MB artifact; relocated names in deep stack traces (mitigated by keeping
adapter-layer exception messages self-describing); a shading build module to maintain
(license/NOTICE aggregation included).

Java 8 is preserved end-to-end (supplier-mvn3 stack is Java 8; transport-apache is shaded in;
if the runtime is known to be 11+, hosts may later prefer a JDK-transport variant — not in
scope).

---

## 10. Effort summary

| Phase | Content | Person-weeks |
|---|---|---|
| 0 | Oracle, corpus + replay server, benchmarks, ledger | 4 |
| 1 | Shaded stack, transporter, LRM, failure caching, settings adapter, facade rewiring | 7 |
| 2 | Model builder, leniency, effective-pom mapper, instance threading, ledger flips | 12 |
| 3 | Collector, graph mapper, errors/listeners, mediation parity | 11 |
| 4 | Retirement, tuning, docs, release notes | 5 |
| **Total** | | **39 pw** |

Sequencing: strictly 0 → 1 → 2 → 3 → 4. Phases 2 and 3 have an internal seam (descriptor
reader) that lets a second engineer start Phase-3 mapper work ~4 weeks into Phase 2;
two engineers compress the calendar to roughly 5 months.

Every phase ends releasable: the tree API never changes, the caches never change interface,
and the test suite (plus ledger) is green at each boundary.

---

## 11. What cannot be perfectly preserved (and the closest achievable)

Candid list; each is a ledger entry with the chosen resolution:

1. **`ResolutionEventListener.clear()` never fires.** Aether mediates without restarts, so
   the restart-discard semantics vanish. Closest: `dependency` events remain exactly-once
   per resolved node (the invariant hosts actually rely on). Hosts must confirm nothing
   consumes `clear()` for other purposes (open question #4).
2. **Event ordering and cardinality differ.** Same events, same semantic points, different
   interleaving (parallel descriptor prefetch; property/DM events emitted at mapping time).
   Contract redefined as "per-node/per-key semantics", not global ordering.
3. **`ResolvedPom.properties` exact contents.** Raw-lineage merge keeps values raw, but
   *which* profile/parent contributes now follows Maven exactly — maps can differ where the
   old code was wrong (activeByDefault approximation, env-var activation). That is the point;
   still, byte-level map diffs are visible to recipes that enumerate properties.
4. **Resolution-time vs read-time interpolation skew.** `getValue()` keeps its live
   `System.getProperty` fallback (API), but resolution no longer lets system properties beat
   pom properties. A recipe reading `getValue("${x}")` after a resolve may see a different
   value than resolution used if the host JVM sets `-Dx`. Closest: document; hosts wanting
   old behavior pass the property explicitly as a user property.
5. **Type filter widening.** Maven resolves `test-jar`/`war`-typed deps that rewrite drops
   today; scope lists gain entries. No preservation attempted (divergence elimination is the
   goal); Phase-0 corpus quantifies blast radius; release notes flag it. A temporary
   mapper-side compat filter exists behind a ctx flag during Phase 3 rollout only.
6. **Range mediation on conflicting ranges.** Rewrite picks highest-of-nearest and never
   intersects; Maven intersects and can throw `UnsolvableVersionConflictException`. Adopted:
   Maven's behavior, surfaced as `MavenParsingException` (same type as today's unsatisfiable
   single range). Poms that only "resolved" due to the non-intersecting bug will now warn.
7. **Repository order in edge cases.** Request-repo ordering is preserved (we assemble it),
   but *descriptor-discovered* repos are aggregated recessively by Maven
   (`DefaultModelResolver`) rather than by rewrite's merge; observable when a transitive pom
   declares a repo shadowing an id. Closest: acceptable — Maven's behavior is the target.
8. **The `dependencyManagementPropagatesToDependencies` fix and friends change output.**
   Known-wrong-today behaviors (a4 #23, plugin merge bugs, pluginRepositories dropping)
   flip to correct; anything downstream that accidentally depended on the wrong output moves.
9. **`Version` class remains rewrite's copy** for API compatibility; mediation uses aether's
   `GenericVersionScheme` (same algorithm — rewrite's class *is* the aether port, a1 §3.4).
   Pathological disagreement is theoretically possible; the corpus watches for it.
10. **Two failure-memory layers.** Rewrite's process-lifetime tri-state negatives sit above
    aether's persistent TTL'd markers; a host clearing one but not the other sees different
    retry timing than today. Documented, with `MavenPomCache` remaining authoritative for
    "never persist negatives" (RocksDB precedent).

---

## 12. Open questions

1. **Maven line vs JDK floor (the big one).** Supplier-mvn3 locks us to 3.9.16 semantics on
   Java 8; upstream 3.9.x is maintenance-only, and Maven 4 ships the 3.x API as deprecated
   compat. When Maven 4 adoption crosses parity-relevance, do we (a) raise rewrite-maven's
   floor to 17, (b) dual-publish a multi-release/variant artifact, or (c) accept 3.9
   semantics indefinitely? The `MavenEngine` facade is built for the swap; the *policy*
   decision (whose semantics are "Maven's"?) is Jon's.
2. **Behavior-change budget.** The ledger will contain order-of-dozens ALIGN_TO_MAVEN flips
   (test-jar inclusion, range intersection, profile-activation fixes, pluginRepositories…).
   One "resolution 2.0" release with comprehensive notes, or per-change ctx compat flags
   with a deprecation window? Recommendation: single release + shadow-mode data to size
   each change; flags only for the type-filter widening.
3. **Raw-XML source of truth.** This design uses LRM files (+ printed XML for project poms).
   Alternative: extend `MavenPomCache` with raw-bytes storage (default methods) so pure
   in-memory hosts avoid disk. Decide in Phase 1 based on server-side host requirements.
4. **`clear()` and event-stream consumers.** Need confirmation from Moderne CLI/plugins that
   no consumer depends on restart semantics or global event ordering before Phase 3 ships.
5. **`getResolvedDependencyPom` cache key.** Keeping the GAV-only key preserves today's
   cross-build contamination caveat; fixing it (key += activation record) is correct but
   changes cache hit rates and is a `Pom.modelVersion` bump. Phase 2 decision.
6. **Glob exclusions.** Preserved via a custom selector today; do we deprecate the superset
   (Maven-only `*` semantics) on a horizon, since "Maven owns semantics" argues against
   keeping it forever?

---

## Appendix A — What each founding reason resolves to

| 2020 reason for the custom algorithm | 2026 answer |
|---|---|
| (1) Maven hard to invoke programmatically (aether/wagon/OSGi/plexus) | Solved upstream: supplier modules, plain-`new` wiring, no container, ~20 Java-8 jars (a7 §7.1). |
| (2) Maven doesn't cache inaccessibility | Half-right then, half-right now for *CLI defaults* — but an embedder sets `ResolutionErrorPolicy(CACHE_ALL)` and gets persistent, TTL'd, auth-keyed failure caching + session dedupe + RRF. Better than the custom layer, owned upstream (a7 §4). |
| (3) No interprocess locking; wanted pluggable caches | Locking solved by default in resolver 2.x (file-lock named locks). Caches stay pluggable at rewrite's own seams (`MavenPomCache` as L1s above the descriptor reader and model builder; `MavenArtifactCache` above the LRM), which this design deliberately preserves rather than mapping onto resolver-internal stores (a5 §8, a7 §7.3). |
