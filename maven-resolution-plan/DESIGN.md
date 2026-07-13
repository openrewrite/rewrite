# Canonical design: rewrite-maven resolution on real Maven APIs

2026-07-07. **This document supersedes** `design/design-full-delegation.md` and
`design/design-incremental-strangler.md` (retained for the record). It merges the two, applies
every finding from both adversarial critiques (`design/critique-*.md`), the four spike results
(`SPIKE-RESULTS.md`), and all decisions Jon made on 2026-07-07. Where this document and the
older designs disagree, this document wins. Study citations (`a1 §2.3` etc.) refer to `study/`.

## 0. Locked decisions

| Decision | Resolution |
|---|---|
| Stack | Resolver 2.0.x via `maven-resolver-supplier-mvn3` + Maven 3.9.16 `maven-resolver-provider`/`maven-model-builder`, shaded. Committed — no Maven 4 preparation, ledgering, or scheduled swap. Spike-proven on 2.0.20/3.9.16 at `--release 8`. |
| Parity policy | **Any divergence from Maven's resolution behavior is a bug.** No lenient mode. Target = Maven's exact tolerance profile: fail where Maven fails, tolerate where Maven tolerates. Every divergence found gets a pinning unit test in rewrite-maven's suite. |
| Release shape | Single flip-over, no user-facing compat flags. Internal dual-engine flag is dev/CI-only. Phase-5 safety = old engine in-tree but unreachable for one release (revert-by-release). |
| Scope mediation | **Single verbose collect (N1) + per-scope projection** — reversed from per-scope collects by the large-reactor baseline (`benchmarks/baseline-2026-07-08.md`): on BOM-heavy camel, four collects are *slower than the old engine* at 50–150 modules (0.55–0.75×) and scale ~3.5× steeper (~8.2 vs ~2.3 ms/module marginal); N1 wins every tier. The 15-module result that had favored N4 didn't hold at scale. Per-scope `ResolvedDependency` trees are still projected shape-exact from the one graph (scope-filtered children, instances shared only within a scope); the cross-scope version-shift behavior change returns to the ledger, blast-radius-sized on the Phase-3 shadow corpus. |
| HTTP | `HttpSender` injection is a hard host requirement (moderne-saas RSocket tunnel). `HttpSenderTransporter` is the sole network transport; stock HTTP transports excluded from the shade; sender resolved per session; session carries no proxy selector (settings `<proxies>` stay unconsumed — the sender owns transport end-to-end). |
| Caches | `MavenPomCache` gains a pom-bytes region (default methods); `MavenArtifactCache` unchanged; engine LRM = private per-run scratch dir; no `~/.m2` writes ever; no negative outlives the run except through `MavenPomCache` policy. |
| Session config | The engine's session template **mirrors Maven 3.9's `DefaultRepositorySystemSessionFactory`** — that is the parity reference for every knob (RRF sources off, `ClassicDependencyManager`, descriptor policy, checksum policy…). Deltas exist only at owned seams (transport, caches, listeners). |
| Model input | **XML-first**: ModelBuilder always reads real XML — printed document bytes for project poms (including `UpdateMavenModel` re-resolution), the pom-bytes cache region for remote poms. `PomToModelConverter` exists only for synthetic `Pom.builder()` graphs (rewrite-gradle's marker-approximation path), which have no XML. |
| Gradle metadata | `.module` platform injection is a parity bug on the Maven parsing path — removed there. Kept as an explicit opt-in engine option set only by rewrite-gradle's `GradleDependencyConfiguration.doResolve` approximation path (its ground truth is Gradle, which honors `.module`). |

Open (do not block Phases 0–2): HTML-index metadata derivation keep/kill (Jon);
pom-bytes + `Pom.modelVersion` bump coordination with the Moderne CLI.

## 1. Ownership split

Anything that decides **which version/scope/property wins** is Maven's. Anything that decides
**where bytes come from, where they are cached, and how results are reported** is rewrite's.
No rewrite code re-implements a merge or a mediation; the folds that remain in adapters
(property lineage merge, requested-dependency GA merge) are mechanical projections whose
precedence inputs come from Maven, and each is tested against Maven's own effective output.

| Concern | Owner | Mechanism |
|---|---|---|
| Effective POM (inheritance, interpolation, profile activation incl. `<os>`/`<file>`, BOM import, DM injection, plugin merge) | Maven | `DefaultModelBuilder` (3.9.16), our factory subclass |
| Parent lookup (relativePath rules, ranges in `<parent>`), workspace-first | Maven + our `ReactorWorkspace` | `readParentLocally`/`readParentExternally` |
| Collection, mediation, ranges, LATEST/RELEASE, snapshots→dated | Maven | BF collector, verbose `ConflictResolver`, `Default{Version,VersionRange}Resolver` |
| Exclusion matching | Maven (stock exact/`*` semantics — the glob superset is a parity bug, removed) | stock `ExclusionDependencySelector` |
| Cycle/self-parent/import-cycle handling | Maven (FATAL) | surfaced via `ParityErrorMapper` |
| Bytes, retries, auth fallback, timeouts, dead-endpoint memory | rewrite | `HttpSenderTransporter` |
| Caching (pom bytes, parsed poms, descriptors, metadata, negatives, artifacts) | rewrite | `MavenPomCache`/`MavenArtifactCache` bridges |
| Projection into frozen `org.openrewrite.maven.tree.*` | rewrite | mappers (§3) |

## 2. Modules, packages, classpath

- **`rewrite-maven-engine`** (new module): adapters + the shaded stack. Depends on rewrite-core
  only. Relocates `org.eclipse.aether`, `org.apache.maven.model(.building)`,
  `org.apache.maven.repository.internal`, `org.apache.maven.artifact`,
  `org.codehaus.plexus.{utils,interpolation}` → `org.openrewrite.maven.engine.shaded.*`;
  strips `META-INF/sisu` and OSGi manifests; aggregates NOTICE/licenses. Excluded from the
  shade: `maven-resolver-transport-apache` + httpclient/httpcore/commons-codec (the sole
  network transport is ours), and the RRF prefix machinery is disabled at session level.
- **`rewrite-maven`**: unchanged coordinates and public types; gains the engine dependency;
  loses ~4.5k lines of algorithm. No shaded type ever appears in `org.openrewrite.maven.tree.*`
  signatures or serialized LSTs.

Why shading is non-negotiable: Maven's core realm force-exports `org.eclipse.aether.*` and
`maven-model(-builder)` parent-first to every plugin (a7 §2; both 3.9 and 4 `extension.xml`),
so un-relocated embedding inside rewrite-maven-plugin resolves against the host's 1.9.27.
Gradle's flat buildscript classpath has the same disease with different mechanics. Relocation
gives one pinned, tested engine version in all four hosts; "use the host's `RepositorySystem`"
is rejected because per-host-version behavior is the exact divergence class being eliminated.

### Class inventory (`org.openrewrite.maven.engine`, ~14 classes)

| Class | Role |
|---|---|
| `MavenEngine` | Bootstrap facade: supplier subclass, session template (mirrors Maven 3.9's session factory), shared `RepositoryCache`, engine options (gradle-metadata opt-in). The only class that knows Maven is underneath. Cached on the `ExecutionContext`. |
| `HttpSenderTransporter(Factory)` | Sole network transport. Sender from session config properties per `newInstance`. Owns `classify()` (deterministic-4xx-except-408/425/429 → NOT_FOUND), Failsafe retry (timeouts only, 5×, 500ms+jitter), authenticated→anonymous 4xx fallback, per-server headers/timeouts, run-scoped `host:port` unreachable set, `recordResolutionTime`. Spike-proven (claims 2–3). |
| `ReactorWorkspace` | `WorkspaceReader` + `MavenWorkspaceReader` + `WorkspaceModelResolver` over the project-pom maps; three-tier GAV match (exact / raw / property-merged, `${revision}` raw-first); serves printed-XML `ModelSource`s; carries the **reactor epoch** (§5.5). relativePath logic is deleted from rewrite and inherited from Maven. |
| `CacheBridge` | `MavenPomCache` above the engine: pom-bytes region read/write (parse-through to the parsed-`Pom` region on bytes hits), descriptor-result caching under the `getResolvedDependencyPom` key (caching Maven's own output — never synthesizing descriptors from `ResolvedPom`), metadata region, tri-state negative semantics, scratch-dir materialization for the LRM. |
| `EngineLocalRepositoryManager` | LRM over the private per-run scratch dir. Mandatory per session; holds nothing durable. |
| `SettingsBridge` | `MavenSettings`/ctx knobs → external profiles + active ids, auth selectors, mirror application at request-repo-list assembly (the session `MirrorSelector` only sees descriptor-discovered repos — both paths must agree), per-server config properties, user properties (parser `.property()` + ctx). No proxy selector. |
| `BomGavAttributor` | Stamps `requestedBom`/`bomGav` by imported-BOM effective-DM membership in import order (spike P2: `InputLocation`/importer alone misattribute BOMs that inherit their DM), using `InputLocation` to separate parent-inherited from BOM-imported entries. |
| `PinnedVersionResolver` | Decorates `DefaultVersionResolver`: `ctx.getPinnedSnapshotVersions()` wins, including for transitive snapshots; routes metadata reads through `MavenPomCache`. |
| `EffectivePomMapper` | `ModelBuildingResult` + raw lineage → `ResolvedPom` (§4.1), threading original `Dependency`/`ManagedDependency` instances by `InputLocation` (line/column — not GACT: `${project.groupId}` defaults and `ModelNormalizer` de-duping break coordinate joins). Overlays parser/ctx-injected properties into the raw merged map (they must stay visible via `getProperties()`/`getValue()`). Emits `parent`/`property`/`dependencyManagement`/`bomImport` events. |
| `DependencyGraphMapper` | Verbose per-scope `DependencyNode` graphs → per-scope `List<ResolvedDependency>` (§4.2). Instances shared within a scope, never across scopes. Emits `dependency` events exactly once per node per scope. |
| `ExclusionAttributor` | Reporting post-pass computing `effectiveExclusions` (shallowest-declaring-ancestor attribution) from data already in the verbose graph. Matching semantics are Maven's (exact/`*`). |
| `ParityErrorMapper` | Maven exceptions → `MavenDownloadingException(s)`/`MavenParsingException` with per-repository responses, attempted URIs, root-GA attribution and per-GA dedup. Fail where Maven fails (cycles, unsolvable ranges — surfaced as per-file `ParseExceptionResult`, the parse run never aborts); tolerate where Maven tolerates (missing/invalid transitive descriptors per `SimpleArtifactDescriptorPolicy`, matching `mvn dependency:tree` output). No leniency beyond Maven's own. |
| `ListenerBridge` | `RepositoryListener`/`TransferListener` + mapper emission points → the 13 `ResolutionEventListener` events. `clear()` never fires (no restart loop; events are exactly-once by construction). |
| `PomToModelConverter` | `Pom` → raw `Model`, used only for synthetic `Pom.builder()` graphs. |

Facades kept in rewrite-maven, signatures frozen: `Pom.resolve(...)`,
`ResolvedPom.resolveDependencies(...)`/`resolveDirectDependencies(...)` (the seeded
`Map<GroupArtifact, VersionRequirement>` overload stays as a deprecated no-op-seed delegate;
`VersionRequirement` remains a source-compatible husk), `MavenPomDownloader` with all four
public constructors and `download`/`downloadMetadata` (rewrite-gradle and recipes call them
cross-module). `RawPom → Pom` is untouched — it is the verbatim requested model and wire
format; it makes no semantic decisions (semantics now always come from real XML).

## 3. Data flow

```
             MavenParser / UpdateMavenModel / rewrite-gradle (synthetic poms)
                                   │
              RawPom.parse ──► Pom (verbatim requested model, frozen wire format)
                   │                          │ pom XML bytes (printed doc / bytes region)
                   ▼                          ▼
   ┌────────────────────────────── MavenEngine ─────────────────────────────────┐
   │ SettingsBridge → session ← MavenExecutionContextView / MavenSettings       │
   │                                                                            │
   │ DefaultModelBuilder (locationTracking=true, XML-first)                     │
   │    ◄ ReactorWorkspace (project poms, epoch-keyed)                          │
   │    ◄ ModelResolver → RepositorySystem descriptor path                      │
   │         ◄ CacheBridge (MavenPomCache: bytes/pom/descriptor/metadata)       │
   │         ◄ EngineLocalRepositoryManager (per-run scratch)                   │
   │         ◄ HttpSenderTransporter ──► HttpSender (host-injected)             │
   │                                                                            │
   │ 4 × collectDependencies (verbose, per scope) → ConflictResolver            │
   │                                                                            │
   │ EffectivePomMapper + BomGavAttributor → ResolvedPom                        │
   │ DependencyGraphMapper + ExclusionAttributor → per-scope ResolvedDependency │
   │ ParityErrorMapper / ListenerBridge → exceptions + events                   │
   └────────────────────────────────────────────────────────────────────────────┘
                                   │
              MavenResolutionResult (org.openrewrite.maven.tree.* — FROZEN)
```

## 4. Entity mapping (the tricky fields)

### 4.1 `ResolvedPom` (EffectivePomMapper)

Build request: `validationLevel=MINIMAL`, `processPlugins=false`, `locationTracking=true`,
external profiles + active ids from `SettingsBridge`, user properties = parser `.property()` +
ctx user properties (Maven `-D` semantics), `WorkspaceModelResolver=ReactorWorkspace`,
`ModelCache` from `CacheBridge`.

| Field | Source | Notes |
|---|---|---|
| `requested` | the input `Pom`, same instance | unchanged |
| `properties` | raw-lineage merge: `getModelIds()` child→parent, `getRawModel(id)` properties with `getActivePomProfiles(id)` profile properties first, first-wins; **plus parser/ctx-injected properties overlaid** | deliberately raw, not the interpolated effective map — `getValue()` interpolates lazily and is serialized API. Maven decides *which* models/profiles contribute; the fold is mechanical and tested against `help:effective-pom` output |
| `dependencyManagement` | effective model DM → `ResolvedManagedDependency` | `requested` = declaring `Defined` instance joined by `InputLocation` line/column; `requestedBom`/`bomGav` from `BomGavAttributor`. Lazy-sort + comparator untouched |
| `requestedDependencies` | GA-keyed child-wins merge over raw lineage + active-profile dependencies, **threading original instances** | preserves `ResolvedDependency.requested == Pom.dependencies` element identity |
| `repositories` | effective model repositories, Maven's ordering and super-POM handling | repo *ordering* aligns to Maven (parity policy); `addLocal/addCentralRepository` ctx tri-states remain as embedder inputs to the repo universe |
| `plugins`/`pluginManagement` | effective model `build.plugins`/`build.pluginManagement` with a no-op `PluginManagementInjector` (management not folded into plugins — rewrite's output contract) | Maven's merge fixes the parent-phase-beats-child and HashSet-goal-order bugs; profile `<build><plugins>` are honored because model building reads real XML |
| `subprojects`, `initialRepositories`, `activeProfiles` | unchanged | |
| `resolve()` identity | field-by-field comparison, `this` when unchanged | recipes rely on reference equality |

### 4.2 `ResolvedDependency` (DependencyGraphMapper)

Four verbose collects per pom (`ConflictResolver` `Verbosity.STANDARD` — the enum, per spike —
+ `DependencyManagerUtils.CONFIG_PROP_VERBOSE`), `ClassicDependencyManager`, stock exclusion
selector, per-scope root seeding exactly as Maven seeds a classpath request (which matches
today's `dScope == scope || transitiveOf(scope) == scope` filter and root-scope=Compile).

| Field | Source | Notes |
|---|---|---|
| `gav` | winner artifact: `version = getBaseVersion()`, `datedSnapshotVersion = getVersion()` when different; `repository` from `CacheBridge`'s per-GAV source map | timestamped-input folding preserved in the facade |
| `requested` | depth-0: the seed instance (seed↔root-node map). Transitive: declaring pom's `Dependency` joined by `InputLocation` | uninterpolated original, as today |
| `dependencies` | children of the winning node; losers redirected to their `NODE_DATA_WINNER`'s instance **within the scope** | instances shared within a scope list and its nested graph; never across scopes (per-scope children differ) |
| `depth` / ordering | BFS depth; first-encounter order | matches today's deterministic-order contract |
| `effectiveExclusions` | `ExclusionAttributor` post-pass | reporting only |
| type/classifier/optional/licenses | Maven-effective values | **no type filter** (test-jar/war/etc. resolve — parity fix); `optional` is model-interpolated (parity fix) |

Removed relative to the old designs (all parity bugs): the jar/ejb/pom/zip/bom/tgz type filter,
the glob-exclusion superset (`GlobExclusionSelector` is not built), the spring-milestone
hardcoded filter, the transitive-repository asymmetry (today a transitive pom's own
`<repositories>` are used for its ancestry — parents/BOMs, `ResolvedPom.java:472,936` — but its
children are fetched with the root's list, `:1104`; Maven aggregates descriptor-declared
repositories into child requests — align, pinning-test fixture: a transitive dep whose child
lives in a repo only that dep's pom declares), cross-repo metadata *merging* inside version
machinery (Maven's resolvers own it; the
public `downloadMetadata` facade keeps its current recipe-facing behavior — it is a version
listing utility, not resolution), env-var profile activation, system-property override of POM
properties at resolution time, the `activeByDefault` over-broad suppression.

## 5. Seams

### 5.1 POM cache

`MavenPomCache` interface + tri-state semantics survive; one addition (default methods):

```java
byte @Nullable [] getPomBytes(ResolvedGroupArtifactVersion gav);  // null=unknown, EMPTY=known-absent
void putPomBytes(ResolvedGroupArtifactVersion gav, byte @Nullable [] bytes);
```

Bytes are the durable unit because ModelBuilder needs real XML and parsed `Pom` is lossy.
Read path: bytes hit → materialize into the scratch LRM (resolver requires real `Path`s) →
parse-through to the parsed-`Pom` region. Write path: transporter success → `putPomBytes` +
`RawPom.parse` + `putPom`. Negative entries: deterministic-4xx only, via `classify()`;
5xx/IOException never cached; RocksDB keeps dropping negatives. The
`getResolvedDependencyPom` region caches **the engine's own descriptor/model output** keyed as
today — never a descriptor synthesized from a `ResolvedPom` (that would be a second semantic
producer, the warm-vs-cold divergence class). `Pom.modelVersion` bumps to 4 with this change
(persistent caches rebuild once; coordinate with the Moderne CLI).

### 5.2 Artifact cache — unchanged

Collect is POM-only; jars move only via `MavenArtifactDownloader` above the unchanged
`MavenArtifactCache` (including non-dated-SNAPSHOT refetch and read-only `~/.m2` composition).

### 5.3 Transport

As locked in §0. Additional spike-driven bootstrap facts: override
`createRemoteRepositoryFilterSources()` (bare resolver 2.x enables prefix/groupId RRF sources
that fetch `.meta/prefixes.txt` out of band — Maven 3.9's resolver does not, so disabling is
parity); `FileTransporterFactory` retained for `file://` repos. The https-preference probe and
`MAVEN_LOCAL_USER_NEUTRAL` substitution remain at this layer (transport reachability and cache
keying, not semantics — ledger-noted). Checksum validation off initially (rewrite never
validated; enabling later is an announced improvement). Phase-1 guarantee test: recording
sender + MockWebServer, every byte through the sender.

### 5.4 Settings

`MavenSettings` remains the only settings model (parsing, decryption, `effectiveSettings(mrr)`
merge, `withServers(null)` sanitization — untouched). Profile activation semantics become
Maven's by construction (external profiles + user properties). Mirrors applied at repo-list
assembly and mirrored into the session selector for descriptor-discovered repos —
`external:http:*` starts working. Proxies: **not** wired to a session proxy selector (§0).

### 5.5 Reactor and re-resolution

`ReactorWorkspace` serves project poms by printed-XML bytes. Because `UpdateMavenModel`
mutates poms without changing GAVs, every GAV-keyed engine cache (ModelCache RAW/IMPORT,
DataPool, descriptor region) is keyed with a **reactor epoch** that increments on marker
replacement; workspace GAVs never serve stale models. Phase-2 gate includes a
mutate-parent-then-re-resolve test. XML-first re-resolution means the re-read set widens to
everything in the document (plugins, profiles) — parity-consistent, ledger-noted.

### 5.6 Events

All 13 `ResolutionEventListener` events keep firing at semantically equivalent points (mapping
table as in the superseded designs, minus leniency emission sites). `clear()` never fires;
event ordering changes (parallel prefetch, mapper-time emission) — contract restated as
per-node/per-key exactly-once, not global ordering.

## 6. Performance

Comparative benchmark harness exists (`spike/benchmark/`, independently reproduced,
apache/maven@3.9.16 reactor): raw new pipeline vs today = cold 5.6s→2.8s, warm 109.5→26.8 ms
(per-scope N4), re-resolution loop 10.8→2.7 ms/iter. The adapter layer is unmeasured and gets
built inside that headroom; the gate is the **ratio vs the old engine on identical inputs**,
re-run each phase, with the harness graduating into `rewrite-benchmarks`. Known cost center:
3.9 `ModelCacheTag` deep-clones per cache hit (measured 59/warm 15-module build; linear in
reactor size × parent depth) — mitigation, if large-reactor benchmarks demand it, is our own
`ModelCache` storing immutable snapshots (a caching-layer change, not semantics). Cold-path
request count is higher (checksum sidecars/HEADs; tunable policy); wall clock is the axis.

## 7. Verification

1. **Double-engine JUnit extension**: the existing suite (~5,200 tests) runs against both
   engines; shape-aware diff (ordered per-scope lists, nested-graph serialization, identity
   probes, event multisets, exception classes). A test's expectation changes only with a
   reviewed ledger entry.
2. **Corpus**: test-suite fixtures + top-N Central poms + large real reactors + a MockWebServer
   synthetic corpus for what Central can't exercise (mirrors, auth fallback,
   snapshots/classifiers, dead repos, `file://`). Ground truth: `mvn dependency:tree -Dverbose`
   and `help:effective-pom` captured per corpus entry. Hermetic record/replay for CI.
3. **Runtime shadow mode** (dev/CI + host opt-in, not a shipped toggle): both engines, diffs to
   a data table, for corpus-scale sizing of each ledger entry before the flip.
4. **The ledger**: every diff classified — `ALIGN_TO_MAVEN` (default for all semantics; ships
   with a **pinning unit test** asserting the Maven-identical behavior) or `KEEP_REWRITE`
   (allowed only for output shape/API and seams; each entry justifies itself against §0's
   parity policy). Existing tolerance tests (cycle suite etc.) are inverted, not deleted —
   assertions come from the parent-cycle spike's captured failure shapes.
5. **Benchmark gates** per §6.

## 8. Phases

Every phase lands independently green; the internal engine flag is dev/CI-only.

| # | Content | Exit criteria | Effort |
|---|---|---|---|
| 0 | Double-engine harness + shape-aware diff; corpora + record/replay; benchmark harness graduation into `rewrite-benchmarks` (large-reactor variant for the ModelCache question); ledger process | old-vs-old zero diffs; baselines recorded | 3–4 pw |
| 1 | `rewrite-maven-engine` module: shading build, `MavenEngine` session template (mirrors Maven 3.9 session factory), `HttpSenderTransporter` + no-bypass guarantee test, `SettingsBridge`, `ReactorWorkspace`, pom-bytes region + `CacheBridge` skeleton, scratch LRM | seam unit tests green; repository/auth/mirror subsets of `MavenPomDownloaderTest` pass against the new plumbing | 4–6 pw |
| 2 | Effective POM on `DefaultModelBuilder`: `EffectivePomMapper`, `BomGavAttributor`, `PomToModelConverter`, InputLocation threading, property overlay, reactor-epoch keying, `ParityErrorMapper` (model side), parity fixes land wholesale (profile activation, plugin merge, cycles→errors + inverted tests) | full suite green dual-engine; effective-pom corpus diff rate 0 unexplained; mutate-parent-re-resolve test green | 6–9 pw |
| 3 | Per-scope verbose collects: `DependencyGraphMapper`, `ExclusionAttributor`, `PinnedVersionResolver` + metadata cache routing, error/listener completion, gradle-metadata engine option wired to rewrite-gradle's path; re-enable `dependencyManagementPropagatesToDependencies` (#376) | suite + rewrite-gradle headless tests green dual-engine; graph corpus diffs 0 unexplained; a4's 15 acceptance tests green | 6–9 pw |
| 4 | Cache/perf hardening: descriptor-region integration, custom `ModelCache` if the large-reactor benchmark demands, memory profiling, Moderne CLI smoke (RocksDB/Composite) | benchmark ratios hold; host caches verified | 2–3 pw |
| 5 | Flip-over (no user-facing flags), one-release revert-by-release window, delete the old algorithm (`ResolvedPom.Resolver`, `doResolveDependencies`, `VersionRequirement` engine, downloader internals → ~300-line facade), ledger → release notes, ADR, downstream suite runs | downstream green; deletion merged | 3 pw |
| | **Total** | | **24–34 pw** |

Ordering rationale: oracle first (parity becomes a measured quantity at zero risk); engine
bootstrap + transport next (lands shading/classpath in all four hosts with minimal semantics,
and creates the bytes substrate); model building before collection (the collector's descriptor
reader *is* model building); flip only after the corpus is clean and the ledger reviewed.

## 9. Ledger seeds (known `ALIGN_TO_MAVEN` flips)

Host `-D` no longer overrides POM properties at resolution (read-side `getValue()` fallback
stays — serialized API; skew documented); profile property activation reads properties not env
vars; `<os>`/`<file>` activation works; `activeByDefault` exact rule; type filter removed
(test-jar/war resolve — biggest observable change, corpus-sized); real range intersection
(+ `UnsolvableVersionConflictException` → `MavenParsingException`); duplicate-range last-wins
gone; glob exclusions → Maven exact/`*`; plugin-merge fixes; `pluginRepositories` inherited;
repository ordering + descriptor repositories per Maven; cycles/self-parents/import cycles →
Maven-identical failures (inverted tests); system scope resolved (`systemPath`); `optional`
interpolated; dependency-cycle recording per aether (`CollectResult.getCycles()`);
`ResolutionEventListener.clear()` never fires; event ordering per §5.6; some exception message
text reads as Maven's; the seeded `resolveDependencies(…, Map<GroupArtifact,
VersionRequirement>, …)` overload becomes a deprecated no-op-seed facade.

`KEEP_REWRITE` (non-semantic only): frozen tree API + per-scope output shape + instance
identity; pluggable caches with tri-state negatives (4xx-only, run-scoped transfer errors);
dead-endpoint set; https-preference probe; `MAVEN_LOCAL_USER_NEUTRAL`; `MavenSettings` as the
settings model; proxies stay the sender's concern; per-file error containment
(`ParseExceptionResult`) as the surface for Maven-identical failures; pom-less-jar stubbing
*if* the pinning test confirms it matches Maven's missing-descriptor tolerance.
