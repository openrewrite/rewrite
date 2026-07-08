# A2: Pluggability seams and I/O behavior in rewrite-maven that a Maven-API-backed resolver must preserve

All paths relative to repo root `rewrite-maven/src/main/java/org/openrewrite/maven/` unless noted.
Line numbers from worktree `maven-resolution` at `75ffca60d3`.

---

## 1. MavenPomDownloader end-to-end flow

File: `internal/MavenPomDownloader.java` (1314 lines).

### 1.1 Construction and configuration inputs

Four constructors (lines 101–148). All funnel to the private constructor (line 138) which reads from the
`ExecutionContext`:

| Field | Source | Line |
|---|---|---|
| `httpSender` | `HttpSenderExecutionContextView.view(ctx).getHttpSender()` | 105/119/130 |
| `mavenCache` (a `MavenPomCache`) | `MavenExecutionContextView.getPomCache()` | 144 |
| `mavenSettings` | ctor arg if non-null, else `ctx.getSettings()` | 106, 143 |
| `mirrors` | `ctx.getMirrors(mavenSettings)` | 107, 147 |
| `activeProfiles` | ctor arg only (nullable) | 108 |
| `addCentralRepository` | `!FALSE.equals(ctx.getAddCentralRepository())` in the project-poms ctor (line 145) but `TRUE.equals(...)` in the no-project ctor (line 120) — i.e. **default true** for Maven parsing contexts and **default false** for the `MavenPomDownloader(ExecutionContext)` (Gradle-side) ctor |
| `addLocalRepository` | same dual-default pattern (lines 121, 146) |
| `projectPoms` / `projectPomsByGav` | ctor arg; GAV map computed with property-placeholder resolution across the in-project parent ancestry (lines 174–238) |

Callers: `MavenParser.parse` (line 101 of `MavenParser.java`), ~10 Maven recipes (`MavenVisitor#downloadMetadata`
480/494, `UpdateMavenModel:198`, `ChangeParentPom:307/412`, `UpgradeDependencyVersion:531/561`, etc.), and many
rewrite-gradle recipes via `new MavenPomDownloader(ctx)` (e.g. `rewrite-gradle/.../DependencyVersionSelector.java:217`).
`ResolvedPom.resolve*`/`resolveDependencies` receive the downloader as a parameter — it is the *only* network
gateway for the whole resolution algorithm.

### 1.2 POM download (`download`, lines 492–753)

Order of resolution for `download(gav, relativePath, containingPom, repositories)`:

1. **Fail fast** if g/a/v missing (line 496) → `MavenDownloadingException`; emits `resolutionListener.downloadError`.
2. Listener: `ctx.getResolutionListener().download(gav)` (503).
3. **Project POM short-circuits** (never hits cache or network):
   - exact resolved GAV match in `projectPomsByGav` (507);
   - raw g/a match with raw version or placeholder-substituted version match (513–528);
   - `<relativePath>` local-parent resolution mirroring Maven's `DefaultModelBuilder#readParentLocally`
     (531–554): default relativePath `".."`, empty `<relativePath/>` means *don't* look locally, GAV
     verification with relaxed version check when the version contains `${`.
4. **Named-version resolution**: `LATEST`/`RELEASE` resolved via `downloadMetadata` (`resolveNamedVersion`, 822–843).
5. **Snapshot handling**: `datedSnapshotVersion` (778–820) computes the timestamped version (see §1.4);
   `handleSnapshotTimestampVersion` (764–776) converts a timestamped input version (regex
   `^(.*-)?([0-9]{8}\.[0-9]{6}-[0-9]+)$`, line 67) back to its `-SNAPSHOT` base version.
