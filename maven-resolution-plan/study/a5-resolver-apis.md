# Maven Resolver (Aether) 2.x — Embedder API & Extension-Point Study for rewrite-maven

Source studied: `/Users/jon/Projects/github/apache/maven-resolver` at commit `16bbda7e6afebb133aed63fe36ade1c850804b07` (2026-07-07, master, version `2.0.21-SNAPSHOT`; maven3Version=3.9.16, maven4Version=4.0.0-rc-5 per root `pom.xml:115-116`).

All paths below are relative to that repo unless noted.

---

## 1. Bootstrap without DI containers: the supplier modules

Resolver 2.x ships two "no-DI" bootstrap modules that hand-wire the entire component graph with plain `new` — no Guice/Sisu/Plexus anywhere (the mvn4 supplier's pom even *excludes* `javax.inject`, `sisu`, `guice`, `asm` from `maven-resolver-provider`: `maven-resolver-supplier-mvn4/pom.xml:95-113`).

### 1.1 `RepositorySystemSupplier` (both modules, ~identical shape)

`maven-resolver-supplier-mvn3/src/main/java/org/eclipse/aether/supplier/RepositorySystemSupplier.java` and the mvn4 twin.

- A memoizing `Supplier<RepositorySystem>`: every component has a `final getXXX()` (memoizes) + a `protected createXXX()` designed to be **overridden by subclassing** (class javadoc, mvn3 lines 155-177). One supplier instance → one `RepositorySystem`; not thread-safe during construction; the built system *is* thread-safe; lifecycle tied to `RepositorySystem.shutdown()` (lines 259-272, `addOnSystemEndedHandler(() -> closed.set(true))` line 265).
- What you get out of the box (mvn3 file, `createXXX` methods):
  - Local repo managers: `SimpleLocalRepositoryManagerFactory` + `EnhancedLocalRepositoryManagerFactory` via `DefaultLocalRepositoryProvider` (`createLocalRepositoryProvider`, lines 535-550).
  - Update machinery: `DefaultUpdateCheckManager(trackingFileManager, updatePolicyAnalyzer, pathProcessor)` (lines 326-328), `DefaultUpdatePolicyAnalyzer` (298-300).
  - Locking: named-lock factories `{noop, rwlock, semaphore, file-lock}` (lines 354-361), name mappers `{static, gav, gaecv, discriminating, file-gav, file-gaecv, file-hgav, file-hgaecv}` (373-384), `DefaultSyncContextFactory(NamedLockFactoryAdapterFactoryImpl(...))` (426-443).
  - Transports: `FileTransporterFactory` + `ApacheTransporterFactory` only (`createTransporterFactories`, lines 720-727). **JDK and Jetty transports are NOT wired by default** — override `createTransporterFactories()` to add/replace.
  - Connector: `BasicRepositoryConnectorFactory` (753-761) plus pipeline connectors for remote-repository filtering and offline enforcement (`FilteringPipelineRepositoryConnectorFactory`, `OfflinePipelineRepositoryConnectorFactory`, lines 789-798).
  - Collectors: both `DfDependencyCollector` and `BfDependencyCollector` registered under `DefaultDependencyCollector` (lines 869-903).
  - Checksums (sha512/256/1, md5), trusted/provided checksum sources, repository layout (`Maven2RepositoryLayoutFactory`), remote-repo filters (groupId/prefixes), `DefaultOfflineController`, `GenericVersionScheme` (980-982).
  - **The Maven bits ("// Maven provided" section, mvn3 lines 1014-1131)**: `DefaultArtifactDescriptorReader`, `DefaultVersionResolver`, `DefaultVersionRangeResolver`, `Snapshot/Versions/PluginsMetadataGeneratorFactory`, `DefaultModelBuilderFactory().newInstance()` (maven-model-builder), `DefaultModelCacheFactory` — all imported from `org.apache.maven.repository.internal.*`, i.e. from the **`org.apache.maven:maven-resolver-provider` artifact of Maven core**, not from this repo (see §7).
  - Final assembly: `DefaultRepositorySystem(...)` (lines 1171-1187).
- What you must provide: nothing mandatory beyond calling `get()`; everything is customized by overriding `createXXX()`. `createArtifactGeneratorFactories`/`createArtifactDecoratorFactories`/`createArtifactTransformers`/`createRepositoryListeners`/`createValidatorFactories` return empty maps/lists by design ("this is extension point", lines 994-1012, 617-619).

### 1.2 mvn3 vs mvn4 `RepositorySystemSupplier` differences

- Dependencies: mvn3 pulls `maven-resolver-provider` + `maven-model-builder` **3.9.16** (`maven-resolver-supplier-mvn3/pom.xml:88,111`); mvn4 pulls the **4.0.0-rc-5** versions (`maven-resolver-supplier-mvn4/pom.xml:94,117`) with DI jars excluded.
- mvn4 additionally wires **relocation sources** (`MavenArtifactRelocationSource`): `UserPropertiesArtifactRelocationSource` + `DistributionManagementArtifactRelocationSource`, passed as an extra `LinkedHashMap` arg to `DefaultArtifactDescriptorReader` (mvn4 supplier lines 1053-1094). mvn3's `DefaultArtifactDescriptorReader` ctor takes 7 args, no relocation sources (mvn3 lines 1059-1069).
- mvn4's `DefaultVersionRangeResolver` ctor additionally takes the `VersionScheme` (mvn4 lines 1122-1126) — Maven 4 made the range resolver scheme-pluggable.
- mvn3 module **vendors deprecated `org.eclipse.aether.spi.locator.{ServiceLocator,Service}`** shims so Maven 3.9's provider classes (compiled against resolver 1.x) still link: "This class here is merely to provide backward compatibility to Maven3. Pretend is not here." (`maven-resolver-supplier-mvn3/src/main/java/org/eclipse/aether/spi/locator/ServiceLocator.java:24-28`).

### 1.3 `SessionBuilderSupplier` (mvn3 vs mvn4)

`maven-resolver-supplier-mvn3/.../SessionBuilderSupplier.java`, mvn4 twin. Produces a `RepositorySystemSession.SessionBuilder` preconfigured "Maven-like"; each `get()` returns a fresh builder (`get()` mvn3 lines 203-208). `configureSessionBuilder` (mvn3 93-109) sets: system properties + `env.*`, optional `ScopeManager`, `DependencyTraverser`, `DependencyManager`, `DependencySelector`, `DependencyGraphTransformer`, `ArtifactTypeRegistry`, `ArtifactDescriptorPolicy`.

Maven-equivalent defaults (mvn3):
- Traverser: `FatArtifactTraverser` (line 115-117) — stops descending below fat (war/ear/…) artifacts.
- **Manager: `getDependencyManager()` → `getDependencyManager(false)` → `ClassicDependencyManager`** (lines 119-131): Maven 3 semantics — dependencyManagement of transitive POMs does NOT apply transitively.
- Selector: `AndDependencySelector(ScopeDependencySelector.legacy(null, [test, provided]), OptionalDependencySelector.fromDirect(), ExclusionDependencySelector())` (lines 133-149) — drop test/provided and optional beyond direct deps, honor exclusions.
- Graph transformer: `ChainedDependencyGraphTransformer(ConflictResolver(ConfigurableVersionSelector, JavaScopeSelector, SimpleOptionalitySelector, JavaScopeDeriver), JavaDependencyContextRefiner)` (lines 151-169) — this **is** Maven's nearest-wins mediation.
- `ArtifactTypeRegistry` with the classic packaging stereotypes (pom, jar, war, test-jar, …; lines 171-187).
- `ArtifactDescriptorPolicy`: `SimpleArtifactDescriptorPolicy(true, true)` = ignore missing AND invalid descriptors (line 189-191) — same leniency Maven itself uses for transitive POMs.
- **You must still set a local repository** ("At least LRM must be set on builder", javadoc lines 193-202) — easiest via `SessionBuilder.withLocalRepositoryBaseDirectories(Path...)` (API `RepositorySystemSession.java:446-476`).

**mvn4 difference:** `getDependencyManager()` → `getDependencyManager(true)` → **`TransitiveDependencyManager`** (mvn4 SessionBuilderSupplier lines 117-129) — Maven 4 applies dependency management transitively. Also mvn4's `FatArtifactTraverser` comes from `org.apache.maven.repository.internal.artifact` (line 26) and scopes come from `org.apache.maven.api.DependencyScope` (line 25); mvn4's class no longer `implements Supplier` (plain `get()`, line 199). Everything else (selector, transformer, stereotypes, descriptor policy) is identical.

Working example of the whole bootstrap: `maven-resolver-demos/maven-resolver-demo-snippets/src/main/java/org/apache/maven/resolver/examples/util/Booter.java:79-117` (`new SessionBuilderSupplier(system).get().withLocalRepositoryBaseDirectories(path).setRepositoryListener(...).setTransferListener(...)`).

---

## 2. Session anatomy (`RepositorySystemSession`)

`maven-resolver-api/src/main/java/org/eclipse/aether/RepositorySystemSession.java`. Session is immutable once built and shareable across threads (javadoc lines 48-56). Resolver 2.x adds `CloseableSession`/`SessionBuilder` (lines 57-99); legacy mutable `DefaultRepositorySystemSession` still exists.

Knobs relevant to rewrite-maven (all on `SessionBuilder`):

| Knob | Where | Semantics |
|---|---|---|
| `setOffline(boolean)` | line 107 | enforced by `DefaultOfflineController` + `OfflinePipelineRepositoryConnectorFactory`; offline miss surfaces as `ArtifactNotFoundException` wrapping `RepositoryOfflineException` (`DefaultArtifactResolver.java:362-374`) |
| `setIgnoreArtifactDescriptorRepositories` | line 118 | if true, repos declared in transitive POMs are ignored; only request repos used (feeds `BfDependencyCollector.doRecurse` `args.ignoreRepos`, BF lines 387-390) |
| `setResolutionErrorPolicy` | line 127 | see §2.1 |
| `setArtifactDescriptorPolicy` | line 136 | `STRICT`/`IGNORE_MISSING`(0x01)/`IGNORE_INVALID`(0x02)/`IGNORE_ERRORS` (`resolution/ArtifactDescriptorPolicy.java:31-48`) |
| `setChecksumPolicy`, `setUpdatePolicy`, `setArtifactUpdatePolicy`, `setMetadataUpdatePolicy` | lines 148-193 | global overrides of per-repository `RepositoryPolicy` (`UPDATE_POLICY_ALWAYS/DAILY/NEVER/interval:N`) |
| `setLocalRepositoryManager` | line 208 | direct LRM injection ("Do not use it, unless you know what are you doing" — intended path is a `LocalRepositoryManagerFactory` with priority, javadoc lines 198-203) |
| `setWorkspaceReader` | line 217 | "consulted first to resolve artifacts" — see §3.2 |
| `setRepositoryListener` / `setTransferListener` | lines 225-233 | see §3.4 |
| `setSystemProperties/UserProperties/ConfigProperties` | lines 245-297 | config properties drive nearly every internal knob (`aether.*` keys) |
| `setMirrorSelector` / `setProxySelector` / `setAuthenticationSelector` | lines 307-329 | apply ONLY to repositories discovered in artifact descriptors; repos passed in requests must already be mirrored/proxied/authenticated ("not used for remote repositories which are passed as request parameters", lines 300-303, 310-312, 321-323). Ready impls in util: `DefaultMirrorSelector`, `DefaultProxySelector`, `DefaultAuthenticationSelector`, `ConservativeAuthenticationSelector` (maven-resolver-util `util/repository/`) |
| `setArtifactTypeRegistry` | line 337 | packaging→extension/classifier mapping |
| `setDependencyTraverser/Manager/Selector`, `setVersionFilter`, `setDependencyGraphTransformer` | lines 345-379 | the collect-time strategy quadruple + graph transformer (§4) |
| `setData` / `setCache` (+ supplier variants) | lines 389-444 | `SessionData` (ad-hoc state) and `RepositoryCache` (see §2.4); passing one instance across sessions shares caches across sessions (javadoc lines 89-92, 382-399) |
| `setScopeManager` | line 407 | optional Maven4-style scope system |
| `withLocalRepositoryBaseDirectories/withLocalRepositories` | lines 446-483 | shortcut; multiple dirs → chained (head writable, tails read-only "split" repos) |

### 2.1 ResolutionErrorPolicy — failure caching

`maven-resolver-api/src/main/java/org/eclipse/aether/resolution/ResolutionErrorPolicy.java`:
- `CACHE_DISABLED=0x00`, `CACHE_NOT_FOUND=0x01`, `CACHE_TRANSFER_ERROR=0x02`, `CACHE_ALL` (lines 42-60).
- Javadoc (lines 25-33): a marker is written in the local repo to suppress repeated attempts for a broken resource; the marker goes stale when the repo's update policy expires; and **the marker is keyed by network config, so changing auth/proxy retriggers revalidation immediately**.
- `SimpleResolutionErrorPolicy` (`maven-resolver-util/.../util/repository/SimpleResolutionErrorPolicy.java`): fixed bitmask per artifacts/metadata; `new SimpleResolutionErrorPolicy(true, true)` = cache both not-found and transfer errors. Maven itself defaults to caching not-found but NOT transfer errors (`-C`/`-c` flags flip this); an embedder chooses freely.
- Per-request granularity possible: policy methods receive a `ResolutionErrorPolicyRequest<Artifact|Metadata>` (lines 69-78), so a custom impl can vary by artifact/repository.

### 2.2 DefaultUpdateCheckManager — how failures are remembered

`maven-resolver-impl/src/main/java/org/eclipse/aether/internal/impl/DefaultUpdateCheckManager.java`. This is the exact machinery Jon suspected Maven lacked — it exists and is precise:

- **On-disk state (ACROSS sessions/processes):**
  - Artifacts: a sibling touch-file `<artifact>.lastUpdated` (`getArtifactTouchFile`, lines 355-357; `UPDATED_KEY_SUFFIX=".lastUpdated"` line 66). Keys inside: `<normalizedRepoUrl[+mirroredUrls]>.lastUpdated` (successful/not-found timestamp; `getDataKey(RemoteRepository)` lines 363-380) and `<repoKey>.error` where repoKey = `authDigest@proxy>authDigest@contentType-id-url` (`getRepoKey`, lines 394-410) — **auth and proxy digests are part of the key**, which is what makes "fix credentials → retry immediately" work.
  - Metadata: a per-directory `resolver-status.properties` (`getMetadataTouchFile`, lines 359-361) with `<filename>.lastUpdated`/`.error` keys (lines 386-392).
  - Write logic (`write`, lines 510-534): success → clear error key, stamp lastUpdated; `ArtifactNotFoundException`/`MetadataNotFoundException` → `error=""` (empty string means NOT_FOUND, `NOT_FOUND=""` line 70) + stamp; any other transfer error → `error=<message>` + stamp transfer key. On artifact success the touch file is **deleted** entirely (`touchArtifact`, lines 481-483).
  - Read/decision logic (`checkArtifact`, lines 136-208): if error recorded and file absent, the session's `ResolutionErrorPolicy` bitmask is consulted (`Utils.getPolicy`) against `CACHE_NOT_FOUND` (error=="") or `CACHE_TRANSFER_ERROR` (error!="") — if the bit is set, resolution is suppressed and a **cached, self-describing exception** is synthesized ("was not found in … during a previous attempt. This failure was cached in the local repository and resolution is not reattempted until the update interval of … has elapsed", `newException` lines 218-241).
- **In-memory state (WITHIN a session):** `SESSION_CHECKS` map in `SessionData` (lines 73-78, 438-456); once a file was checked this session it is never re-checked (`isAlreadyUpdated`), controllable via `aether.updateCheckManager.sessionState` = `enabled` (default) / `bypass` / anything-else=disabled (lines 90-99, 424-436). **Reusing one session (or one SessionData) across many resolutions gives process-wide "check once" behavior.**
- Timestamps semantics: `TS_NEVER=0` ("never downloaded → go get it"), `TS_UNKNOWN=1` ("have file, unknown age → policy decides") (lines 101-118).
- All touch-file IO goes through `TrackingFileManager` (`read`/`update`), whose default impl does per-file `FileLock`-based interprocess-safe read-modify-write (impl `NamedLocksTrackingFileManager`/`LegacyTrackingFileManager` in `internal/impl/`; supplier wires it via `TrackingFileManagerSupplier` bound to the named-lock selector, mvn3 supplier lines 227-229).

### 2.3 `_remote.repositories` — provenance tracking in the local repo

`EnhancedLocalRepositoryManager` (`maven-resolver-impl/.../internal/impl/EnhancedLocalRepositoryManager.java`):
- Per-directory `_remote.repositories` properties file with keys `filename>repoId` (javadoc lines 43-58). Empty repoId = locally installed → always accepted (lines 188-190). Otherwise the artifact is only "available" if it was downloaded from one of the repositories **in the current request** (lines 191-201) — this is the mechanism that prevents cross-repo cache poisoning, and is also the source of the infamous "artifact is present but cached from a repository ID that is unavailable" re-download (logged at `DefaultArtifactResolver.java:347-352`).
- Untracked-but-present files are assumed locally installed (inter-op with simple LRM, lines 202-208).
- In-memory `trackingFileCache` per manager instance to avoid re-reading tracking files (lines 93-104).
- `SimpleLocalRepositoryManager` is the no-tracking variant (any present file accepted). Selection between them is by `LocalRepository.getContentType()` (`"simple"`/`"enhanced"`) through priority-ordered `LocalRepositoryManagerFactory` (§3.1).

### 2.4 RepositoryCache

`maven-resolver-api/.../RepositoryCache.java`: opaque, thread-safe key/value store on the session ("data … meant for exclusive consumption by the repository system", lines 24-27). `DefaultRepositoryCache` = `ConcurrentHashMap`. Users can share ONE cache across sessions via `setCache`/`setRepositoryCacheSupplier` (API lines 391-399, 436-444). What actually lands in it during collection: `DataPool` intern pools — artifacts, dependencies, **parsed artifact descriptors (POM results)**, dependency lists (`internal/impl/collect/DataPool.java:171-206`, pools "live across session (if session carries non-null RepositoryCache)" lines 137-155; descriptor pool defaults to hard references, `CONFIG_PROP_COLLECTOR_POOL_DESCRIPTOR` default HARD lines 84-90). Additionally Maven's `DefaultArtifactDescriptorReader` puts model-builder `ModelCache` entries there (via `DefaultModelCacheFactory` — maven core side). **This is the resolver's "hot POM cache": share one `RepositoryCache` across sessions and descriptor re-reads become map hits.**

### 2.5 VersionFilter

Session-level `setVersionFilter` (API line 370) filters candidate versions during range resolution in collect. Stock impls in `maven-resolver-util/.../util/graph/version/`: `HighestVersionFilter`, `LowestVersionFilter`, `SnapshotVersionFilter` (drop snapshots), `ContextualSnapshotVersionFilter` (drop snapshots unless root is snapshot), `ChainedVersionFilter`, predicate-based variants (dir listing above).

---

## 3. SPIs an embedder can implement

### 3.1 `LocalRepositoryManager` — can it be backed by a custom POM/artifact cache? YES.

Interface: `maven-resolver-api/.../repository/LocalRepositoryManager.java`. Required operations:
- Path mapping (pure functions, no IO): `getAbsolutePathForLocalArtifact` / `...ForRemoteArtifact(artifact, repo, context)` / local+remote metadata variants (lines 50-144; the deprecated relative-`String` forms are the abstract ones).
- `find(session, LocalArtifactRequest) → LocalArtifactResult` (line 154): answer "is this artifact locally available, at which path, from which repo". Result carries `path`, `isAvailable`, `repository`.
- `add(session, LocalArtifactRegistration)` (line 164): record that an artifact was installed/downloaded (registration only — the connector writes the bytes to the path your manager returned).
- Metadata analogues `find(…, LocalMetadataRequest)` / `add(…, LocalMetadataRegistration)` (lines 174-185).

Wiring: implement `LocalRepositoryManagerFactory` (`maven-resolver-spi/.../spi/localrepo/LocalRepositoryManagerFactory.java`) keyed by `LocalRepository.getContentType()` with a `float getPriority()` (lines 33-55; provider iterates factories in descending priority) — in the supplier world just override `RepositorySystemSupplier.createLocalRepositoryProvider()` (mvn3 lines 535-550). Or brute-force `SessionBuilder.setLocalRepositoryManager` (API line 208).

Key constraint: resolved artifacts must ultimately be **files on a `Path`** (`ArtifactResult.getArtifact().getPath()`); a custom LRM can point paths anywhere (per-process dirs, content-addressed store, jimfs — the demo `Booter` runs the whole system on an in-memory jimfs FS, `Booter.java:83-89`), but it cannot return bytes-without-a-path. A custom LRM controls: layout, availability semantics (e.g. ignore `_remote.repositories` repo-matching), and can delegate hot-lookups to an in-memory index.

### 3.2 `WorkspaceReader` — reactor/in-repo POMs

`maven-resolver-api/.../repository/WorkspaceReader.java`: three methods — `getRepository()`, `findArtifact(Artifact) → File|null` (+ `findArtifactPath` default, lines 47-59), `findVersions(Artifact) → List<String>` (line 67).

Where consulted — **before the local repository and before any remote IO**:
- Artifact resolution: `DefaultArtifactResolver.resolve` checks system-scope path → version resolution → `workspace.findArtifactPath(artifact)` (lines 299-308) → LRM `find` (line 310) → remote download.
- Descriptor reading: Maven's `DefaultArtifactDescriptorReader` resolves the POM artifact through the same `ArtifactResolver`, so a workspace hit substitutes the on-disk `pom.xml` for the repo POM — this is exactly how Maven resolves reactor/parent POMs from the checkout. For rewrite-maven this is the natural home for "POMs of the repository being edited".
- Version resolution: Maven's `DefaultVersionResolver`/`DefaultVersionRangeResolver` consult `workspace.findVersions` for workspace versions (maven core side).

### 3.3 `Transporter` / `TransporterFactory` — custom HTTP client bridge

SPI: `maven-resolver-spi/.../spi/connector/transport/Transporter.java`. Contract:
- `classify(Throwable) → ERROR_NOT_FOUND | ERROR_OTHER` (lines 43-61) — the resolver's entire not-found-vs-transfer-error distinction (and thus §2.1/§2.2 failure caching) hinges on this method.
- `peek(PeekTask)` (existence check), `get(GetTask)` (download to file or memory, must feed `TransportListener` progress, must not delete partial files), `put(PutTask)`, `close()` (lines 63-96). **Must be thread-safe** (javadoc lines 32-33).
- `TransporterFactory.newInstance(session, RemoteRepository)` + `float getPriority()`; HTTP-specific subtype `HttpTransporterFactory`/`HttpTransporter` (spi `connector/transport/http/`) adds checksum-extraction hooks (`x-checksum-*` headers via `ChecksumExtractor`) and RFC9457 problem-details reporting.
- Selection: `DefaultTransporterProvider` picks the highest-priority factory that doesn't throw `NoTransporterException` for the repo's protocol.

Scope of work, measured on the three stock HTTP transports (main sources):
- **transport-jdk**: `JdkTransporter` 676 lines + factory 89 + RFC9457 reporter 70 (multi-release jar; jdk8 sub-module is a 73-line stub factory that always throws — JDK transport needs Java 11+, `JdkTransporterFactory.java:35-41` "on Java11+ it works", priority 10.0f line 44). Uses `java.net.http.HttpClient`, supports HTTP/2.
- **transport-apache**: `ApacheTransporter` 800 lines + ~700 lines of connection-manager/auth plumbing (`GlobalState`, `LocalState`, `DeferredCredentialsProvider`, `ConnMgrConfig` — connection pooling shared across transporter instances).
- **transport-jetty**: `JettyTransporter` 499 lines + 305 (`PutTaskRequestContent`) + factory 77.
So a from-scratch `Transporter` over an existing async HTTP client (e.g. OkHttp, or rewrite's `HttpSender`) is roughly a 400-800-line, well-bounded job; all retry/checksum/resume/policy logic lives ABOVE the transporter in `BasicRepositoryConnector`, and all caching/locking below-decides-nothing — the transporter is a dumb byte pump with an error classifier.

### 3.4 `RepositoryListener` / `TransferListener`

- `RepositoryListener` (`maven-resolver-api/.../RepositoryListener.java`): 19 lifecycle events — `artifactDescriptorInvalid/Missing`, `metadataInvalid`, `artifact/metadataResolving/Resolved`, `artifact/metadataDownloading/Downloaded`, installing/installed, deploying/deployed (lines 44-218). Set per-session (`setRepositoryListener`) or system-wide via supplier `createRepositoryListeners()` map (mvn3 supplier lines 607-619). `artifactDescriptorMissing/Invalid` are the observability hooks for lenient descriptor policies.
- `TransferListener` (api `transfer/TransferListener.java`): initiated/started/progressed/corrupted/succeeded/failed with `TransferEvent`/`TransferResource`. Demo impls: `examples/util/ConsoleTransferListener.java`, `ConsoleRepositoryListener.java`. Also note `ReverseTreeRepositoryListener` demo — reconstructs "why was this downloaded" chains from `RequestTrace`/`CollectStepData`, useful provenance pattern.

---

## 4. Dependency collection

Entry: `DefaultDependencyCollector` (`internal/impl/collect/DefaultDependencyCollector.java`) delegates by config `aether.dependencyCollector.impl`; **default is `bf`** (`DEFAULT_COLLECTOR_IMPL = BfDependencyCollector.NAME`, lines 58-61).

### 4.1 DF vs BF

- `df/DfDependencyCollector.java` (416 lines): classic Maven-3-style depth-first, single-threaded, recursion via `NodeStack` (lines 90-95). No skipper, no parallelism.
- `bf/BfDependencyCollector.java` (601 lines): breadth-first with a FIFO `dependencyProcessingQueue` (lines 221-224) plus:
  - **Skipper optimization** (`bf/DependencyResolutionSkipper.java`): before descending into a node, `skipResolution(node, parents)` can declare it a foregone conflict-loser (deeper than an already-seen instance of the same key) and skip descriptor work entirely; modes `versionless` (key=G:A:C:E, default), `versioned` (G:A:C:E:V), `false` via `aether.dependencyCollector.bf.skipper` (BF lines 93-117, 160-174). Skipped nodes still appear in the graph so ConflictResolver sees them.
  - **Parallel descriptor download**: `ParallelDescriptorResolver` on a `SmartExecutor` sized by `aether.dependencyCollector.bf.threads` (default **5**, lines 119-134, 176-182). Descriptors for queued dependencies are prefetched async (`resolveArtifactDescriptorAsync`, lines 440-467) — ranges resolve all matching versions, newest first "to maximize benefits of skipper" (lines 448-450), individual versions in a `parallelStream` (line 454).
- Child recursion (`doRecurse`, lines 366-438): derives child selector/manager/traverser/filter via `deriveChild*` (lines 377-385), aggregates repositories from the POM's `<repositories>` unless `ignoreArtifactDescriptorRepositories` (lines 387-390), and **caches children subtrees in `DataPool` keyed by (artifact, repos, selector, manager, traverser, filter)** (lines 392-401, 435-437) — identical contexts share subtrees.

### 4.2 How exclusions / optional / dependencyManagement apply during collect

These are session strategies, not collector logic:
- Exclusions: `ExclusionDependencySelector` (util `graph/selector/`) — a child selector derived per node filters out excluded GAs before they are ever queued (`selectDependency` calls at BF lines 200-202, 409-411).
- Optional: `OptionalDependencySelector.fromDirect()` — optional deps kept at level 0/1, dropped transitively.
- Scopes: `ScopeDependencySelector.legacy(null, [test, provided])` — test/provided dropped for transitive deps.
- dependencyManagement: `DependencyManager.deriveChildManager` accumulates each POM's `<dependencyManagement>`; `PremanagedDependency.create(depManager, dependency, disableVersionManagement, premanagedState)` applies version/scope/optional/exclusions management to each dependency as it is queued (BF lines 205-216, 414-416). `ClassicDependencyManager` = Maven 3 (root-level depMgmt only); `TransitiveDependencyManager` = Maven 4 semantics (util `graph/manager/`).

### 4.3 ConflictResolver — nearest-wins, ties, scope mediation

`maven-resolver-util/.../util/graph/transformer/ConflictResolver.java` — runs as a `DependencyGraphTransformer` AFTER collection.
- Pipeline (javadoc lines 89-100): `ConflictMarker` assigns conflict IDs by G:A:C:E; `ConflictIdSorter` topo-sorts + detects cycles; then the resolver walks conflict groups in dependency order.
- Two impls behind a delegating facade (lines 252-267): `classic` (O(N²) worst case, Maven 3 behavior, the default via `auto`) and `path` (O(N), "not yet recommended for production") — `aether.conflictResolver.impl` (lines 119-137).
- Pluggable strategy quadruple (ctor lines 241-250): `VersionSelector` (which version wins — `ConfigurableVersionSelector`/`NearestVersionSelector` implement **nearest-wins with first-seen tie-break**; throws `UnsolvableVersionConflictException` on hard range conflicts), `ScopeSelector` (`JavaScopeSelector` = widest-scope mediation among conflicting nodes), `OptionalitySelector`, `ScopeDeriver` (`JavaScopeDeriver` = parent-scope × child-scope derivation, e.g. anything under provided → provided).
- `ConfigurableVersionSelector` additionally supports "dependency convergence"-style strictness knobs (util, same package).

### 4.4 Verbose mode — provenance (winners, premanaged state)

- `ConflictResolver.CONFIG_PROP_VERBOSE` = `aether.conflictResolver.verbose`, values `NONE` (default; losers removed), `STANDARD` (losers retained childless, "for analysis only", link to winner), `FULL` (complete original graph retained) — `Verbosity` enum lines 144-173, `getVerbosity` accepts Boolean/String/enum (lines 185-197).
- Node-data keys on losing nodes: `NODE_DATA_WINNER` ("conflict.winner" → the winning `DependencyNode`), `NODE_DATA_ORIGINAL_SCOPE`, `NODE_DATA_ORIGINAL_OPTIONALITY` (lines 199-215).
- Dependency-management provenance: `DependencyManagerUtils.CONFIG_PROP_VERBOSE` = `aether.dependencyManager.verbose`; when set, `PremanagedDependency` records `premanaged.version/scope/optional/exclusions/properties` in node data plus `DependencyNode.getManagedBits()` flags; accessors `getPremanagedVersion(node)` etc. (`util/graph/manager/DependencyManagerUtils.java:44-88`).
- **Caveat:** STANDARD/FULL graphs "cannot be resolved" (javadoc lines 76-80) — for rewrite-maven's provenance-rich `MavenResolutionResult`, collect with verbose ON to build the model, and run a separate NONE-mode resolution (or apply a filter) if actual artifact downloads of the winning set are needed.

---

## 5. Version machinery

### 5.1 GenericVersionScheme ordering

`maven-resolver-util/.../util/version/GenericVersionScheme.java` javadoc (lines 27-49) is the normative spec:
- Segments split on `-`, `_`, `.` and digit↔letter transitions; delimiters equivalent.
- Numeric segments compare mathematically; alpha segments lexicographically case-insensitively; special qualifiers ordered `alpha=a < beta=b < milestone=m < cr=rc < snapshot < final=ga(=release="") < sp`, all well-known qualifiers < arbitrary strings.
- `min`/`max` tokens and `[M.N.*]` range sugar (lines 40-44).
- Number-vs-string collisions resolved by padding with `0`/`ga` ("1-alpha" = "1.0.0-alpha" < "1.0.1-ga" = "1.0.1", lines 46-49).
- Implementation: `GenericVersion.parse`/`compareTo` with Item kinds MIN < QUALIFIER < STRING < INT < BIGINT < MAX (`GenericVersion.java:294-310`), trailing-zero trimming (`trimPadding`, lines 86-103); parsed versions interned in a concurrent weak cache (`GenericVersionScheme.java:54-61`). This is deliberately kept behavior-compatible with maven-artifact's `ComparableVersion` (the `main()` even mimics its output, lines 68-80). `VersionRange` parsing lives in `VersionSchemeSupport`/`GenericVersionRange` (same package).

### 5.2 VersionResolver / VersionRangeResolver (META-level: these live in Maven core)

`org.eclipse.aether.impl.VersionResolver` and `.VersionRangeResolver` are resolver-internal SPIs, but their implementations `DefaultVersionResolver` and `DefaultVersionRangeResolver` are in `org.apache.maven:maven-resolver-provider` (imported by suppliers, mvn3 lines 32-33; `find . -name DefaultVersionResolver.java` in this repo → nothing). Semantics (Maven-core code, constructor wiring visible in supplier lines 1081-1101/1106-1126):
- `VersionResolver`: resolves metaversions — `RELEASE`/`LATEST` via `maven-metadata.xml` `<versioning><release|latest>`, and `X-SNAPSHOT → X-timestamp-build` via `maven-metadata.xml` `<snapshot>` per repository; consults workspace; caches results in the session `RepositoryCache`.
- `VersionRangeResolver`: parses constraints with the version scheme; for ranges, reads `maven-metadata.xml` `<versions>` from **all** repositories (resolved through resolver's `DefaultMetadataResolver`) and unions them, tagging each version with the repository it came from (`VersionRangeResult.getRepository(version)` — used by BF collector at line 322).

### 5.3 Metadata read/merge across repositories (resolver side)

`DefaultMetadataResolver` (`internal/impl/DefaultMetadataResolver.java`): resolves each `MetadataRequest` per repository (local copy named per-repo, e.g. `maven-metadata-central.xml`), applying the metadata update policy and update-check/error caching from §2.2, downloading **in parallel across requests** with `aether.metadataResolver.threads` = default **4** (lines 85-94, 312). Merging of the version lists across repositories is the CALLER's job (Maven's version(-range) resolver); `MergeableMetadata.merge` (api `metadata/MergeableMetadata.java`) is for deploy-time merging of remote+local metadata.

---

## 6. Concurrency

### 6.1 What is locked and by whom

- The unit of locking is the **`SyncContext`** (api `SyncContext.java`), acquired around local-repository mutation/read sets. `DefaultArtifactResolver` acquires a SHARED context first, and only escalates to EXCLUSIVE when actual downloads are needed (`resolve` retry loop: `current.acquire(subjects, null)` line 217; `if (!groups.isEmpty() && current == shared) { current = exclusive; continue; }` lines 393-397). Same pattern in `DefaultMetadataResolver`, `DefaultInstaller`, `DefaultDeployer`.
- `DefaultSyncContextFactory` → `NamedLockFactoryAdapter` (`internal/impl/synccontext/named/NamedLockFactoryAdapter.java:128-131`): a `NameMapper` maps artifacts/metadata to lock names, then a `NamedLockFactory` provides the locks; acquisition is all-or-nothing over sorted keys with configurable wait (`aether.syncContext.named.time` etc.).
- Defaults: name mapper `file-gaecv` (`NamedLockFactoryAdapterFactoryImpl.DEFAULT_NAME_MAPPER_NAME = NameMappers.FILE_GAECV_NAME`, line 56) — i.e. **file-per-G:A:E:C:V lock files under `<localRepo>/.locks`** (`BasedirNameMapper.CONFIG_PROP_LOCKS_DIR`, `DEFAULT_LOCKS_DIR=".locks"`, lines 50-52). Config keys: `aether.syncContext.named.factory`, `aether.syncContext.named.nameMapper` (lines 68-78).

### 6.2 Named lock implementations (`maven-resolver-named-locks`)

`org/eclipse/aether/named/providers/`:
- `NoopNamedLockFactory` — none.
- `LocalReadWriteLockNamedLockFactory` / `LocalSemaphoreNamedLockFactory` — JVM-local only.
- **`FileLockNamedLockFactory`** ("file-lock") — `FileChannel.lock()` advisory file locks → **true interprocess mutual exclusion on the local repository**; lock names must be `file:` URIs (hence the `file-*` name mappers); Windows quirk knobs `aether.named.file-lock.deleteLockFiles/attempts/sleepMillis` (`FileLockNamedLockFactory.java:44-101`).
- Separate modules: Hazelcast and Redisson factories (distributed, for build farms).

### 6.3 IPC named locks (`maven-resolver-named-locks-ipc`)

`IpcNamedLockFactory` (NAME="ipc", `IpcNamedLockFactory.java:44-86`): delegates all locking to a **shared lock-server process**; `IpcClient` auto-discovers or **forks the server daemon** (java or native binary) and connects over Unix-domain or TCP sockets (`IpcClient.java:98-248`; `SocketFamily`); server self-terminates after idle timeout (`IpcServer.SYSTEM_PROP_IDLE_TIMEOUT`, `IpcServer.java:53-65`; nofork/native/debug knobs lines 45-65). This exists precisely for many-JVMs-one-local-repo (Maven daemon/mvnd heritage). For rewrite-maven, `file-lock` is the zero-infra interprocess answer; `ipc` is the high-throughput one.

### 6.4 Thread-safety & parallelism knobs

- `RepositorySystem` is thread-safe; a built session is immutable and shareable (`RepositorySystemSession` javadoc lines 48-52; supplier javadoc "constructed RepositorySystem is thread safe", mvn3 lines 169-170). `SessionBuilder` is not thread-safe (API line 87). Caveat: if you share `SessionData`/`RepositoryCache` instances across sessions they must be thread-safe (they are: `DefaultSessionData`/`DefaultRepositoryCache` are CHM-based).
- Knobs (defaults): BF collector descriptor threads `aether.dependencyCollector.bf.threads`=5 (BF lines 119-134); metadata resolver `aether.metadataResolver.threads`=4; basic connector parallel GET/PUT threads (`BasicRepositoryConnectorConfigurationKeys.CONFIG_PROP_THREADS`); executors are pooled per-session via `SmartExecutorUtils.smartExecutor` stored in `SessionData` (`util/concurrency/SmartExecutorUtils.java:92-95`).
- Whole `resolveArtifacts` batches are internally grouped per repository and downloaded via the connector's own thread pool; multiple `resolveDependencies` calls may run concurrently on one session (demo `ResolveTransitiveDependenciesParallel`).

---

## 7. `ArtifactDescriptorReader` — the POM-reading seam (lives OUTSIDE resolver)

- SPI: `maven-resolver-impl/src/main/java/org/eclipse/aether/impl/ArtifactDescriptorReader.java` — ONE method: `readArtifactDescriptor(session, ArtifactDescriptorRequest) → ArtifactDescriptorResult`, must honor the session's `ArtifactDescriptorPolicy` (lines 35-47). Marked `@provisional`.
- Output shape `ArtifactDescriptorResult` (api `resolution/ArtifactDescriptorResult.java:42-60`): artifact (possibly relocated), source repository, **dependencies**, **managedDependencies**, **repositories** (the POM's `<repositories>`), relocations chain, aliases, properties, exceptions. This is ALL the collector ever sees of a POM (`DependencyCollectorDelegate`/`BfDependencyCollector` consume exactly these fields).
- Implementation location: **NOT in this repo** (`find . -name DefaultArtifactDescriptorReader.java` → empty). It is `org.apache.maven.repository.internal.DefaultArtifactDescriptorReader` inside **Maven core's `maven-resolver-provider`** artifact (Maven 3.9.x) or Maven 4's equivalent (rc-5), pulled in as an external dependency by the suppliers (mvn3 pom line 86-88; imports mvn3 supplier line 30). What it does (per its wiring, supplier lines 1059-1069/1083-1094): resolve `G:A:V` → POM version via `VersionResolver`; download the POM via `ArtifactResolver` (which itself consults WorkspaceReader → LRM → remote); run **maven-model-builder** (`ModelBuilder`, `DefaultModelBuilderFactory` — full interpolation, profile activation, parent resolution, import-scope BOM expansion) with a session-`RepositoryCache`-backed `ModelCache` (`DefaultModelCacheFactory`); convert the effective `Model` to `ArtifactDescriptorResult`; apply relocations (mvn4: pluggable `MavenArtifactRelocationSource` chain, supplier mvn4 lines 1053-1071).
- **Consequence for rewrite-maven:** the seam is clean and coarse. Options: (a) use Maven's reader as-is (get exact Maven POM semantics — profiles, interpolation, BOMs — for free); (b) wrap/decorate it to intercept results into rewrite's Pom cache; (c) replace it entirely with a reader backed by rewrite's own `RawPom`→effective-model pipeline while keeping resolver's collection/mediation — the whole collector contract is just this one interface plus `VersionResolver`/`VersionRangeResolver`. The supplier makes each independently overridable (`createArtifactDescriptorReader`, `createVersionResolver`, `createVersionRangeResolver`, `createModelCacheFactory`).

---

## 8. Mapping rewrite-maven needs → resolver extension points

| rewrite-maven need | Resolver extension point | Mechanics / citation | Confidence |
|---|---|---|---|
| **Pluggable POM cache** (parsed effective models) | Session `RepositoryCache` (+ `ModelCacheFactory` and `DataPool` descriptor pool ride on it); for deeper control, decorate/replace `ArtifactDescriptorReader` via `RepositorySystemSupplier.createArtifactDescriptorReader()` | `RepositorySystemSession.setCache`/`setRepositoryCacheSupplier` (api lines 391-444); `DataPool` descriptor intern pool "lives across session" (`DataPool.java:148-155,171-206`); supplier override point mvn3 lines 1059-1069 | **High** for cross-session in-memory reuse; **Medium** for a persistent/serialized POM cache (RepositoryCache values are opaque resolver-internal objects — persistence requires owning the `ArtifactDescriptorReader` or `ModelCacheFactory`) |
| **Pluggable artifact (binary) cache** | `LocalRepositoryManager` + `LocalRepositoryManagerFactory` (priority-selected), or chained/split local repos via `withLocalRepositories` | interface ops §3.1 (`LocalRepositoryManager.java:50-185`); factory priority (`LocalRepositoryManagerFactory.java:33-55`); supplier `createLocalRepositoryProvider` mvn3 535-550 | **High** (this is exactly what the SPI is for; constraint: results must be real `Path`s) |
| **Custom HTTP client** | `Transporter`/`TransporterFactory` (or `HttpTransporterFactory`) registered via `createTransporterFactories()`; priority beats stock ones | SPI contract `Transporter.java:35-96`; stock impls 500-800 LOC each (§3.3); selection by priority in `DefaultTransporterProvider` | **High** (bounded, dumb-byte-pump contract; `classify()` must be implemented carefully — it feeds failure caching) |
| **Failure caching** (dead repos / missing artifacts remembered, within & across processes) | `ResolutionErrorPolicy` (`SimpleResolutionErrorPolicy(CACHE_ALL)`) + `DefaultUpdateCheckManager` `.lastUpdated`/`resolver-status.properties` machinery + per-repo update policies + session-scoped once-per-session check | `ResolutionErrorPolicy.java:42-60`; `DefaultUpdateCheckManager.java:136-241,510-534` (auth/proxy-keyed error records, synthesized cached exceptions); session state `CONFIG_PROP_SESSION_STATE` lines 90-99 | **High** — this directly refutes reason (2) for the custom algorithm: resolver has first-class, policy-controlled negative caching at artifact, metadata, and repo+auth granularity, both intra-session (memory) and inter-session (disk) |
| **Interprocess locking on shared local repo** | Named locks: `file-lock` factory + `file-gaecv` mapper (default `<repo>/.locks/*`), or `ipc` lock-server module; SHARED→EXCLUSIVE escalation already built into resolvers | §6.1-6.3; `FileLockNamedLockFactory.java`; `IpcNamedLockFactory.java:44-86`; adapter default mapper `NamedLockFactoryAdapterFactoryImpl.java:56` | **High** — directly answers reason (3); nothing to build, only configure |
| **Reactor / in-repo POMs** | `WorkspaceReader` on the session | `WorkspaceReader.java:32-68`; consulted before LRM/remote in `DefaultArtifactResolver.java:299-308`, and by Maven's descriptor/version resolvers | **High** |
| **Provenance-rich results** (winner tracking, premanaged state, requested→resolved mapping) | `ConflictResolver` verbose `STANDARD`/`FULL` + `DependencyManagerUtils.CONFIG_PROP_VERBOSE`, node-data keys `conflict.winner`, `premanaged.*`; `VersionRangeResult.getRepository(version)`; `RequestTrace`/`CollectStepData`; `RepositoryListener` events | `ConflictResolver.java:107-215`; `DependencyManagerUtils.java:44-88`; BF collector repo attribution line 321-322 | **High** for graph provenance; **Medium** on ergonomics — verbose graphs are "analysis only" (not resolvable), so building `MavenResolutionResult` likely means one verbose collect + mapping, mirroring Maven's own `mvn dependency:tree -Dverbose` |
| **Raw resolution speed** | BF collector (default) with skipper + 5-thread parallel descriptor prefetch; shared `RepositoryCache` across sessions; hard descriptor pool; metadata resolver 4-thread parallelism; `UPDATE_POLICY_NEVER` + session once-per-check; subtree caching in `DataPool` | §4.1, §6.4; `BfDependencyCollector.java:93-134,440-467`; `DataPool.java` | **High** that the levers exist; benchmarking vs rewrite's current algorithm still required |
| **Strict `MavenResolutionResult` API compat** | Not a resolver extension point — an adapter layer mapping `CollectResult`/`DependencyNode`(+node data) → rewrite tree entities | shapes: `ArtifactDescriptorResult.java:42-60`, `DependencyNode` node-data keys §4.4 | **Medium** — feasible but this is where the real migration work concentrates (scope model, `dependencyManagement` provenance, repository attribution per node) |
| **Deterministic no-DI embedding** | `RepositorySystemSupplier` (mvn3 now; mvn4 when Maven 4 GA settles) + `SessionBuilderSupplier` | §1; demo `Booter.java:79-117` | **High** |

### Key judgment calls for the rewrite-maven migration

1. **Pick the mvn3 supplier initially** (Maven 3.9.16 provider = the semantics today's ecosystem builds against; `ClassicDependencyManager`), but architect the bootstrap behind rewrite's own factory so the mvn4 supplier (TransitiveDependencyManager, relocation sources, `VersionScheme`-aware range resolver) is a swap.
2. **The one thing resolver does NOT give you** is a bytes-not-files artifact store and a persistable parsed-POM cache format: `LocalRepositoryManager` must yield `Path`s, and `RepositoryCache` holds opaque live objects. If rewrite needs its existing serialized POM cache (e.g. RocksDB `MavenPomCache`), the insertion point is a custom `ArtifactDescriptorReader` decorator (cheap: single-method interface) rather than `RepositoryCache`.
3. **`Transporter.classify()` is load-bearing**: a custom HTTP bridge that misclassifies 404 vs connect-timeout will corrupt the not-found/transfer-error caching that motivates this whole migration.
