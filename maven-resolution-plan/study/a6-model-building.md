# A6 — Maven model building (effective POM construction): Maven 3.9 vs Maven 4

Scope: embeddability + fidelity of the two model-builder stacks for replacing rewrite-maven's custom
resolution. All paths relative to `/Users/jon/Projects/github/apache/maven-3.9.x` (branch `maven-3.9.x`,
version `3.9.17-SNAPSHOT`, HEAD 2026-06-29) and `/Users/jon/Projects/github/apache/maven-4` (branch
`master`, version `4.1.0-SNAPSHOT`, HEAD 2026-07-07). Both clones are depth-1; release/branch signals
taken from `git ls-remote` (see §7).

---

## 1. Maven 3.9 `DefaultModelBuilder` pipeline

Entry point: `maven-model-builder/src/main/java/org/apache/maven/model/building/DefaultModelBuilder.java`.
Two-phase design; `build(request)` runs phase 1 and, unless `request.isTwoPhaseBuilding()`, immediately
phase 2 (`DefaultModelBuilder.java:418-420`).

### Phase 1 — lineage assembly (`build(ModelBuildingRequest, Collection<String> importIds)`, :252-423)

1. **Read + raw-validate input model** — `readModel(...)` (:571-656): `ModelProcessor.read` with
   `IS_STRICT` (validation level ≥ `VALIDATION_LEVEL_MAVEN_2_0`) and an `InputSource` allocated only when
   `request.isLocationTracking()` (:586-591); strict parse failure retries non-strict with WARNING/ERROR
   problem (:596-619); then `modelValidator.validateRawModel` (:649).
2. **Profile activation context** (:266, `getProfileActivationContext` :658-673): active/inactive profile
   ids, system properties, user properties **enriched with the raw model's `packaging`** as
   `ProfileActivationContext.PROPERTY_NAME_PACKAGING` (:664-667), project directory.
3. **External (settings) profiles** selected once: `profileSelector.getActiveProfiles(request.getProfiles(), ...)`
   (:269-270); their properties are folded into the context's user properties (:274-281).
4. **Walk the parent lineage** (loop at :291-372). For each model, super-POM last:
   - clone raw model (:297);
   - **model normalization** `modelNormalizer.mergeDuplicates` (:303) — de-dupes duplicate
     plugin/dependency declarations (`normalization/DefaultModelNormalizer.java`);
   - **interpolate profile *activations* only** (`getInterpolatedProfiles` :430-493): `RegexBasedInterpolator`
     over project/user/system properties; `file.exists/missing` additionally go through
     `ProfileActivationFilePathInterpolator` (path-translating, `${basedir}` aware) (:470-475); `os.*`,
     `property.name/value` and `jdk` are plain-interpolated (:476-490);
   - **per-POM profile selection** `profileSelector.getActiveProfiles(tmpModel.getProfiles(), ...)` (:310-311).
     Activators (pluggable `@Named ProfileActivator` set): `profile/activation/JdkVersionProfileActivator.java`,
     `OperatingSystemProfileActivator.java`, `PropertyProfileActivator.java`, `FileProfileActivator.java`.
     `DefaultProfileSelector.java` adds explicit-id activation and "activeByDefault only when nothing else
     activated" semantics;
   - **profile injection** `profileInjector.injectProfile` per active POM profile (:320-322)
     (`profile/DefaultProfileInjector.java` — merge with profile-dominant semantics); **external profiles
     are injected into the result (bottom) model only** (:324-328);
   - **feed repositories to the resolver** `configureResolver(request.getModelResolver(), tmpModel, ...)`
     (:334, :675-702) → `ModelResolver.addRepository(repo, replace=false)` for each `<repository>`;
   - **parent resolution** `readParent` (:806-865):
     * cache lookup `ModelCacheTag.RAW` keyed g:a:v (:821);
     * `readParentLocally` (:867-991): if a `WorkspaceModelResolver` is present it wins
       (`resolveRawModel`, :890-905); otherwise `getParentPomFile` resolves `parent.relativePath` via
       `ModelSource2.getRelatedSource(path)` (:1000-1012; note: **empty relativePath ⇒ skip local, no
       default ".." here — the default `../pom.xml` lives in the modello default of `Parent.relativePath`**).
       Candidate must match parent g:a (mismatch ⇒ WARNING + fall back to repository, :923-941); version
       mismatch: plain version ⇒ fall back to repo; **version ranges honored** — candidate must be inside
       the range and child version must be constant (MNG-2199 FATAL) (:942-977);
     * `readParentExternally` (:1014-1099): `ModelResolver.resolveModel(Parent)` (range-capable),
       validation degraded to `VALIDATION_LEVEL_MAVEN_2_0` via `FilterModelBuildingRequest` (:1062-1070),
       same "Version must be a constant" checks (:1074-1093);
     * parent-cycle detection via `parentIds` set of model ids (:358-369, FATAL);
     * parent packaging must be `pom` (:854-859).
