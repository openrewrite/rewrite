> **SUPERSEDED (2026-07-07)** by `../DESIGN.md`, which merges this design with the incremental
> strangler, both critiques, four spike results, and Jon's decisions (parity absolutism — the
> leniency layer here does NOT ship; per-scope collects; no compat flags). Retained for the record.

# Design: Full Delegation — Replacing rewrite-maven's Custom Resolution with Real Maven

Angle: replace the guts of resolution wholesale. Maven ModelBuilder builds effective POMs; Maven Resolver
(Aether) collects and mediates the dependency graph. rewrite code shrinks to (1) adapters from Maven API
objects into the frozen `org.openrewrite.maven.tree.*` types and (2) SPI implementations bridging Maven's
extension points to rewrite's seams (caches, HttpSender, settings, reactor POMs, event listeners).
`ResolvedPom.Resolver`, `doResolveDependencies`, `VersionRequirement`, and the semantic half of
`MavenPomDownloader` are retired; the classes remain as API facades.

Inputs: study reports a1–a7 (cited as `a1 §2.3` etc.).

---

## 1. Stack choice: Maven Resolver 2.0.x + Maven 3.9 provider ("supplier-mvn3"), shaded

**Chosen stack:** `org.apache.maven.resolver:maven-resolver-supplier-mvn3` (Resolver 2.0.x with
Maven 3.9.16 `maven-resolver-provider` + `maven-model-builder`), relocated into a new shaded module.

Justification, in order of force:

1. **Java 8.** rewrite's bytecode target is Java 8 (a7 §0). The entire Maven 4 stack — supplier-mvn4,
   `maven-impl`, `ApiRunner` — requires Java 17. Only the mvn3 supplier stack runs on 8. This is
   dispositive today.
2. **Semantics parity with the ecosystem.** The parity oracle (5,200-line `MavenParserTest` and friends)
   encodes Maven **3** behavior: `ClassicDependencyManager` (depMgmt of transitive POMs does not apply
   transitively), first-import-wins BOM conflicts, no import exclusions. The world still builds with
   Maven 3.9; adopting Maven 4's `TransitiveDependencyManager` semantics now would *introduce* divergence
   from what users' `mvn` produces.
3. **Maturity.** Maven 4 was still at rc as of the study; its new API is `@Experimental` and
   `maven-impl` internals are not API (a6 §7). The 3.9 model-builder line is frozen, bugfix-only —
   exactly what you want under a strict-compat facade.
4. **The upstream reasons for the custom algorithm are gone in this stack** (a7 §7): no
   wagon/plexus-container/sisu/guice/OSGi at runtime (~20 plain jars, all Java 8); resolver 2.x defaults
   to interprocess `file-lock` named locks; `ResolutionErrorPolicy` + `DefaultUpdateCheckManager` give
   policy-controlled negative caching at artifact/metadata/repo+auth granularity.