6. Unresolved `${...}` in version → throw (561).
7. **Repository iteration** over `distinctNormalizedRepositories(...)` (566, see §1.5). For each repo:
   - listener `.repository(repo, containingPom)` (579);
   - skip if `repositoryAcceptsVersion` fails (581; snapshot/release policy per repo, plus a hardcoded
     special case for `https://repo.spring.io/milestone` accepting only `.*(M|RC)\d+$`, lines 1235–1247);
   - **cache probe**: `mavenCache.getPom(resolvedGav)` where `resolvedGav` = `(repoUri, g, a, v, datedSnapshotVersion)`
     (585–587). Tri-state: `null` = never tried; `Optional.empty()` = **cached failure** → repositoryResponses gets
     "Did not attempt to download because of a previous failure..." (731); present → return cached (727–729).
   - **file:// repos** (596–649): looks for `artifactId-version.pom` and sibling `.jar`; a pom whose packaging is
     jar-like but whose jar is missing/empty is *skipped* (622–627, "unusable"); poms found in the default local repo
     get `MavenRepository.MAVEN_LOCAL_USER_NEUTRAL` substituted as their repository so paths are user-independent (629–631).
   - **http(s) repos** (650–726): GET `<repo>/<g>/<a>/<v>/<a>-<datedSnapshotOrV>.pom` via
     `requestAsAuthenticatedOrAnonymous`; if body contains `published-with-gradle-metadata`, also fetches the
     `.module` Gradle Module Metadata and injects `platform`/`enforcedPlatform` deps as `import`-scope BOM entries into
     dependencyManagement (663–684) — a deliberate Gradle-emulation divergence from Maven;
     module fetch failures are swallowed unless server-unreachable (685–691).
   - **POM-less JAR fallback**: on client-side pom failure, HEAD the sibling `.jar` (`jarExistsForPomUri`, 1083–1109);
     if the jar exists, remember `(repo, resolvedGav)` and, after all repos, synthesize a minimal jar-packaging `RawPom`
     (`rawPomFromGav`, 755–759) and return it as a real Pom (735–746). Emits `downloadSuccess`.
   - **Negative caching**: only on `isClientSideException()` (all 4xx except 408/425/429, lines 1210–1215):
     `mavenCache.putPom(resolvedGav, null)` (721). IOExceptions and 5xx are NOT negatively cached.
   - Success: `mavenCache.putPom(resolvedGav, pom)`, listener `.downloadSuccess`, timer tag `outcome=downloaded|from maven local|cached`.
8. All repos exhausted → listener `.downloadError(gav, uris, containingPom)`, throw
   `MavenDownloadingException` carrying per-repository responses (`setRepositoryResponses`, 750–751).

### 1.3 Metadata download + cross-repo merging (`downloadMetadata`, 246–353)