5. **checkPluginVersions** (:704-745) — WARNING when plugin version neither declared nor managed
   anywhere in lineage (validation ≥ 2.0).
6. **Inheritance assembly** top-down (`assembleInheritance` :747-754 → 
   `inheritance/DefaultInheritanceAssembler.java`, a `MavenModelMerger` subclass; handles URL child-path
   appending etc.).
7. **Model interpolation** — one full pass over the assembled result model only (`interpolateModel`
   :767-804 → `interpolation/StringVisitorModelInterpolator.java`). Value-source precedence
   (`AbstractStringBasedModelInterpolator.createValueSources` :99-190, in order): basedir/baseUri,
   `${build.timestamp}` (`BuildTimestampValueSource`), model object (prefixed `pom.`/`project.`),
   **user properties**, **model properties**, **system properties**, `env.` fallback, un-prefixed model
   object; with path-translating and URL-normalizing post-processors. Profile *activations* are restored
   to their raw values after interpolation (:796-801). The parent's version additionally gets its own
   user→model→system interpolation (:773-793). Note deliberate quirk: user properties beat POM properties
   here.
8. **URL normalization** `modelUrlNormalizer.normalize` (:399) — collapses `/../` in URLs.
9. **Repositories replaced with interpolated ones**: `configureResolver(..., replaceRepositories=true)` (:402).
10. Result gets effective model + per-model-id raw models and active profiles (:404-416).

### Phase 2 (`build(request, result, imports)`, :501-555)

Order: `modelPathTranslator.alignToBaseDirectory` (:512) → **pluginManagementInjector.injectManagement**
(:515, `management/DefaultPluginManagementInjector.java` — merges `build/pluginManagement/plugins` into
`build/plugins`) → `BUILD_EXTENSIONS_ASSEMBLED` listener event (:517) → lifecycle bindings injection only
if `isProcessPlugins()` (:519-526) → **importDependencyManagement** (:529) →
**dependencyManagementInjector.injectManagement** (:532, `management/DefaultDependencyManagementInjector.java`
— applies managed versions/scopes/exclusions to `dependencies`) → `modelNormalizer.injectDefaultValues`
(:534 — e.g. dependency `type=jar` etc.) → report/plugin configuration expansion if processPlugins
(:536-545) → `modelValidator.validateEffectiveModel` (:548).

### dependencyManagement import (BOMs) — order + recursion (:1105-1264)

- Iterates the (post-injection) `dependencyManagement.dependencies` in declaration order; every
  `type=pom&scope=import` entry is **removed from the list** (:1128-1135) and resolved.
- g/a/v must be fully specified — ERROR otherwise (:1141-1161).
- **Cycle detection** via `importIds` collection threaded through recursive `build()` calls (:1163-1174;
  seeded with the importing model at :1117-1119).
- Cache lookup `ModelCacheTag.IMPORT` (g:a:v) (:1176-1177).
- `WorkspaceModelResolver.resolveEffectiveModel` tried first (:1189-1198), else
  `ModelResolver.resolveModel(g,a,v)` and a **full recursive `build()`** of the BOM with
  `VALIDATION_LEVEL_MINIMAL`, shared `ModelCache`, same system/user properties, `locationTracking`
  propagated, and `modelResolver.newCopy()` (:1220-1242).
- Merge semantics: `composition/DefaultDependencyManagementImporter.java:44-75` — target's own managed
  deps win; across BOMs **first import wins** (`if (!dependencies.containsKey(key))`). **No exclusion
  filtering of imports (MNG-5600 is NOT in 3.9)** and no conflict warning (MNG-8004 is 4.x).

### Validation levels