**Forward path to Maven 4 is designed in, not bolted on.** Every Maven type is consumed behind two
rewrite-owned interfaces (`EffectivePomBuilder`, `DependencyCollector` — §3). When rewrite's JDK floor
rises, the mvn4 supplier is a second implementation pair; its `importedFrom` provenance and
`RequestCacheFactory` then *simplify* two adapters (§6.1, §5.2) rather than reshaping the design.
The hybrid option (3.9 model-builder + hand-rolled graph walk, or Maven 4 model + 3.9 mediation) is
rejected: it re-creates a seam through the middle of Maven's semantics, which is exactly where the last
eight years of divergence bugs (a4's 94-entry catalog) came from.

Version pinning: resolver 2.0.x line + Maven artifacts 3.9.16, renovate-managed; shaded so host Maven
version can never leak in (§4).

---

## 2. What Maven owns vs. what rewrite owns after this change

| Semantic decision | Owner after migration | Mechanism |
|---|---|---|
| Effective POM: inheritance, interpolation, profile activation (jdk/os/file/property), BOM import, depMgmt injection order, plugin merge | **Maven** | `DefaultModelBuilder` (3.9), our factory subclass |
| Parent lookup incl. relativePath, version ranges in `<parent>`, workspace-first | **Maven** + our `WorkspaceModelResolver` for reactor | `readParentLocally`/`readParentExternally` |
| Version conflict mediation (nearest-wins, ties, ranges, LATEST/RELEASE) | **Maven** | BF collector + `ConflictResolver` + `DefaultVersion(Range)Resolver` |
| Scope derivation and scope mediation | **Maven** | `JavaScopeDeriver`/`JavaScopeSelector` |
| Snapshot → dated version | **Maven** | `DefaultVersionResolver` (metadata), pinned-snapshot pre-pass kept |
| Repository ordering, mirror matching, credentials, https-upgrade probing, dead-repo memory | **rewrite** (pre-session pipeline, unchanged behavior) | `RepositoryPipeline` extracted from MavenPomDownloader |
| Bytes on the wire, retries, auth fallback, timeouts | **rewrite** | `HttpSenderTransporter` |
| Caching (POM bytes, metadata, negative entries, artifacts) | **rewrite** | `MavenPomCache`/`MavenArtifactCache` bridges |
| Lenience for pathological inputs (cycles, invalid POMs) | **rewrite** | `LenientModelResolver` + descriptor policy + error mapping (§6.7, §10) |
| Projection into `org.openrewrite.maven.tree.*` | **rewrite** | adapters (§5) |

The rule of thumb for readability: **anything that decides which version/scope/property wins is Maven's;
anything that decides where bytes come from, where they are cached, and how results are reported is
rewrite's.** No rewrite code re-implements a merge or a mediation.

---

## 3. Architecture overview

New Gradle modules:

- **`rewrite-maven-engine`** — the delegation engine + the shaded Maven stack (relocated packages).
  Depends on rewrite-core only. Publishes one artifact with maven/aether classes relocated to
  `org.openrewrite.maven.engine.shaded.*`.
- **`rewrite-maven`** — unchanged coordinates; gains a dependency on `rewrite-maven-engine`; loses
  ~2,500 lines of resolution internals.

Package layout inside `rewrite-maven-engine` (all new code, ~15 top-level classes — the seam count is
deliberately small):

```
org.openrewrite.maven.engine
    MavenEngine                  // facade: owns RepositorySystem, session template, shared caches
    EngineSession                // per-parse-batch: ctx knobs, settings, repositories, listeners
    EffectivePomBuilder          // interface: (Pom | xml source, repos) -> EffectiveModelResult
    DependencyCollector          // interface: (root effective model, scope config) -> mediated graph
    Maven3EffectivePomBuilder    // impl over DefaultModelBuilder
    Maven3DependencyCollector    // impl over RepositorySystem.collectDependencies (verbose)

org.openrewrite.maven.engine.adapt
    PomToModelConverter          // rewrite Pom -> raw org.apache.maven.model.Model (synthetic/mutated poms)
    ResolvedPomAdapter           // ModelBuildingResult -> ResolvedPom (properties/DM/repos/plugins projection)
    DependencyGraphAdapter       // verbose DependencyNode graph -> ResolvedDependency graph + 4 scope lists
    ExclusionAttributor          // post-pass: effectiveExclusions, shallowest-ancestor attribution
    ErrorMapper                  // aether exceptions -> MavenDownloadingException(s), repo responses, URIs

org.openrewrite.maven.engine.spi          // bridges INTO Maven extension points
    HttpSenderTransporterFactory / HttpSenderTransporter
    ReactorWorkspace             // WorkspaceReader + WorkspaceModelResolver over project poms
    CacheBridge                  // MavenPomCache <-> descriptor/model caches; pom-bytes region
    EngineLocalRepositoryManager // LocalRepositoryManager over cache dirs (paths for the connector)
    SettingsBridge               // MavenSettings -> external profiles, auth, mirrors, session props
    RecordingBomImporter         // DependencyManagementImporter decorator: importedFrom bookkeeping
    GlobExclusionSelector        // ExclusionDependencySelector with rewrite's glob superset
    GradleMetadataEnricher       // ModelProcessor decorator: .module platform -> import-scope DM
    ListenerBridge               // RepositoryListener/TransferListener -> ResolutionEventListener
    LenientModelResolver         // cycle/invalid-input tolerance shims (parent cycles -> stub parents)
```

`rewrite-maven` keeps every public type; these become facades:
- `Pom.resolve(...)` → `MavenEngine.resolveEffectivePom(...)`
- `ResolvedPom.resolveDependencies(...)` → `MavenEngine.collect(...)`; the `Resolver` inner class,
  `doResolveDependencies`, and `VersionRequirement`'s engine role are deleted.
- `MavenPomDownloader` keeps all four public constructors and `download`/`downloadMetadata` signatures
  (rewrite-gradle and ~10 recipes call them); `download` delegates to the engine's descriptor pipeline;
  `downloadMetadata`, the metadata merge, and HTML-index derivation move to
  `org.openrewrite.maven.internal.MetadataDownloader` (same behavior, extracted for readability) and the
  facade delegates.
- `RawPom` survives **only** as the verbatim XML→`Pom` reader. It makes no semantic decisions; `Pom` is
  serialized wire format (model version, Smile caches) and must be produced exactly as today. All of
  RawPom's resolution-era leniences (trimming, namespace-unaware, profile reversal) stay because they
  define the *requested* model, which is API. Its `snapshotVersion` hooks and Gradle-metadata mutation
  points are removed.

### Component diagram

```
                         MavenParser / UpdateMavenModel / rewrite-gradle headless
                                             │
                    RawPom.parse ──> Pom (verbatim, unchanged wire format)
                                             │
     ┌───────────────────────────────────────▼─────────────────────────────────────────┐
     │                              MavenEngine (rewrite-maven-engine)                  │
     │                                                                                  │
     │  EngineSession ◄── MavenExecutionContextView knobs, MavenSettings, activeProfiles│
     │       │                                                                          │
     │       │  RepositoryPipeline (in rewrite-maven, extracted from MavenPomDownloader)│
     │       │   order: local → settings/ctx → pom → central-last; mirrors; credentials;│
     │       │   https probe; unreachable host:port set; normalized-repo cache          │
     │       ▼                                                                          │
     │  EffectivePomBuilder ════ shaded DefaultModelBuilder ═══╗                        │
     │       │        ▲                                        ║ ModelResolver /        │
     │       │        │ WorkspaceModelResolver                 ║ ArtifactResolver        │
     │       │   ReactorWorkspace (project poms by path/GAV)   ║                        │
     │       ▼                                                 ▼                        │
     │  ResolvedPomAdapter                        shaded RepositorySystem               │
     │       │                                     ├─ DefaultArtifactDescriptorReader   │
     │       ▼                                     ├─ Default{Version,VersionRange}Resolver
     │  DependencyCollector ═══ BF collector ══════├─ ConflictResolver (verbose)        │
     │       │                                     ├─ EngineLocalRepositoryManager ──┐  │
     │       ▼                                     └─ HttpSenderTransporter ──────┐  │  │
     │  DependencyGraphAdapter + ExclusionAttributor + ErrorMapper                │  │  │
     └────────────┬─────────────────────────────────────────────────────────────┼──┼──┘
                  │                                                              │  │
                  ▼                                                              ▼  ▼
   MavenResolutionResult / ResolvedPom / ResolvedDependency          HttpSender   MavenPomCache
   (org.openrewrite.maven.tree.* — FROZEN, unchanged)                (ctx)        MavenArtifactCache
                                                                                  (ctx, pluggable)
```

---

## 4. Classpath strategy per host

The shaded module answers all four environments with one artifact:

| Host | Hazard | Resolution |
|---|---|---|
| Inside Maven (`rewrite-maven-plugin`) | Maven's core realm force-exports `org.eclipse.aether.*` (1.9.x in 3.9 hosts) and `org.apache.maven.model(.building)` to every plugin, parent-first (a7 §2) — unrelocated resolver 2.x classes would be shadowed by 1.9.27 → `NoSuchMethodError` skew | All maven/aether/plexus packages relocated to `org.openrewrite.maven.engine.shaded.*` at `rewrite-maven-engine` build time (Gradle shadow). `META-INF/sisu` indexes and OSGi manifests stripped so a Sisu-scanned realm cannot double-register components |
| Inside Gradle plugins | Flat buildscript classpaths; other plugins may carry their own resolver | Same relocation; nothing new — rewrite-gradle-plugin already practices relocation |
| Standalone / server-side (Moderne, RPC) | None beyond jar size | +~6–8 MB of shaded classes; acceptable; one `RepositorySystem` per process (thread-safe, shared) |
| Tests / `rewrite-test` | none | engine used directly |

Explicitly rejected: "use the host Maven's injected `RepositorySystem` when running inside Maven." It
would reintroduce per-host-version behavior differences — the precise disease this project cures — and
has no analog in Gradle/standalone.

The shaded stack (a7 §1a): 8 resolver jars + 6 maven jars + plexus-utils + plexus-interpolation +
slf4j-api. We do **not** ship transport-apache (httpclient et al.) — the only transporter is
`HttpSenderTransporter` (plus resolver's `FileTransporterFactory` for `file://` repos), which keeps the
footprint down and guarantees all network I/O flows through the host-injected `HttpSender`.

---

## 5. Entity mapping

### 5.1 Raw side (unchanged)

`RawPom → Pom` is untouched: trimming, groupId/version parent-fallback, reversed profiles, license
classification, `obsoletePomVersion` refusal, `ManagedDependency.Defined/Imported` split at literal
`scope == "import"`. `Pom.getModelVersion()` stays 3 until the pom-bytes cache region ships (then 4;
persistent caches rebuild — cheap by design, a3 §3).

### 5.2 `ModelBuildingResult` → `ResolvedPom` (ResolvedPomAdapter)

The build request: `DefaultModelBuildingRequest` with `validationLevel = MINIMAL`,
`processPlugins = false`, `twoPhaseBuilding = false`, `locationTracking = true`,
system properties = ctx user properties merged over engine-scoped system snapshot,
user properties = MavenParser builder properties + marker `userProperties` (MNG-7563 pattern, a6 §2.5),
external profiles = settings profiles via `SettingsBridge`, `ModelResolver = LenientModelResolver`,
`WorkspaceModelResolver = ReactorWorkspace`, `ModelCache` from `CacheBridge`.

Field-by-field (tricky ones):

| `ResolvedPom` field | Source | Notes |
|---|---|---|
| `requested` | the input `Pom`, same instance | unchanged; the in-place gav placeholder fix-up (a1 §2.1 step 3) is reproduced by the facade for compat |
| `activeProfiles` | passthrough of caller list | unchanged |
| `properties` | **raw-lineage projection**: for each model id in `ModelBuildingResult.getModelIds()` (child→parent), merge `getRawModel(id).getProperties()` with `getActivePomProfiles(id)` profile properties first, first-wins | Deliberately NOT the effective model's (interpolated) properties: `ResolvedPom.properties` is documented/serialized as the *raw* merged map and `getValue()` interpolates lazily (a3 §1.2). Winner selection (which profiles are active, lineage order) comes from Maven; the merge itself is a mechanical first-wins fold — no semantics re-implemented. `getValue`/`getProperty` code is kept verbatim (it is read-side API: `project.*` pseudo-props, null→`""`, circular sentinel, System.getProperty fallback) |
| `dependencyManagement` | effective model `getDependencyManagement().getDependencies()` → `ResolvedManagedDependency` list | scope parsed via `Scope.fromName`; type defaulted "jar"; lazily-sorted getter + comparator unchanged. **Back-links** (`requested`, `requestedBom`, `bomGav`): see below |
| `requestedDependencies` | GA-keyed child-wins merge over raw lineage models' `getDependencies()` + active-profile dependencies — **threading the original `Dependency` instances** from `Pom.dependencies` / `Profile.dependencies` | This preserves identity contract #1 (a3 §2): `ResolvedDependency.requested ==` an element of `Pom.dependencies`. Mechanical: membership and precedence dictated by Maven-computed lineage/profiles; the fold is `putIfAbsent` on GA exactly as today |
| `repositories` | rewrite-order merge (initialRepositories → child-first raw lineage profile+body repos), interpolated via `getValue` | Repository *ordering* is a founding performance behavior pinned by `repositoryOrder`/`centralIdOverridesDefaultRepository` (a4 #49) and rewrite's order (central appended last) intentionally differs from Maven's (super-POM central first). We keep rewrite's pipeline; the session's super-POM is replaced by a central-less `SuperPomProvider` so Maven never injects central on its own, and `addCentralRepository`/`addLocalRepository` tri-states keep their per-constructor defaults |
| `pluginRepositories` | as today (i.e., effectively dropped by resolution; survives only on no-change) | preserving the wart; fixing it is a separate announced change |
| `plugins` / `pluginManagement` | effective model `build.plugins` / `build.pluginManagement.plugins`, with a **no-op `PluginManagementInjector`** substituted in our `DefaultModelBuilderFactory` subclass so management is *not* folded into `plugins` | rewrite's contract is "merged declared plugins, management not applied; consumers join" (a1 §2.7). Deltas vs today, all divergence *fixes*: profile `<build><plugins>` now honored (today silently dropped, a1 §1.3), child phase beats parent phase, goal order deterministic. `configuration` Xpp3Dom → Jackson `JsonNode` via a small converter (same shape `MavenXmlMapper` produces) |
| `subprojects` | raw model modules + Maven-4-style subprojects concat | unchanged |
| `initialRepositories` | as today (frozen after first resolve) | unchanged |

**BOM back-links without `importedFrom` (the 3.9 gap):** `RecordingBomImporter` decorates
`DefaultDependencyManagementImporter` (swapped in via the factory subclass). For each import event it
records `(importing model id, source DependencyManagement, imported BOM gav)`. The adapter joins this
record against the declaring `Pom.dependencyManagement` to set `requestedBom` to the **same
`ManagedDependency.Imported` instance** and `bomGav` to the resolved BOM GAV — identity contract #2
holds. `Defined` entries map to their declaring instance via `InputLocation.getSource().getModelId()`
(location tracking) + positional match within that model. When the stack moves to Maven 4,
`RecordingBomImporter` is deleted and `importedFrom` read directly (a6 §4).

**`ResolvedPom.resolve()` no-change identity:** the facade keeps today's field-by-field comparison and
returns `this` when effectively unchanged — recipes rely on reference equality (a3 §2.5).

### 5.3 Verbose `DependencyNode` graph → `ResolvedDependency` (DependencyGraphAdapter)

Collect configuration: session from `SessionBuilderSupplier` defaults (FatArtifactTraverser,
`ClassicDependencyManager`, `AndDependencySelector(Scope.legacy(test,provided), Optional.fromDirect(),
GlobExclusionSelector)`), `ConflictResolver` verbose **STANDARD** +
`DependencyManagerUtils.CONFIG_PROP_VERBOSE=true`, `setIgnoreArtifactDescriptorRepositories(false)`
(Maven semantics own; see §10 item 6).

| `ResolvedDependency` field | Source | Notes |
|---|---|---|
| `gav` | winner node artifact: `version = artifact.getBaseVersion()`, `datedSnapshotVersion = artifact.getVersion()` when it differs (aether puts the timestamped version in `getVersion()`, base `-SNAPSHOT` in `getBaseVersion()`); `repository` URI from the descriptor-source map maintained by `CacheBridge` (it knows which `MavenRepository` served each POM — same key the pom cache uses) | timestamped-input folding (`1.0-20230101.123456-1` → base + dated) preserved in the facade's gav normalization, as today |
| `requested` | instance-threaded: depth-0 nodes matched (GA, last-declaration-wins) against `ResolvedPom.requestedDependencies`; transitive nodes matched (GACT) against the containing `Pom.dependencies` fetched from `MavenPomCache` | keeps `requested` as the **uninterpolated original** (a1 §5.2) and preserves `getResolvedDependency(Dependency)`'s `==` lookup |
| `dependencies` | children of the node in the STANDARD graph, with loser nodes redirected to their `NODE_DATA_WINNER`'s single shared `ResolvedDependency` | one `ResolvedDependency` per winner; flat scope lists and nested children are the same instances (identity contract #3); parent lists built by the same `unsafeSetDependencies` mutation |
| `licenses` | from the dep's cached `Pom.licenses` | |
| `type` / `classifier` | artifact extension (getter defaults "jar") / classifier | the type filter (`jar|ejb|pom|zip|bom|tgz` else skip silently) is applied by the adapter when projecting nodes into lists — preserved as an explicit, commented projection rule rather than buried logic |
| `optional` | `Boolean.valueOf` of the requested dependency's optional (as today, incl. the property-valued-optional-→false quirk at depth 0) | |
| `depth` | node depth in the verbose tree | |
| `effectiveExclusions` | `ExclusionAttributor` post-pass: walk winners; for each node compute accumulated (requested + managed) exclusions from ancestors, glob-evaluate against the children the node's `Pom` declares, attribute matches to the shallowest declaring ancestor | Same algorithm as today's `includedByMap` walk, but as reporting over data we already hold, not as selection logic. Selection itself is Maven's (`GlobExclusionSelector` extends exact/`*` matching to rewrite's glob superset so `com.foo*` keeps working, a4 #74) |

**The four scope lists.** One verbose collect over the full graph (Maven-identical single mediation),
then the adapter derives `{Compile, Runtime, Test, Provided}` membership from each winner's derived
scope + root scope using the existing `Scope.transitiveOf` table — the same classpath filtering Maven
performs. Depth-0 roots are seeded per scope exactly as today (declared scope in classpath of the target
scope; duplicate direct GAs last-declaration-wins before seeding). Consequence: mediation now happens
**once across all scopes, like `mvn`**, instead of four independent mediations. In graphs where a
test-scoped direct dep and a compile-transitive conflict, the resolved version in the Compile list can
change — to what real Maven produces. This is divergence elimination working as intended; the shadow
corpus (§9) quantifies the blast radius, and a per-scope-collect fallback flag exists during migration
(§8 Phase 3) in case the radius is unacceptable for a release.

Ordering within lists: BFS depth then first-encounter order from the collector — matches today's
"nearest by depth, ties by declaration order" and the deterministic-order contract (a4 #84).

### 5.4 Error mapping (ErrorMapper)

| Maven/aether signal | rewrite result |
|---|---|
| `ArtifactDescriptorException` on a depth-0 jar/ejb node | `MavenDownloadingException` accumulated into `MavenDownloadingExceptions` with root-GA attribution; parse continues, `ParseExceptionResult` + `e.warn(doc)` markup as today |
| descriptor missing, jar exists (HEAD probe via HttpSender) | synthesize minimal jar-packaging `Pom` (as today, `rawPomFromGav`); node kept; `downloadSuccess` fired |
| descriptor missing, jar also missing | `MavenDownloadingException` (entry a4 #64 matrix preserved) |
| descriptor invalid (transitive) | `artifactDescriptorInvalid` listener → warn-and-continue; dependency set matches `mvn`'s "POM is invalid, transitive dependencies will not be available" (a4 #82) — this is native `SimpleArtifactDescriptorPolicy(IGNORE_MISSING\|IGNORE_INVALID)` behavior, no custom code |
| `VersionRangeResolutionException` / unsolvable range | `MavenParsingException` naming the range set (message-compatible) |
| unresolved `${...}` in final GAV | pre-flight check in the facade (kept from today) → `MavenDownloadingException` "Could not resolve property" |
| depth-0 versionless dependency | pre-flight in facade → `MavenDownloadingException("No version provided…")` |
| transporter failures | per-repo response strings recorded in a request-scoped trace; `MavenDownloadingException.setRepositoryResponses` populated with the same phrasing incl. "Did not attempt to download because of a previous failure…" for cached negatives |

Attempted-URI lists for `downloadError` events come from the same trace the transporter populates.

---

## 6. Seam-by-seam adapter designs

### 6.1 POM cache (`MavenPomCache`) — the one interface extension

The interface and all four tri-state sub-caches survive verbatim. One addition (default methods, so
existing host implementations keep compiling):

```java
// MavenPomCache — new region; default null/no-op = "not supported, fall back to LRM files"
byte @Nullable [] getPomBytes(ResolvedGroupArtifactVersion gav);   // null=unknown, EMPTY=known-absent
void putPomBytes(ResolvedGroupArtifactVersion gav, byte @Nullable [] bytes);
```

Why: the ModelBuilder must read real XML (the parsed `Pom` drops sections — relocations,
distributionManagement — that Maven semantics need), so the durable cache unit becomes POM **bytes**,
keyed identically to `getPom` today. Flow:

- **Read:** `CacheBridge` sits inside the artifact-resolution path for `*.pom` artifacts (a decorator on
  our `EngineLocalRepositoryManager.find`): bytes-cache hit → materialize to the engine's content-addressed
  scratch dir (LRM must yield `Path`s, a5 §3.1) → report available. `Optional.empty()` (known-absent for
  this repo) → the not-found is replayed without network, and `repositoryAccessFailedPreviously` fires.
- **Write:** on transporter success, `putPomBytes` + parse `RawPom→Pom` + `putPom` (the parsed-Pom region
  stays maintained — `MavenParser`, facades, and instance-threading (§5.3) read it).
- **Negative caching rules unchanged:** `HttpSenderTransporter.classify()` maps 4xx-except-408/425/429 to
  `ERROR_NOT_FOUND` (→ cached, `putPom(gav, null)`), everything else to `ERROR_OTHER` (→ never persisted).
  `ResolutionErrorPolicy = CACHE_NOT_FOUND` only; `aether.updateCheckManager.sessionState=enabled` gives
  once-per-run in-memory dedup. RocksDB's "never persist negatives" stance is preserved because negatives
  ride the in-memory L1 exactly as today.
- `get/putResolvedDependencyPom` (the memoized partial-effective-model cache): retained as the engine's
  warm-path cache for adapter outputs (§7). `getMavenMetadata`/`getNormalizedRepository` regions unchanged
  (used by `MetadataDownloader` and `RepositoryPipeline`).

`Pom.getModelVersion()` bumps to 4 when this ships (persistent caches rebuild once).

### 6.2 Artifact cache (`MavenArtifactCache`)

**Unchanged.** Binary artifact download in rewrite is not part of POM resolution — collect is POM-only
(a7 §5, "Misconception No1"), and `MavenArtifactDownloader` + `MavenArtifactCache` already operate off
`ResolvedDependency` downstream. They keep working as-is, including the non-dated-SNAPSHOT always-refetch
rule and `ReadOnlyLocalMavenArtifactCache.mavenLocal()` composition. `EngineLocalRepositoryManager`'s
artifact side only ever sees POMs in practice; its jar-side `find` composes `MavenArtifactCache` for
completeness.

### 6.3 HTTP transport (`HttpSenderTransporter`, ~400 lines)

- One `TransporterFactory` registered via `createTransporterFactories()` override; priority above stock;
  stock apache transport not shipped. `FileTransporterFactory` retained for `file://` repos (with
  rewrite's `maven-metadata-local.xml`, user-neutral-repo substitution, and missing/empty-jar-means-
  unusable checks kept in `CacheBridge`'s local-repo handling).
- `get()` → `HttpSender.get` with: basic auth from the repo (populated by `RepositoryPipeline`), per-server
  `httpHeaders` and connect/read timeouts from `MavenSettings.Server` (passed through session config
  properties keyed by server id), Failsafe retry policy verbatim (timeouts only, max 5, 500ms+10% jitter),
  and the authenticated→anonymous 4xx fallback verbatim.
- `peek()` → HEAD (used for jar-exists probes and checksum-free existence checks).
- **`classify()` is the load-bearing method** (a5 key judgment #3): `HttpSenderResponseException.
  isClientSideException()` → `ERROR_NOT_FOUND`; all else `ERROR_OTHER`. This single mapping makes Maven's
  entire negative-caching machinery enforce rewrite's exact rules.
- **Dead-repo memory:** the run-scoped `ctx.getUnreachableEndpoints()` host:port set is consulted before
  any connection and populated on connection-level failure — preserved verbatim, now inside the
  transporter, so *every* Maven-initiated request (descriptors, metadata, checksums) benefits.
- Checksums: `aether.checksums.algorithms` set to empty/lenient initially — rewrite never validated
  checksums; enabling them later is an announced improvement, not a silent change.
- Metadata derivation (Nexus HTML index scraping): on 404 for a `maven-metadata.xml` GET, the transporter
  consults `deriveMetadataIfMissing` and synthesizes metadata XML from the index listing — behavior moved,
  not changed, incl. the self-disabling flag.

### 6.4 Settings (`SettingsBridge`)

`MavenSettings` remains the only settings model (parsing, `~/.m2` + `M2_HOME/conf` merge, env
interpolation, settings-security decryption — all rewrite code, untouched; `withServers(null)`
sanitization on the marker untouched). The bridge translates per `EngineSession`:

- settings/ctx **profiles** → `org.apache.maven.model.Profile` external profiles on the model request;
  explicit active ids → `setActiveProfileIds`. Profile *activation semantics* thereby become Maven's:
  property activation reads `-D`/user properties (not env vars), `<os>`/`<file>` activation starts
  working, JDK activation reads the request's system properties (which we can pin for determinism). See
  §10 items 2–3 for the announced behavior changes.
- **mirrors** → `DefaultMirrorSelector` (covers descriptor-discovered repos; request repos are
  pre-mirrored by `RepositoryPipeline` as today, so both paths agree). `external:http:*` starts working
  (gap today, a4 #55).
- **servers** → auth on `RemoteRepository` instances (pipeline) + `ConservativeAuthenticationSelector`
  for discovered repos; headers/timeouts via session config properties.
- **proxies** → `DefaultProxySelector` with nonProxyHosts. Note this *activates* a parsed-but-dead
  feature (a2 §3); announced improvement. The HttpSender remains free to override at the socket level.
- `localRepository` → `EngineLocalRepositoryManager` base directory;
  `pinnedSnapshotVersions` → a pre-pass in the facade's gav normalization exactly as today.

### 6.5 Workspace / reactor (`ReactorWorkspace`)

Implements both `WorkspaceReader` (aether: consulted before LRM/remote for every artifact, a5 §3.2) and
`WorkspaceModelResolver` (model-builder: `resolveRawModel` wins over relativePath and remote, a6 §1.4):

- backed by the same `projectPoms`/`projectPomsByGav` maps `MavenPomDownloader` builds today, including
  the property-merged-ancestry GAV computation;
- `findVersions`/`resolveRawModel` implement the three-tier match (exact GAV, raw version, property-merged
  version) so `${revision}`-style CI-friendly parents match in-reactor before interpolation (a4 #6, #12);
- `<relativePath>` semantics (default `..`, explicit-empty opt-out, `/pom.xml` appending, GA(V)
  verification) are Maven's own `readParentLocally` — the code rewrite copied its comments from — so this
  logic is **deleted** from rewrite and inherited, with `ModelSource2.getRelatedSource` backed by real
  files for parsed poms;
- `.m2`-relative parents excluded from reactor membership as today (a4 #13).

For **synthetic and mutated poms** (Gradle headless `Pom.builder()` graphs; `UpdateMavenModel`'s
in-place-mutated `Pom`): `PomToModelConverter` produces a raw `Model` from the `Pom`, and
`ReactorWorkspace` serves it as the workspace model. This keeps contract #19 (UpdateMavenModel re-reads
exactly properties/parent/deps/DM/repos) byte-exact: the converter only carries what `Pom` carries.
Feeding the freshly-printed XML instead (which would let plugin/profile edits update the model) is a
deliberate later improvement, not part of parity.

### 6.6 Event listeners (`ListenerBridge`)

All 13 `ResolutionEventListener` events keep firing at semantically equivalent points:

| rewrite event | new emission point |
|---|---|
| `download`, `downloadSuccess`, `downloadError(gav, attemptedUris, containing)` | transporter trace + `artifactResolving/Resolved` bridge; attempted URIs from the per-request trace |
| `downloadMetadata` | `metadataResolving` bridge |
| `repository(repo, containing)` | `RepositoryPipeline` enumeration (unchanged call sites) |
| `repositoryAccessFailed` / `repositoryAccessFailedPreviously` | pipeline probe / cached-negative replay |
| `parent(parentPom, containing)` | `LenientModelResolver` on each parent resolution |
| `bomImport(gav, containing)` | `RecordingBomImporter` |
| `property`, `dependencyManagement` | `ResolvedPomAdapter` during projection (per merged property / DM entry) |
| `dependency(scope, resolved, containing)` | `DependencyGraphAdapter`, exactly once per node per scope list |
| `clear()` | **never fires.** It existed solely for the custom algorithm's restart loop; aether has no restart, so events are exactly-once by construction. Hosts that reconstruct logs get a strictly simpler stream; documented in the migration notes |

`recordResolutionTime` accumulates in the transporter (same wall-clock accounting).

### 6.7 Lenience shims (`LenientModelResolver`) — keeping rewrite's parse-anything guarantee

Real Maven aborts where rewrite must parse (a4 #8, #16, #30, #83). The model builder is kept lenient by:

- **parent cycles**: the resolver tracks the lineage it has served; a request that would revisit an
  ancestor GAV returns a synthesized empty `pom`-packaging stub → build succeeds, chain truncates where
  today's silently-stop truncates, warn marker attached;
- **self-recursive dependency / circular BOM import**: descriptor policy `IGNORE_INVALID` already keeps
  dependency-side builds lenient; for the *root* project pom, `ModelBuildingException` is caught, the
  partial result's effective model used when present, problems mapped to warn markers;
- **circular `${project.version}`**: Maven's interpolator reports a problem and leaves the raw value; the
  read-side `getProperty` sentinel (`error.circular.project.version`) is unchanged, so recipes observing
  through `getValue` see today's behavior; resolution proceeds (see §10 item 5 for the residual delta);
- **obsolete `pomVersion` poms**: refused before the engine is ever invoked (facade check, as today).

---

## 7. Hot path walkthrough (fully warm cache, many-module reactor)

Scenario: 300-module reactor, all external POMs in `MavenPomCache`, second parse in the same process
(or `UpdateMavenModel` re-resolution after a one-line property edit).

**Today:** per module — three ancestry walks (cache-served), lazy interpolation churn, BFS over the graph
with `getResolvedDependencyPom` hits per node, four independent scope resolutions. Single-threaded. No
network. Cost ≈ hash lookups + merge/interpolation allocations × 4 scopes.

**New engine, per module:**

1. `Pom.resolve` → facade → `MavenEngine`. `EffectivePomBuilder` first probes the engine's
   **effective-model memo** (ConcurrentHashMap keyed `(gav, activeProfiles-hash, repo-list-hash)`, the
   successor of `getResolvedDependencyPom`): hit → `ResolvedPom` projected from memo, zero ModelBuilder
   work. The `resolve()` no-change identity check then returns the old instance for `UpdateMavenModel`
   non-model edits — the cheap path stays cheap.
2. Miss → `DefaultModelBuilder.build`: parent chain served by `ReactorWorkspace` (map lookups) or
   `ModelCache` RAW hits; BOM imports from `ModelCache` IMPORT hits. Known 3.9 wart: `ModelCacheTag`
   deep-clones on every cache hit. Mitigation is architectural: the **memo in step 1 sits above
   ModelBuilder**, so clone costs are paid once per distinct (gav, context), not per reference — the same
   discipline `getResolvedDependencyPom` imposes today.
3. `collectDependencies`: BF collector, one pass. Every descriptor request hits the engine's
   `DataPool` descriptor pool (one shared `RepositoryCache` across all sessions in the process —
   map get, no model building); the skipper prunes foregone conflict losers before descriptor work;
   `UpdateCheckManager` session-state suppresses every touch-file check after the first. Zero network.
4. `ConflictResolver` transform (classic impl; `path` O(N) impl is a config-key flip if profiling ever
   says so) + `DependencyGraphAdapter` projection + `ExclusionAttributor` — linear in graph size, one
   pass instead of four (scope lists are derived by filtering, not re-collected).

**Expected relative cost:** warm-path work per module = 1 memo probe + 1 collect + 1 transform + 1
projection, versus today's 3 ancestry walks + 4 BFS resolutions. Fewer passes, but each aether pass
carries more object ceremony. Target, enforced by the Phase-0 JMH suite in `rewrite-benchmarks`
(existing module): **within 1.2× of today's warm-reactor wall clock; anything above is a Phase-4 blocker.**

**Cold path:** expected *faster* than today. Today's BFS downloads POMs serially, level by level. The BF
collector prefetches descriptors on 5 threads and the metadata resolver on 4 (a7 §5); on wide graphs the
cold parse is network-bound and parallelism wins. Shadow runs measure this on the corpus.

Memory: `DataPool` interning + retained `ResolvedPom.deduplicate()` on memo insertion keep the
per-process footprint at parity; the shared `RepositoryCache` is size-bounded via a Caffeine-backed
implementation (same 100k-entry discipline as `InMemoryMavenPomCache`).

---

## 8. Migration phases (each lands independently green against the existing suite)

Effort in person-weeks (pw), one senior engineer full-time equivalent; phases 2 and 3 parallelize across
two people with ~20% overhead.

| Phase | Contents | Exit criteria | Effort |
|---|---|---|---|
| **0 — Oracle & harness** | `ShadowResolutionComparator` (§9); corpus assembly (every pom fixture in the repo's tests + top-500 Maven Central POMs + 3 large real reactors); JMH baselines for warm/cold parse in `rewrite-benchmarks` | comparator runs old-vs-old with zero diffs; baselines recorded | **2 pw** |
| **1 — Engine bootstrap** | `rewrite-maven-engine` module + shading/relocation build; `MavenEngine`/`EngineSession`; `HttpSenderTransporterFactory`; `RepositoryPipeline` extraction (pure refactor of MavenPomDownloader normalization, suite green); `SettingsBridge`; `ReactorWorkspace`; central-less `SuperPomProvider` | seam unit tests green; `MavenPomDownloaderTest` repository/auth/mirror subsets pass against the pipeline; no production switch | **4 pw** |
| **2 — Effective-POM delegation** | `Maven3EffectivePomBuilder`, `ResolvedPomAdapter`, `PomToModelConverter`, `RecordingBomImporter`, `LenientModelResolver`, no-op plugin injector; engine selected by `MavenExecutionContextView` flag (default old) | full rewrite-maven suite green with flag ON in a second CI job; shadow corpus: `ResolvedPom` diff rate < agreed threshold, every diff triaged as fix/bug | **6 pw** |
| **3 — Graph delegation** | `Maven3DependencyCollector` (verbose collect), `DependencyGraphAdapter`, `GlobExclusionSelector`, `ExclusionAttributor`, `ErrorMapper`, `ListenerBridge`, `GradleMetadataEnricher`; per-scope-collect fallback flag; re-enable `dependencyManagementPropagatesToDependencies` (the @Disabled known-deficit test, a4 #23) | suite green flag-ON incl. rewrite-gradle headless tests; shadow corpus graph diffs triaged; the 15 highest-value acceptance tests (a4) green | **6 pw** |
| **4 — Cache & performance integration** | pom-bytes region + `CacheBridge` + `EngineLocalRepositoryManager`; `Pom` model-version bump; negative-caching parity tests; JMH comparison vs Phase-0 baselines; memory profiling | warm ≤1.2× baseline, cold ≤1.0×; RocksDB/Composite host caches verified via Moderne CLI smoke run | **3 pw** |
| **5 — Flip & retire** | default flag flips; old algorithm kept one minor release behind the flag; then delete `ResolvedPom.Resolver`, `doResolveDependencies`, `VersionRequirement` engine role, MavenPomDownloader internals → facade; ADR documenting the delegation architecture; downstream test runs (rewrite-spring, rewrite-java-dependencies, plugins) | downstream suites green; deletion PR merged; migration notes published for the announced behavior changes (§10) | **3 pw** |

**Total: ~24 person-weeks** (≈ 15 pw calendar with two people through phases 2–4). Every phase leaves
`main` releasable: phases 1–4 are additive behind a default-off flag; the suite is the parity oracle at
each step.

---

## 9. Shadow-comparison strategy

- `ShadowResolutionComparator` runs both engines on the same inputs (same ctx, same caches isolated per
  engine) and diffs `MavenResolutionResult` via canonical projection: per-scope ordered GAV+depth lists,
  `ResolvedPom` properties/DM/repos/plugins as sorted multimaps, requested-instance threading verified by
  identity probes (`getResolvedDependency(each requested) != null`), exception classes+messages, and the
  `ResolutionEventListener` event multiset (minus `clear`).
- Wired three ways: (a) a JUnit extension that, when `-Prewrite.maven.shadow=true`, runs every
  `rewriteRun`-based maven test double-engine and fails on undiffed divergence — turning the whole
  existing suite into a differential oracle; (b) the standalone corpus runner (top-500 central POMs +
  real reactors) producing a triage report (`MATCH / FIX (matches real mvn, old was wrong) / BUG`); (c)
  ad-hoc: a `-Dorg.openrewrite.maven.engine=old|new|shadow` system property for host-side reproduction.
- Ground truth for FIX-vs-BUG triage: `mvn dependency:tree -Dverbose` output captured for the corpus
  (the repo already documents this technique at MavenParserTest:3925, a4 #82).
- Diffs classified FIX get their pinned tests updated in the same PR with the a4 catalog entry cited;
  diffs classified BUG block the phase.

---

## 10. What CANNOT be perfectly preserved (and the closest achievable behavior)

Candid list; each item ships in the migration notes.

1. **`ResolvedPom.resolveDependencies(Scope, Map<GroupArtifact, VersionRequirement>, …)`** — the
   public overload takes the old engine's requirement-chain type as a seed. Aether has no equivalent
   seedable mediation state. Closest: the overload remains, `@Deprecated`, ignores the seed map, and
   delegates to the plain overload. `VersionRequirement` stays as a source-compatible husk. In-repo and
   known-downstream usage of the seeded form is nil, but it is technically a behavior break.
2. **System-property override of POM properties during resolution** (a1 divergence 16). Today any `-D`
   on the *host JVM* silently overrides same-named POM properties mid-resolution. Maven's precedence is
   user-props > model > system-props, with `-D` flowing in as user properties. Closest: `MavenParser
   .property(...)` and ctx user properties map to Maven user properties (still win, as today's tests
   pin); raw host-JVM system properties no longer override POM properties inside model building. The
   read-side `getProperty` keeps its `System.getProperty` fallback (it is serialized-API behavior), so
   an incoherence window exists between resolution-time and read-time interpolation for host-JVM-set
   keys. This is the deterministic-parsing fix Jon has wanted; it is still a change.
3. **Profile property activation via environment variables** (a1 divergence 7, a4 #38). Maven evaluates
   `-D`/user properties; the env-var behavior is unreproducible without re-forking the activator.
   Closest: Maven semantics, plus a transition shim (ctx flag that injects `System.getenv()` into user
   properties) for hosts that depended on env activation, removed after one minor release. Pinned tests
   (`parseNotInProfileActivation` etc.) are updated as FIX.
4. **Restart-loop listener semantics** — `clear()` never fires (§6.6). Events are exactly-once; hosts
   that implemented clear-and-rebuild get a stream with no clears, which their logic already handles as
   the common case.
5. **Circular `${project.version}` sentinel inside resolved fields.** Read-side `getValue` still returns
   the sentinel. But values Maven interpolated during model building will contain the raw `${...}` (with
   a warn marker) rather than the sentinel string in effective positions. Observable only in the
   pathological-cycle fixtures; those tests update to assert the raw-value + warning shape.
6. **Per-scope-independent version mediation** (§5.3). Single mediation like real `mvn` is the target;
   in adversarial graphs the Test/Provided list versions shift to Maven's answers. The per-scope-collect
   flag is the escape hatch during migration; it is not intended to survive Phase 5.
7. **Transitive POMs' `<repositories>`** now used for their children (Maven behavior), not just their
   parents/BOMs (a1 divergence 13) — strictly more resolvable POMs; repos discovered this way pass
   through mirror/auth selectors and the pipeline's normalization cache, so the dead-repo guarantees
   hold.
8. **Plugin-merge quirks** — parent-phase-beats-child and HashSet goal ordering (a1 divergence 15) and
   silently-dropped profile plugins become Maven-correct. Tests pinning the buggy output update as FIX.
9. **Exact exception message text.** Aggregation shape (`MavenDownloadingExceptions`, per-GA dedup,
   repositoryResponses) is preserved; some message strings for range/parent failures will read as
   Maven's phrasing. Hosts parsing message text (they shouldn't) may notice.
10. **`_remote.repositories`/touch-file artifacts in the engine scratch dir.** The engine's LRM writes
    resolver sidecar files under rewrite's cache root (never `~/.m2`, which stays read-only via the
    existing composition). New on-disk artifacts, same directory ownership as today's artifact cache.

Everything else on a3's MUST checklist — wire format, `@JsonIdentityInfo` topology, null-tolerant
getters, `Scope` ordinals, instance threading, `unsafeSet*` marker graph, `MavenParser` builder/stitching,
headless `Pom.builder()` engine reuse, tri-state caches, dead-repo skip, snapshot pinning, POM-less-jar
synthesis, Gradle-metadata BOM injection, spring-milestone filter (as a `RemoteRepositoryFilterSource`) —
is preserved by construction in the designs above.

---

## 11. Open questions

1. **Scope mediation strategy (§5.3):** single collect (Maven-exact, changes rare per-scope results) vs
   four collects (shape-exact, 4× collector work). Needs shadow-corpus data; decision gate at end of
   Phase 3. Recommendation baked into this design: single collect.
2. **Java floor trajectory:** if rewrite's Java 8 bytecode floor rises to 17 within the support horizon,
   the mvn4 supplier becomes the second `EffectivePomBuilder`/`DependencyCollector` implementation and
   `RecordingBomImporter`/location-tracking joins get deleted in favor of `importedFrom`. Does Moderne's
   deployment matrix allow scheduling that, and do we gate any Phase-5 cleanup on it?
3. **`MavenPomCache` bytes region rollout:** default-method extension keeps hosts compiling, but the
   Moderne CLI's `CompositeMavenPomCache(InMemory, Rocksdb)` should implement it natively for the warm
   interprocess path — coordinate the CLI release with the `Pom.modelVersion` bump so caches rebuild
   exactly once.
4. **Checksum validation:** rewrite has never validated checksums; the engine can (`aether.checksums.*`).
   Off for parity — turn on later as a security improvement?
5. **UpdateMavenModel full-fidelity re-read:** once stable, feed the mutated `Xml.Document`'s printed
   bytes instead of `PomToModelConverter` so `<build><plugins>`/`<profiles>` edits update the model —
   an announced improvement deliberately excluded from parity scope.