- GAV values substituted through `containingPom.getValues(gav)` (252).
- Listener `.downloadMetadata(gav)` (255).
- Iterates **all** normalized repos and **merges** results (Maven itself effectively does per-repo pick; OpenRewrite
  merges across every repo):
  - cache probe `mavenCache.getMavenMetadata(URI.create(repo.getUri()), gav)` — tri-state like poms (270);
  - file scheme reads `maven-metadata-local.xml` (283); http reads `maven-metadata.xml` at
    `<repo>/<g-as-path>/<a>/[<v>/]` — version segment omitted for null/LATEST/RELEASE versions (276–279);
  - 400–404 ⇒ `cacheEmptyResult = true` (299–302);
  - **derived metadata** fallback (`deriveMetadata`, 365–399): when a repo has no maven-metadata.xml (Nexus),
    scrape the html index `<a href="...">` dir listing (or a file:// directory listing) for version directories
    (401–458); disabled per-repo when `deriveMetadataIfMissing == false`, or when a specific version is requested;
    a non-404 client error *mutates* `repo.setDeriveMetadataIfMissing(false)` for the rest of the run (392–397);
    metric `rewrite.maven.derived.metadata`.
  - empty result + `cacheEmptyResult` ⇒ `putMavenMetadata(repoUri, gav, null)` — **negative metadata cache entry** (324–328);
  - each successful per-repo result is put into the cache *per-repo* and merged into the running result
    (`mergeMetadata`, 460–469): union of versions, concat snapshotVersions, max snapshot timestamp, max lastUpdated,
    `Semver.max` of latest/release.
- No result from any repo ⇒ `MavenDownloadingException` with repositoryResponses; listener `.downloadError` (344–349).

### 1.4 Snapshot timestamp resolution (`datedSnapshotVersion`, 778–820)

- **Pinned snapshots first**: `ctx.getPinnedSnapshotVersions()` matches on g/a/v and returns the pinned
  `datedSnapshotVersion` — deterministic snapshot resolution knob (780–787).
- Otherwise download metadata for the GAV; if that fails, return the plain `-SNAPSHOT` version (local-only artifact case).
- Selects the newest `<snapshotVersion>` whose classifier equals the classifier declared in the containing pom's
  requested dependencies (`getClassifierFromContainingPom`, 1273–1284), sorted by `updated` desc (797–810).
- Fallback: `SNAPSHOT` suffix replaced with `<timestamp>-<buildNumber>` from `<snapshot>` (812–816).

### 1.5 Repository normalization pipeline (`distinctNormalizedRepositories`, 845–917; `normalizeRepository`, 919–1040)

Ordering of the candidate repo list (LinkedHashMap keyed by repo id — later same-id entries overwrite earlier):
1. `"local"` → `ctx.getLocalRepository()` if `addLocalRepository` (861);
2. repos from settings/context: `ctx.getRepositories(mavenSettings, activeProfiles)` (865);
3. repos passed as parameter (the POM's own repositories) (870);
4. `MavenRepository.MAVEN_CENTRAL` appended if `addCentralRepository` and no `central` id present (875).

Returned as a **lazy iterator** that normalizes on demand, de-dups by normalized id, and filters by
`repositoryAcceptsVersion` (880–916).

Per-repo `normalizeRepository(originalRepository, ctx, containingPom)`:
- property substitution of the URI against the containing pom (`${ARTIFACTORY_URL}/repo`) (922–925);
- **mirror application** (`applyMirrors` → `MavenRepositoryMirror.apply`, 926/1186–1188) then
  **credential application** (`applyAuthenticationToRepository` → `MavenRepositoryCredentials.apply(ctx.getCredentials(mavenSettings), repo)`, 1143–1145);
- file URI normalization (percent-encoding, backslashes; `normalizeFileUri`, 930/1291–1313);
- `knownToExist` short-circuits everything (935) — no probe, no cache;
- unresolved `${` in URI ⇒ return null (never cached, deliberately: transient during parent property resolution, 939–946);
- URI containing `0.0.0.0` ⇒ blocked, null (948–954);
- `file` scheme ⇒ returned as-is (957–959);
- cache probe `mavenCache.getNormalizedRepository(repository)` — tri-state; cached-absent emits
  `repositoryAccessFailedPreviously` (960, 993–995);
- non-http(s) scheme (e.g. s3) ⇒ null + `repositoryAccessFailed` (962–966);
- **run-scoped unreachable-endpoint set**: key `host:port` (`endpointOrNull`, 1181–1184) in
  `ctx.getUnreachableEndpoints()` (a `ConcurrentHashMap.newKeySet()` on the ExecutionContext,
  `MavenExecutionContextView:91`); if present, skip without probing (974–978);
- **https-preference probe** (`normalizeRepository(repository)`, 1005–1040): try OPTIONS on the https variant of the URL
  (trailing `/` appended), then HEAD https, then OPTIONS original http URL, then HEAD original.
  `SUCCESS` (2xx) or `ERROR` (any HTTP response ≥100, except 408 — "server reached", 1230–1232) accepts the URL;
  total unreachability throws, which records the endpoint into `unreachableEndpoints` and calls
  `repositoryAccessFailed` (982–989);
- result (including null=failure) written to `putNormalizedRepository` (991, 999);
- credentials re-applied to the (possibly cached) normalized repo before return (1002).

### 1.6 Credential/header/timeout application to requests

`applyAuthenticationAndTimeoutToRequest` (1150–1175):
- **Only when `mavenSettings.servers` is non-null**: sets connect timeout (repo.timeout or 10s) and read timeout
  (repo.timeout or 30s), and copies matching `<server><configuration>` httpHeaders + timeout (both connect & read)
  for the server whose id equals the repo id. Otherwise the HttpSender's own defaults apply
  (JDK sender: 1s connect / 10s read, `rewrite-core/.../HttpUrlConnectionSender.java:31-32`).
- Basic auth from `repository.username/password` (populated earlier by `MavenRepositoryCredentials.apply`).

`requestAsAuthenticatedOrAnonymous` (1115–1138): replicates Maven's behavior — if an authenticated GET fails with a
client-side (4xx, non-408/425/429) error and the repo has credentials, retry anonymously; if the anonymous retry is
denied (401–403) rethrow the *original* exception.

### 1.7 Retry/backoff

Static Failsafe `RetryPolicy` (59–65): retries only `SocketTimeoutException`, `TimeoutException`, or
`UncheckedIOException(SocketTimeoutException)`; 500ms delay, 10% jitter, max 5 retries. Applied in `sendRequest`
(150–172, which also accumulates wall-clock into `ctx.recordResolutionTime`) and in `jarExistsForPomUri` (1087).
Non-2xx responses become `HttpSenderResponseException(code, body)` — not retried.

### 1.8 Failure-caching summary (the behavior a replacement must reproduce)

| What | Key | Value | Trigger | Lifetime |
|---|---|---|---|---|
| POM absent | `ResolvedGroupArtifactVersion(repoUri,g,a,v,datedSnapshot)` | `Optional.empty()` via `putPom(gav,null)` | 4xx except 408/425/429 | Lifetime of the `MavenPomCache` (in-memory: Caffeine, size-bounded, no TTL; RocksDB: **not stored** — putPom(null) is dropped, line RocksdbMavenPomCache:156–159) |
| Metadata absent | `(repoURI, gav)` | `Optional.empty()` | 400–404 on metadata AND derive failed | same as above (RocksDB never stores metadata at all) |
| Repository normalization failure | `MavenRepository` (equality = id+uri+releases+snapshots+knownToExist; **credentials excluded**, `tree/MavenRepository.java:46-71`) | `Optional.empty()` via `putNormalizedRepository(repo,null)` | probe threw (unreachable) or generic exception | MavenPomCache lifetime (RocksDB impl: not stored) |
| Unreachable endpoint | `host:port` string | set membership | connection-level failure on every probed URL | **one execution context / run** (`MavenExecutionContextView:91`) |
| deriveMetadataIfMissing disabled | mutated field on the (shared, normalized) `MavenRepository` instance | `false` | non-404 4xx on the index scrape | lifetime of that repo object (effectively the run, via the normalized-repo cache) |

IOExceptions, 5xx, and 408/425/429 are **never** negatively cached — retried on next request.

---

## 2. Cache contracts

### 2.1 `MavenPomCache` (`cache/MavenPomCache.java`)

Four logical sub-caches, all with **tri-state** get semantics (`null` = unknown, `Optional.empty()` = known-absent,
present = hit) — annotated `@SuppressWarnings("OptionalAssignedToNull")` everywhere:

1. `get/putPom(ResolvedGroupArtifactVersion)` → `Optional<Pom>` — raw parsed POM per (repoUri,g,a,v,datedSnapshot). Negative entries allowed.
2. `get/putMavenMetadata(URI repo, GroupArtifactVersion)` → `Optional<MavenMetadata>` — per-repository metadata. Negative entries allowed.
3. `get/putNormalizedRepository(MavenRepository)` → `Optional<MavenRepository>` — result of the https/http probe. Negative entries allowed.
4. `get/putResolvedDependencyPom(ResolvedGroupArtifactVersion)` → `ResolvedPom` (no Optional; no negative entries) —
   **fully resolved** dependency POMs. Written/read only by `ResolvedPom.resolveDependencies`
   (`tree/ResolvedPom.java:1107, 1123`); value is `resolvedPom.deduplicate()`d before store in the in-memory impl (InMemoryMavenPomCache:88).

Construction/lifecycle: lazily defaulted to `new InMemoryMavenPomCache()` by `MavenExecutionContextView.getPomCache()`
(`MavenExecutionContextView:158-160`); hosts override with `setPomCache` (e.g. Moderne CLI uses
`CompositeMavenPomCache(InMemory, Rocksdb)`). The interface has no eviction/close API — persistence lifecycle belongs
to implementations.

### 2.2 `InMemoryMavenPomCache`

Caffeine caches: poms 100k, metadata 100k, normalized repos 10k, resolved dependency poms 100k; metrics via
`CaffeineCacheMetrics` (lines 41–72). Stores `Optional`s directly, so negative entries survive.

### 2.3 `RocksdbMavenPomCache` — deliberate divergences

- Only sub-cache #1 (raw POMs) is persisted. Metadata (135–143: "will change over time"), normalized repositories
  (169–175), and resolved dependency poms (126–132) are all no-ops returning `null`.
- **Never stores negative pom entries**: `putPom(gav, null)` returns immediately (156–159), so cached-failure state is
  in-memory-only (via the Composite's L1) and never persists across processes.
- Keying: `gav.toString()` UTF-8 bytes, Jackson-Smile serialized (148, 162). Values: Smile-serialized `Pom`.
- Model-version stamp `org.openrewrite.maven.internal.Pom.version` (currently 3, `tree/Pom.java:61-64`)
  checked at open; mismatch or checksum failure destroys and recreates the DB (238–269).
- Interprocess characteristics: one shared static `RocksCache` per directory within a JVM (73, 96–98); deletes a stale
  `LOCK` file at construction (117–121); WAL disabled, 1MB write buffer, paranoid checks off, close via JVM shutdown
  hook semantics documented at 47–62.

### 2.4 `CompositeMavenPomCache`

L1/L2 read-through with L1 backfill; note that **negative entries do not backfill** (`l2m.isPresent()` guards, lines
58, 77, 96), but a *negative* L2 answer (Optional.empty) is still returned as authoritative. Writes go to both layers.

### 2.5 `MavenArtifactCache` (`cache/MavenArtifactCache.java`)

- `getArtifact(ResolvedDependency) -> @Nullable Path`, `putArtifact(dep, InputStream, Consumer<Throwable>) -> @Nullable Path`.
- `computeArtifact` default method: **non-dated SNAPSHOTs are always re-fetched** (mutable, e.g. Maven-local publishes;
  lines 52–70); otherwise get-then-put.
- `orElse(other)` combinator chains caches (84–99). `NOOP` constant (28–44).
- `LocalMavenArtifactCache`: layout `<cache>/<g-as-path>/<a>/<v>/<a>-<versionOrDatedSnapshot>[-classifier].jar`
  (71–90); directory creation synchronized on the cache root — **no interprocess locking** (77–81).
- `ReadOnlyLocalMavenArtifactCache`: get-only; `mavenLocal()` points at `~/.m2/repository` (31–34) — this is how
  `~/.m2` is reused without ever writing to it.
- Consumers: `utilities/MavenArtifactDownloader` (ctor arg) and `MavenExecutionContextView.getArtifactCache()`
  (default `LocalMavenArtifactCache(~/.rewrite/cache/artifacts)`, falling back to a JVM-lifetime temp dir;
  lines 177–207). `rpc/JavaRewriteRpc.java:169-176` wires it for recipe-bundle resolution.

### 2.6 `MavenArtifactDownloader` (`utilities/MavenArtifactDownloader.java`)

Binary (jar) download path, separate from pom resolution: URL built from `dependency.getRepository().getUri()` +
standard layout with datedSnapshotVersion and classifier (96–105); `~`-home and `file://` URIs read from disk (109–112);
same Failsafe retry policy (52–58); credentials/httpHeaders resolved from `MavenSettings.Server` by repo id, falling
back to repo-embedded username/password, with `${` placeholders rejected (189–204); authenticated→anonymous fallback on
client error mirroring the pom downloader (126–134); type != jar returns null (92). Errors go to the `onError`
consumer, not exceptions (136–141).

---

## 3. MavenSettings (`MavenSettings.java`)

Modeled subset of settings.xml (fields, lines 54–95): `localRepository`, `profiles` (each: id, `ProfileActivation`
(activeByDefault/jdk/property), `RawRepositories` — **repositories only**, no pluginRepositories in settings profiles),
`activeProfiles`, `mirrors` (id/url/mirrorOf/releases/snapshots), `servers` (id/username/password +
`ServerConfiguration{httpHeaders, timeout}`), `proxies` (id/active/protocol/host/port/username/password/nonProxyHosts).
**NOT modeled**: `offline`, `interactiveMode`, `pluginGroups`, `usePluginRegistry`, per-repo `updatePolicy`/`checksumPolicy`.

**Proxies are parsed, interpolated, merged, password-decrypted — but never consumed by resolution.** No caller of
`getProxies()` exists outside `MavenSettings` itself (grep). Proxy support today comes only from whatever `HttpSender`
the host injects (JDK sender honors a `java.net.Proxy` ctor arg / system properties,
`rewrite-core/.../HttpUrlConnectionSender.java:56`).

Sourcing:
- `parse(Parser.Input|Path, ctx)` (97–124) — Jackson-XML (`MavenXmlMapper`), then `Interpolator` resolves `${...}`
  against system properties, then `env.NAME`, then bare env (253–330). Interpolation covers everything **except**
  profiles (POM-visible section, comment at 249–252).
- `readMavenSettingsFromDisk(ctx)` (126–139): `~/.m2/settings.xml` merged over `$MVN_HOME|M2_HOME|MAVEN_HOME/conf/settings.xml`
  (181–195); merge is per-section putIfAbsent keyed by id (205–214, 342+).
- Password decryption via `MavenSecuritySettings` (`~/.m2/settings-security.xml` + relocation support,
  `MavenSecuritySettings.java:85-99`), Maven's `{...}` master-password AES scheme (141–168).
- Test harness: `Assertions.customizeExecutionContext` auto-loads disk settings when nothing is configured
  (`Assertions.java:47-58`).

Feeding resolution: `MavenExecutionContextView.setMavenSettings(settings, activeProfiles...)` **replaces** active
profiles, credentials (from servers), mirrors, localRepository, repositories (from active profiles' repos) on the
context (309–323). At download time the downloader consults settings again through
`ctx.getRepositories(mavenSettings, activeProfiles)` / `getCredentials(mavenSettings)` / `getMirrors(mavenSettings)`
which prefer a *passed* settings object over context state (119–124, 141–151, 260–266). Per-POM provenance: the
`MavenResolutionResult` marker stores the settings (with `withServers(null)` sanitization, `MavenParser.java:104-105`)
and `effectiveSettings(mrr)` merges context settings over marker settings (`MavenExecutionContextView:334-342`) —
recipes use this via `MavenVisitor.downloadMetadata` (`MavenVisitor.java:474-496`).

`getMavenLocal()` (235–243): `localRepository` from settings or `~/.m2/repository` (`MAVEN_LOCAL_DEFAULT`),
`knownToExist=true`.

---

## 4. MavenExecutionContextView — every knob

Message keys `org.openrewrite.maven.*` (lines 45–58). Setters are used by hosts (Moderne CLI/plugins — outside this
repo), `Assertions` (tests), and `JavaRewriteRpc`.

| Knob | Set by | Read by |
|---|---|---|
| `settings` (`setMavenSettings` — also fans out to 5 other knobs) | hosts; `Assertions:56`; build plugins | downloader ctor (143), `MavenParser:104`, `MavenVisitor.effectiveSettings`, `getLocalRepository` (219) |
| `activeProfiles` | `setMavenSettings`; hosts | `MavenParser:106` (concat with parser's own), downloader via ctor arg |
| `mirrors` | `setMavenSettings`; hosts | `MavenPomDownloader` ctor → `applyMirrors` (1186) |
| `credentials` (`MavenRepositoryCredentials`) | `setMavenSettings`; hosts | `applyAuthenticationToRepository` (1144) |
| `localRepository` | `setMavenSettings`; hosts | `distinctNormalizedRepositories` ("local", 861) |
| `addLocalRepository` (Boolean, tri-state) | hosts, gradle tooling | downloader ctors (121, 146) |
| `addCentralRepository` (Boolean, tri-state) | `JavaRewriteRpc:159`; hosts | downloader ctors (120, 145) |
| `repositories` | hosts (inject extra repos) | `getRepositories(settings, profiles)` fallback (265); seed for settings-repo enrichment (374) |
| `pinnedSnapshotVersions` | hosts (deterministic snapshot builds) | `datedSnapshotVersion` (780) |
| `pomCache` | hosts | `getPomCache()` (lazy `InMemoryMavenPomCache`) → downloader (144) + `ResolvedPom:1106` |
| `artifactCache` | hosts | `getArtifactCache()` (lazy `LocalMavenArtifactCache(~/.rewrite/cache/artifacts)` → temp fallback, 177–207) |
| `resolutionListener` | hosts (build tools log/report through this) | 28 call sites in downloader + 7 in `ResolvedPom` |
| `resolutionTime` | `sendRequest` accumulates (downloader 170) | hosts (metrics) |
| `unreachableEndpoints` | `normalizeRepository` (986) | `normalizeRepository` (975) |

Note `getRepositories(settings, activeProfiles)` merges: settings-profile repos are *enriched* with
credentials/knownToExist from same-id context repos (373–414).

---

## 5. HttpSender transport

- Interface `org.openrewrite.ipc.http.HttpSender` (rewrite-core); only bundled impl is `HttpUrlConnectionSender`
  (JDK `HttpURLConnection`; defaults 1s connect/10s read; optional `java.net.Proxy` ctor). No OkHttp in this repo —
  OkHttp-based senders are injected by hosts via `HttpSenderExecutionContextView.setHttpSender`
  (`rewrite-core/.../HttpSenderExecutionContextView.java:36-43`; default lazily `new HttpUrlConnectionSender()`).
- The downloader uses `get`, `head`, `options` builders; basic auth via `withBasicAuthentication`; per-request
  connect/read timeouts and custom headers (from `<server><configuration>`); response streaming via
  `getBodyAsBytes`. A separate `largeFileHttpSender` exists for `Remote*` (not used by maven resolution).
- Proxy/`nonProxyHosts` from settings.xml are **not** applied anywhere; proxying is entirely the HttpSender's concern.

---

## 6. ResolutionEventListener (`tree/ResolutionEventListener.java`)

Default-method interface, `NOOP` default. Events and emitters:

| Event | Emitted from |
|---|---|
| `downloadMetadata(gav)` | downloader 255 |
| `download(gav)` | downloader 503 |
| `downloadSuccess(resolvedGav, containing)` | downloader 638, 697, 744 |
| `downloadError(gav, attemptedUris, containingPom)` | downloader 346, 498, 644, 714, 748 |
| `repository(repo, containingPom)` | downloader 265, 579 (once per repo per request) |
| `repositoryAccessFailed(uri, t)` | downloader 943, 951, 964, 988, 997 |
| `repositoryAccessFailedPreviously(uri)` | downloader 976, 994 (cache/endpoint-set hits) |
| `parent(parentPom, containing)` | `ResolvedPom` 526, 558 |
| `dependency(scope, resolvedDependency, containing)` | `ResolvedPom` 1139 |
| `bomImport(gav, containing)` | `ResolvedPom` 898 |
| `property(key, value, containing)` | `ResolvedPom` 889 |
| `dependencyManagement(managedDep, containing)` | `ResolvedPom` 961 |
| `clear()` | `ResolvedPom` 1088 (conflict-resolution restart discards prior events) |

Consumers in this repo: tests only (`MavenPomDownloaderTest`, `ResolvedPomTest`). Real consumers are hosts
(Moderne CLI/build plugins) that reconstruct a resolution log/graph — the `clear()` restart semantics and
exactly-once `dependency` emission per resolved node are load-bearing there.

---

## 7. Minimal seam contract for a Maven-API-backed backend

1. **`MavenPomCache` (pluggable, tri-state)** — keep the interface and semantics verbatim: four sub-caches; `null`
   vs `Optional.empty()` vs value; negative caching *only* for deterministic 4xx (not 408/425/429/5xx/IO); keys
   exactly `(repoUri,g,a,v,datedSnapshot)` for poms, `(repoURI, gav)` for metadata, credential-free `MavenRepository`
   equality for normalization. A Maven-API backend must route Maven's model reads through this cache (or an adapter)
   rather than only `~/.m2`, and must not persist negative entries into long-lived stores (Rocksdb precedent).
2. **`MavenArtifactCache` (pluggable)** — keep `get/put/computeArtifact/orElse`; preserve the non-dated-SNAPSHOT
   always-refetch rule and the `~/.m2`-read-only composition pattern; artifact path layout is observable API
   (classpath construction downstream).
3. **`HttpSender` transport seam** — all network I/O must flow through the context-provided `HttpSender`
   (hosts inject instrumented/OkHttp/proxied senders). Replacing with Maven Resolver's own transporters would break
   this; a custom `TransporterFactory` bridging to `HttpSender` is the compatible shape. Preserve: auth-then-anonymous
   fallback, basic auth from settings servers, custom `httpHeaders`, per-server timeout, retry policy scope
   (timeouts only, max 5, 500ms+jitter).
4. **`MavenExecutionContextView` knobs** — every knob in §4 must keep working, notably `pinnedSnapshotVersions`,
   `addLocal/addCentralRepository` tri-states with their differing per-constructor defaults, context-injected
   `repositories`/`mirrors`/`credentials` that exist independently of any settings.xml, and `resolutionTime` accounting.
5. **`MavenSettings` as the only settings model** — sourced from explicit object, context, or disk
   (`~/.m2` + `M2_HOME/conf`, env-var interpolation, settings-security decryption); per-POM provenance via
   `MavenResolutionResult.mavenSettings` + `effectiveSettings` merge; `withServers(null)` sanitization before
   storing in LSTs. A backend translating to Maven's `Settings` must round-trip exactly this subset and must not
   start honoring un-modeled fields (offline, updatePolicy) silently.
6. **Repository pipeline order and failure memory** — local → settings/context → pom repos → central; id-keyed
   de-dup where later wins; mirror match semantics (`*`, `external:*`, `!repo`, local exclusion —
   `tree/MavenRepositoryMirror.java:116-175`); credential application by server-id; https-preference probing
   (or an equivalent that still avoids per-artifact probing of dead repos); run-scoped `host:port` unreachable set;
   normalized-repo caching including negative results; `repositoryAcceptsVersion` release/snapshot gating.
7. **`ResolutionEventListener`** — emit all 13 events at the same semantic points, including `clear()` on
   conflict-resolution restart and `repositoryAccessFailedPreviously` on cached failures.
8. **Behavioral quirks that are de-facto API** (unit-tested, host-visible): project-pom short-circuit incl.
   `<relativePath>`; LATEST/RELEASE named versions; snapshot classifier-aware timestamp selection + pinning +
   timestamped-version→base-version folding; metadata **merging across all repos** (not first-wins);
   Nexus html-index metadata derivation with its self-disabling flag; Gradle Module Metadata BOM injection;
   POM-less-jar synthesis; `MAVEN_LOCAL_USER_NEUTRAL` substitution; spring milestone-repo version filter;
   `file://` repo semantics incl. missing-jar-means-unusable. Each is either preserved, or consciously dropped with
   Maven-parity justification and test updates.