`ModelBuildingRequest.java:42-63`: `MINIMAL=0`, `MAVEN_2_0=20`, `MAVEN_3_0=30`, `MAVEN_3_1=31`,
`STRICT=MAVEN_3_0`. Dependency/parent POMs are built at `MINIMAL`; `DefaultModelValidator.java` keys
most checks off the level.

### SPIs (all constructor/setter injectable)

- `ModelResolver` (`resolution/ModelResolver.java`) — `resolveModel(g,a,v)`, `resolveModel(Parent)`,
  `resolveModel(Dependency)`, `addRepository(repo, replace)`, `newCopy()`.
- `ModelCache` (`building/ModelCache.java:30-53`) — flat `(g,a,v,tag)→Object` get/put; tags are
  `ModelCacheTag.RAW` (ModelData) and `ModelCacheTag.IMPORT` (DependencyManagement), both stored as
  **deep clones** via `ModelCacheTag.intoCache/fromCache`.
- `WorkspaceModelResolver` (`resolution/WorkspaceModelResolver.java`) — `resolveRawModel`,
  `resolveEffectiveModel` (reactor/workspace override).
- `ModelBuildingListener` (`building/ModelBuildingListener.java`) — single event
  `buildExtensionsAssembled` fired in phase 2 (:517).
- Also swappable: `ModelProcessor`/`ModelReader`/`ModelLocator`, `ProfileActivator`s, `ModelValidator`,
  `ModelInterpolator`, `InheritanceAssembler`, `SuperPomProvider`, etc.

### Plexus-free? **Yes.**

`DefaultModelBuilderFactory` (`building/DefaultModelBuilderFactory.java:78-224`) wires every component
with plain `new` (JSR-330 annotations are inert without a container; the `LifecycleBindingsInjector` is a
no-op stub, :226-231). Compile-scope deps of maven-model-builder (`maven-model-builder/pom.xml:35-62`):
`plexus-interpolation`, `javax.inject` (annotations only), `maven-model`, `maven-artifact`,
`maven-builder-support`, `org.eclipse.sisu.inject` (annotations only; sisu.plexus/guice are test scope).
No plexus container, no wagon, no OSGi.

---

## 2. maven-resolver-provider (3.9): `DefaultArtifactDescriptorReader`

File: `maven-resolver-provider/src/main/java/org/apache/maven/repository/internal/DefaultArtifactDescriptorReader.java`.
This is exactly the "read dependency info from a POM" seam that rewrite's `MavenPomDownloader` +
`RawPom→Pom→ResolvedPom` replaces.

Flow (`loadPom`, :199-354):
1. `VersionResolver.resolveVersion` for the artifact and its POM artifact (SNAPSHOT/`RELEASE`/`LATEST` →
   concrete, via metadata; `DefaultVersionResolver.java`) (:206-225).
2. Relocation-cycle guard on g:a:baseVersion (:227-236).
3. `ArtifactResolver.resolveArtifact` of the POM (download to local repo) (:238-255); honors
   `session.getArtifactDescriptorPolicy()` for missing/invalid (IGNORE_MISSING/IGNORE_INVALID → return
   null descriptor instead of throwing, :396-402).
4. Workspace short-circuit: `MavenWorkspaceReader.findModel` (:259-266).
5. **Model build** (:268-323): `DefaultModelBuildingRequest` with `VALIDATION_LEVEL_MINIMAL`,
   `processPlugins=false`, `twoPhaseBuilding=false`; **system properties = session user props merged over
   session system props, user properties = empty (MNG-7563: dependency POMs must not see the build's user
   properties as user properties, but still see them for interpolation)** (:273-277); `ModelCache` from the
   pluggable `ModelCacheFactory` (`DefaultModelCache` stores into resolver `RepositoryCache` keyed by
   session, `DefaultModelCache.java:36-58`); `DefaultModelResolver` bridging `ModelResolver` SPI onto
   aether `ArtifactResolver`/`VersionRangeResolver`/`RemoteRepositoryManager` (:279-286).
6. `<distributionManagement><relocation>` loop (:339-353).

`DefaultModelResolver.java` details: repository precedence — request repositories first, POM-declared
repos recessively aggregated via `RemoteRepositoryManager.aggregateRepositories` (:110-133), honoring
`session.isIgnoreArtifactDescriptorRepositories()` (:117-119); parent/dependency version **ranges**
resolved to highest matching version, unbounded-upper ranges rejected (:169-253).

Result mapping: `ArtifactDescriptorReaderDelegate.populateResult` (:52-90) — model repositories →
`result.repositories`; `model.getDependencies()` → aether `Dependency` (type→`ArtifactType` stereotype,
system-path property, exclusions normalized to `g:a:*:*`) ; `dependencyManagement.dependencies` →
`managedDependencies`; prerequisites + licenses flattened into string properties. **The delegate is
overridable per session** via config property `ArtifactDescriptorReaderDelegate.class.getName()`
(:185-191) — a clean hook to project the *full effective model* out of the descriptor reader (a known
trick to avoid re-reading the POM: put the `Model` into `ArtifactDescriptorResult.properties`).

`MavenRepositorySystemUtils` (:55-128): `newSession()` builds the Maven-flavored resolver session —
`FatArtifactTraverser`, `ClassicDependencyManager` (Maven's "manage only depth≥2" rule),
`AndDependencySelector(Scope(test,provided) + Optional + Exclusion)`, `ConflictResolver(NearestVersion,
JavaScope, SimpleOptionality, JavaScopeDeriver)` + `JavaDependencyContextRefiner`, the full artifact-type
stereotype table, `SimpleArtifactDescriptorPolicy(ignoreMissing=true, ignoreInvalid=true)`. The deprecated
`newServiceLocator()` (:69-78) still exists; modern wiring is `RepositorySystemSupplier` from
maven-resolver-supplier.

**Reuse vs reimplement for rewrite**: reuse `DefaultModelBuilder` (via factory), reuse
`DefaultArtifactDescriptorReader`+`DefaultVersionResolver`+`DefaultVersionRangeResolver` wholesale;
implement rewrite-side: a `ModelResolver`/`ModelCache` (or resolver-level `RepositoryCache` +
`WorkspaceReader`) backed by rewrite's pluggable POM cache, and the `ArtifactDescriptorReaderDelegate`
override to capture effective models for `ResolvedPom` construction.

---

## 3. Maven 4: new API + impl

### API surface

- `org.apache.maven.api.services.ModelBuilder` (`api/maven-api-core/.../services/ModelBuilder.java:26-44`):
  `newSession() → ModelBuilderSession.build(ModelBuilderRequest)`, plus `buildRawModel`. Knows model
  versions 4.0.0 / 4.1.0 / 4.2.0.
- `ModelBuilderRequest` (`.../ModelBuilderRequest.java:46-137`): immutable, builder-created;
  `RequestType` enum {`BUILD_PROJECT`, `BUILD_EFFECTIVE`, `BUILD_CONSUMER`, `CONSUMER_PARENT`,
  `CONSUMER_DEPENDENCY`} replaces 3.9's validation-level/processPlugins/twoPhase flag soup;
  `isLocationTracking`, `isRecursive`, external `profiles` + active/inactive ids, system/user property
  maps, `RepositoryMerging` {POM_DOMINANT, REQUEST_DOMINANT}, explicit `repositories`, optional
  lifecycle-bindings `ModelTransformer`.
- `Sources` (`.../Sources.java:61-92`): `buildSource(path)` (build POMs, `CacheRetention.REQUEST_SCOPED`,
  can `resolve()` relative paths), `resolvedSource(path, modelId)` (repo POMs — cache-retention default),
  `fromPath`.
- `ModelBuilderResult` (`.../ModelBuilderResult.java:35-129`): fileModel / rawModel / parentModel /
  effectiveModel, active POM profiles **per model id**, active external profiles, problem collector,
  recursive `getChildren()` (whole reactor from one call).
- SPIs in `api/maven-api-spi` + `api/maven-api-core/.../services/model/`: `ModelResolver` (record-based
  request; returns source + possibly rewritten version; classifier/extension-aware for mixins),
  `ModelProcessor`, `ProfileActivator`, `ModelTransformer` (file/raw/effective hooks), plus the same
  decomposition as 3.9 (`InheritanceAssembler`, `DependencyManagementImporter`, …) — all as `Service`s.

### Implementation (`impl/maven-impl/src/main/java/org/apache/maven/impl/model/DefaultModelBuilder.java`, 2756 lines)

Same conceptual phases, but session-oriented and immutable-model-based:

- `ModelBuilderSessionState` carries request, result, a file-graph DAG for reactor cycle detection,
  `mappedSources` (GA→source map = built-in workspace/reactor resolution: `resolveReactorModel` :2203-2216),
  and the repository lists (`pomRepositories`/`externalRepositories`/`repositories`) — repository merging
  is **internalized** (`mergeRepositories` :562-607) instead of 3.9's mutable `ModelResolver.addRepository`.
- `BUILD_PROJECT` loads the whole reactor from the root POM **in parallel** (`PhasingExecutor`,
  parallelism = cores/2+1 or `Constants.MAVEN_MODEL_BUILDER_PARALLELISM`, :393-408; `loadFromRoot`/
  `loadFilePom` :869-966), then builds effective models per module in parallel (:816-843).
- Effective-model order (`buildEffectiveModel` :974-1032 and `readEffectiveModel` :1411-1516):
  file model → activateFileModel (external+POM profile activation on the *file* model for CI-friendly
  properties, :1372-1408) → parent chain (`readParent` :1034-1088; local reactor first for build requests,
  then external; **recursive parents are resolved eagerly, parents are cached per profile-activation
  context** — `readAsParentModel` caches `ParentModelWithProfiles` keyed by a recorded
  `DefaultProfileActivationContext.Record` of exactly which properties/profiles the activation consulted,
  replayed on hits, :1927-2018) → inheritance assembly → **mixins** (4.1+ feature; parent-like merges,
  :1459-1470) → normalization → per-model profile activation + injection (:1476-1490) → interpolation
  (:1493-1494) → URL normalization → repositories replaced (:1503-1513) → path translation → plugin mgmt
  injection → optional lifecycle bindings (skipped for `CONSUMER_DEPENDENCY`, :987-993) → **BOM import**
  (:2020-2061; supports **exclusions on the import dependency, MNG-5600**, :2122-2131, and warns on
  conflicting imports, MNG-8004, `DefaultDependencyManagementImporter.java:77-85`) → dep mgmt injection →
  default values → plugin config expansion → `ModelTransformer.transformEffectiveModel` hooks (:1008-1010)
  → effective validation.
- File-model reading (`doReadFileModel` :1546-1797) adds: model version inferred from xmlns (:1624-1629),
  early parent GAV completion from relativePath (:1634-1685), **subproject auto-discovery** for 4.1+ pom
  packaging (:1687-1710), **CI-friendly `${revision}` handling with profile-aware property resolution**
  (:1712-1743), repository URL interpolation up front (:1728-1735), string **interning** of hot fields
  (`InterningTransformer` :2574-2663), and a **`Features.mavenMaven3Personality` flag** that forbids
  model ≥4.1.0 to emulate Maven 3 exactly (:1782-1786).
- Validation levels survive internally (`ModelValidator.VALIDATION_LEVEL_STRICT|MINIMAL` chosen by
  request type, :1023-1027, :1906-1910).
- Interpolation is reimplemented without plexus-interpolation (`DefaultModelInterpolator.java`;
  `PROJECT_PREFIXES_3_1` keeps `pom.` for 4.0.0 POMs, drops it for 4.1+, :56-129;
  `DefaultInterpolator.java` is the generic engine).

### Caching (replaces 3.9 `ModelCache`)

Model-builder results go through `InternalSession.request(req, supplier)` with typed keys
(`SourceCacheKey{source,tag}` for file/raw/parent, `RgavCacheKey{repositories,g,a,v,classifier,tag}` for
imports — note **repositories are part of the import cache key**, fixing a classic 3.9 divergence class;
`DefaultModelBuilder.java:2218-2248, 2452-2556`). The cache itself is a **pluggable service**:
`api/maven-api-core/.../cache/RequestCache(Factory)` with `CacheRetention`
{PERSISTENT, SESSION_SCOPED, REQUEST_SCOPED, DISABLED} (`cache/CacheRetention.java:30-66`), default impl
`impl/maven-impl/.../cache/DefaultRequestCache.java` (+ per-request-type config via `CacheConfig`).
This is exactly the seam rewrite needs for its pluggable POM cache — implement one `RequestCacheFactory`.

### Standalone bootstrap — **yes, `ApiRunner` exists and maven-core is not needed**

`impl/maven-impl/src/main/java/org/apache/maven/impl/standalone/ApiRunner.java:100-144`:
`Injector.create()` (Maven's own tiny DI in `impl/maven-di`, no Guice/sisu/plexus),
`injector.discover(classloader)` finds all `@Named` impl services, `RepositorySystemSupplier` (same
package, 735 lines) assembles a full resolver `RepositorySystem` with basic connector. It creates a
`Session` with settings read via `SettingsBuilder`, central fallback, local repo wiring (:366-430).
Caveats for production embedding: it's test-oriented (`properties.put("user.home", "target")` hack
:376-377, mirrors/proxies marked TODO :414-416, `getMavenVersion()` returns null :219-221) — an embedder
would write its own ~200-line variant of `newSession` rather than use it verbatim.

**Artifacts a Maven-4 embedder needs** (from `impl/maven-impl/pom.xml:41-127`): `maven-api-*` (core, spi,
metadata, xml, toolchain, di, annotations, model, settings), `maven-impl`, `maven-di`, `maven-support`,
`maven-resolver-{api,spi,util,impl,named-locks,connector-basic}`, a transport
(`maven-resolver-transport-apache` or `-jdk`), `maven-xml`, woodstox/stax2, `plexus-sec-dispatcher`
(new codehaus 4.x lib, no plexus container), slf4j. **No maven-core, no plexus-utils container stack, no
Guice/sisu.**

Additionally Maven 4 ships the whole Maven 3 API surface as deprecated **compat** modules
(`compat/maven-model-builder`, `compat/maven-resolver-provider`, `compat/maven-settings-builder`, … ;
e.g. `compat/maven-model-builder/.../DefaultModelBuilder.java:99` is `@Deprecated(since="4.0.0")`) — a
3.9-based embedder has a documented migration path.

---

## 4. Provenance (what tells rewrite WHERE a value came from)

### Maven 3.9

- `InputSource{modelId, location}` + `InputLocation{line, column, source, nested locations map}` are
  generated into every model class by modello; populated only when
  `ModelBuildingRequest.isLocationTracking()` (`DefaultModelBuilder.java:587`,
  `DefaultArtifactDescriptorReader` does **not** enable it — an embedder must).
- Every field has `getLocation(fieldName)`; merged models keep the winning side's location
  (`MavenModelMerger`), so after inheritance/injection you can ask a managed dependency for
  `dep.getLocation("version").getSource().getModelId()` → the POM (g:a:v) that contributed it. This covers
  rewrite's "attribute managed versions/properties/dependencies to a POM" need at the granularity of
  *defining model*, including BOM-contributed entries (their locations point into the BOM file).
- **No `importedFrom`**: `grep -r importedFrom maven-3.9.x` → zero hits. For multi-level BOM chains you
  see the final defining POM but not the import path. Interpolated values keep the location of the
  *declaration site*, not of the property that supplied the value.

### Maven 4

- Same per-field `InputLocation`/`InputSource`, but immutable and **extended with `importedFrom`**
  (MNG-7982-family work): `src/mdo/java/InputLocation.java:42-56, 279-288` (field + `getImportedFrom()`),
  `InputSource` also carries an `importedFrom` location (`src/mdo/java/InputSource.java:44-63`).
- `DefaultDependencyManagementImporter.updateWithImportedFrom` (`impl/maven-impl/.../model/
  DefaultDependencyManagementImporter.java:154-189`) stamps every BOM-imported managed dependency with the
  location of the importing BOM, **chaining through transitive imports** — set only when
  `request.isLocationTracking()` (:86-89).
- Direct fit: rewrite's `ResolvedManagedDependency.requestedBom`/`bomGav`
  (`rewrite-maven/.../tree/ResolvedManagedDependency.java:42-48`) maps 1:1 onto `importedFrom`; 3.9 would
  require re-deriving the BOM chain manually (rewrite already does this itself today).
- Maven 4 also uses locations functionally (e.g. `hasSubprojectsDefined` checks
  `model.getLocation("subprojects")`, `DefaultModelBuilder.java:2314-2319`), so location tracking is
  first-class, not an afterthought.

---

## 5. Settings

### Maven 3.9 (`maven-settings-builder`)

- `DefaultSettingsBuilder.build()` — global (`${maven.home}/conf/settings.xml`) then user settings parsed
  (strict→lenient retry), validated, merged (`MavenSettingsMerger`, user wins, tracking
  `TrackableBase.GLOBAL_LEVEL`), then **interpolated by serializing the whole settings to XML, regex-
  interpolating the string (user props → system props → env with `env.` prefix), and re-parsing**
  (`DefaultSettingsBuilder.java:188-236` — crude but authoritative); drive-relative Windows localRepository
  absolutized (:105-112).
- Plexus-free factory exists: `building/DefaultSettingsBuilderFactory.java`.
- **Decryption**: `crypto/DefaultSettingsDecrypter.java:43-113` decrypts server passwords/passphrases and
  proxy passwords via `org.sonatype.plexus.components.sec.dispatcher.SecDispatcher` (the old
  plexus-sec-dispatcher + plexus-cipher, `~/.m2/settings-security.xml`). Decryption is *on-demand*, not
  part of settings building.
- **Profile → model conversion** (settings profiles injected as external profiles):
  `maven-core/.../settings/SettingsUtils.convertFromSettingsProfile` (maven-core, not settings-builder).
- **Mirror matching** is NOT in settings-builder: authoritative logic in
  `maven-core/src/main/java/org/apache/maven/bridge/MavenRepositorySystem.java:694-860` and mirrored in
  resolver's `DefaultMirrorSelector`: exact id, `*`, `external:*` (not localhost/file), `external:http:*`
  (3.8.0+), `!repo` negations, comma lists, plus layout matching (:816-860). Proxy selection at resolver
  level (`DefaultProxySelector` w/ nonProxyHosts) wired from settings by maven-core's session factory.
- **Verdict for rewrite**: settings are consumed by model building only as (a) external profiles,
  (b) active/inactive profile ids, (c) user properties; mirrors/proxies/servers matter only to the
  *resolver session*. Rewrite can keep `MavenSettings` and translate: profiles →
  `request.setProfiles(...)`, mirrors/auth/proxies → aether session (`MirrorSelector`,
  `AuthenticationSelector`, `ProxySelector`). No need to adopt maven-settings-builder at all, though it is
  trivially embeddable if wanted.

### Maven 4

- API: `api/maven-api-core/.../services/SettingsBuilder(Request)` — **three levels: installation, project
  (`.mvn/settings.xml`), user** (`SettingsBuilderRequest.java:76-108`).
- Impl `impl/maven-impl/.../DefaultSettingsBuilder.java` (383 lines): parse→interpolate→**decrypt inline**
  (:204, :258-295) using the new `org.codehaus.plexus:plexus-sec-dispatcher` 4.x (standalone lib,
  `settings-security4.xml`, pluggable `Dispatcher`s; legacy master-password format handled
  best-effort with warnings) → validate. `SettingsUtilsV4` converts settings profiles ↔ model profiles.
- Mirror/proxy application again lives at session build time (`MavenSessionBuilderSupplier` /
  maven-core `DefaultRepositorySystemSessionFactory`); standalone `ApiRunner` currently skips it (TODO at
  `ApiRunner.java:414-416`) — rewrite would keep its own translation exactly as with 3.9.

---

## 6. Effective-model output vs rewrite `ResolvedPom` needs

rewrite `ResolvedPom` fields (`rewrite-maven/.../tree/ResolvedPom.java`, field block at :60-142):
`properties`, `dependencyManagement: List<ResolvedManagedDependency>`, `initialRepositories`,
`repositories`, `pluginRepositories`, `requestedDependencies`, `plugins`, `pluginManagement`,
`subprojects`, `activeProfiles`.

| rewrite need | 3.9 effective `Model` | Maven 4 effective `Model` |
|---|---|---|
| properties map | `getProperties(): Properties` (interpolated, profile+parent merged) | `getProperties(): Map<String,String>` (immutable) |
| managed deps w/ exclusions | `getDependencyManagement().getDependencies()` — BOMs flattened, exclusions per dep; **import exclusions ignored** | same, plus MNG-5600 import exclusions + `importedFrom` provenance |
| requested (declared) deps | `getDependencies()` (versions managed only after `dependencyManagementInjector`; raw model available in result for "as-declared") | same; `getRawModel()`/`getFileModel()` both retained in result |
| repositories / pluginRepositories | `getRepositories()` / `getPluginRepositories()` (interpolated, profile-injected; super-POM central included) | same; repo URL interpolation done even earlier (file-model stage) |
| plugins + configuration | `getBuild().getPlugins()` / `getPluginManagement()`; config = `Xpp3Dom` | same; config = immutable `XmlNode` (`api/maven-api-xml`) |
| licenses | `getLicenses()` | same |
| packaging | `getPackaging()` | same (+`Type.POM` handling) |
| modules/subprojects | `getModules()` | `getSubprojects()` + deprecated `getModules()` (mdo `maven.mdo:544-570`); auto-discovery for 4.1 POMs |
| active profiles per pom | `ModelBuildingResult.getActivePomProfiles(modelId)` (`ModelBuildingResult.java:78`) | `ModelBuilderResult.getActivePomProfilesByModel()` (`ModelBuilderResult.java:94-104`) |
| parent chain | `getModelIds()` + `getRawModel(id)` (lineage ids in order) | `getParentModel()` + per-model results |

Both give everything `ResolvedPom` carries; the deltas are representational (mutable clones + `Properties`
vs immutable records + `Map<String,String>`; `Xpp3Dom` vs `XmlNode` — rewrite converts plugin config to
Jackson `JsonNode` either way). The one structural mismatch either way: rewrite keeps
*uninterpolated* `requested` values alongside resolved ones — both stacks keep raw models in the result, so
this is coverable.

---

## 7. Verdict

**Target Maven 4 `maven-impl` + `maven-api-*` as the primary embedding target; treat 3.9 only as a
stopgap.** Reasons:

1. **Embeddability**: maven-impl is containerless by construction (`maven-di` + `@Named` discovery;
   `ApiRunner` proves the wiring in ~400 lines; deps list in §3 has no maven-core/plexus/sisu/guice).
   3.9 is *also* embeddable via `DefaultModelBuilderFactory`, but its behavior-critical surroundings
   (mirror matching, settings profile conversion, session factory) live in maven-core, forcing selective
   reimplementation — the same trap that produced rewrite's current divergence.
2. **Fidelity**: the Maven 4 `CONSUMER_DEPENDENCY` request type is the *exact* code path Maven 4 itself
   uses for dependency POMs (`impl/.../resolver/DefaultArtifactDescriptorReader.java:199-220`), including
   the MNG-7563 property rules, per-repository import caching, and Maven-3-personality guardrails. Model
   4.0.0 POMs remain first-class (super-POM per model version, `pom.` prefix kept for 4.0.0,
   `Features.mavenMaven3Personality`).
3. **Provenance**: only Maven 4 has `importedFrom` chains; both have per-field `InputLocation` with
   defining-model ids (enable `locationTracking`). rewrite's `ResolvedManagedDependency.bomGav` maps
   directly.
4. **Caching**: Maven 4's `RequestCacheFactory` + `CacheRetention` is a real SPI made for exactly
   rewrite's pluggable-cache requirement, with typed keys that include the repository list; 3.9's
   `ModelCache` is a flat g:a:v:tag map that deep-clones values on every hit (`ModelCacheTag`) — workable
   but slower and easy to key incorrectly.
5. **Support horizon**: 3.9.x is in bugfix-only maintenance (releases through `maven-3.9.16`; HEAD is a
   dependabot CI bump; the module tree is frozen pre-split layout). Maven 4 is the active line (master =
   `4.1.0-SNAPSHOT`, `maven-4.0.x` release branch at rc-5, tags `maven-4.0.0-rc-1..rc-5`, HEAD is same-day
   feature/test work). All 3.x APIs already ship as `@Deprecated` compat wrappers inside Maven 4
   (`compat/`), i.e., the 3.9 API is scheduled for the exit.
   **Counterweight**: Maven 4 has not shipped a 4.0.0 GA as of 2026-07-07 (rc-5), the new API is annotated
   `@Experimental`, and `maven-impl` internals (e.g. `ModelBuilderSessionState`) are not API — pin
   versions and isolate behind a rewrite-side facade. If GA slippage is unacceptable, the fallback is
   3.9's `DefaultModelBuilderFactory` + `DefaultArtifactDescriptorReader` with a custom
   `ArtifactDescriptorReaderDelegate`, which is stable but locks in 3.9 semantics (no import exclusions,
   no importedFrom, first-wins BOM conflicts silently).

Recommended embedding seams (Maven 4): implement `RequestCacheFactory` (POM cache),
`ModelResolver`/`ModelProcessor` only if rewrite needs custom source loading (probably not — use
`Sources.resolvedSource`), drive everything through `ModelBuilder.newSession()` with
`RequestType.CONSUMER_DEPENDENCY`/`BUILD_EFFECTIVE`, `locationTracking=true`, and translate
`MavenSettings` → external profiles + resolver-session mirrors/proxies/auth exactly as today.
