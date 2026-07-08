# Phase 2 slice B1 — engine-side model-building service

2026-07-08, worktree `maven-resolution` (uncommitted). New classes in
`org.openrewrite.maven.internal.engine` (rewrite-maven), composed on the A2 seams (`SettingsBridge`,
`EffectiveSettings`, `ReactorWorkspace`) and the shaded engine module (`MavenEngine`, `SessionConfig`,
`HttpSenderTransporter`, `EngineRepositorySystemSupplier`). Scoped build green:
`:rewrite-maven:compileJava`, `:rewrite-maven:test --tests …engine.*` (28 tests, 0 failures),
`:rewrite-maven:licenseMain/licenseTest`.

## Supply-chain architecture

```
 EngineEffectivePom.build(pomXml, requested, EffectiveSettings, ReactorWorkspace, ctx)
   │  DefaultModelBuilder (EngineModelBuilderFactory: no-op PluginManagementInjector)
   │    request: MINIMAL / processPlugins=false / locationTracking=true / twoPhase=false
   ├── setModelSource ........ StringModelSource(pomXml)              ← XML-first root
   ├── setWorkspaceModelResolver → ReactorWorkspace (reactor parents/BOMs, epoch-cleared)
   ├── setModelResolver ......... EngineModelResolver (recessive addRepository, ranges)
   │        └── CacheBridge.resolvePom(g,a,v,repos)
   │              MavenPomCache.getPomBytes  ──hit──►  materialize file → FileModelSource
   │                 │ known-absent → replay not-found (no I/O)
   │                 └ miss → RepositorySystem.resolveArtifact(.pom)   ← scratch LRM
   │                            └ HttpSenderTransporter → HttpSender   (4xx/transfer classify)
   │                          success → putPomBytes + RawPom.parse→putPom + servedBy[gav]=repo
   │                          not-found(ArtifactNotFound) → putPomBytes(null)  (negative)
   │                          transfer error → record response, no cache
   └── setModelCache ......... EngineModelCache (shared session RepositoryCache; skips reactor GAVs)
        result → EngineModelBuildingOutcome{ ModelBuildingResult, servedBy }
        ModelBuildingException → ModelParityErrorMapper → { MavenDownloadingException | MavenParsingException }
```

## Request configuration vs DESIGN §4.1

| §4.1 knob | Set | Value |
|---|---|---|
| validationLevel MINIMAL | ✓ | `VALIDATION_LEVEL_MINIMAL` |
| processPlugins=false | ✓ | `setProcessPlugins(false)` |
| locationTracking=true | ✓ | `setLocationTracking(true)` |
| no-op PluginManagementInjector | ✓ | `EngineModelBuilderFactory.newPluginManagementInjector()` → lambda no-op |
| external profiles + active ids | ✓ | `setProfiles(settings.externalProfiles)` / `setActiveProfileIds(settings.activeProfiles)` |
| user properties (parser + ctx) | ✓ | `setUserProperties(settings.userProperties)` |
| WorkspaceModelResolver = ReactorWorkspace | ✓ | `setWorkspaceModelResolver(reactor)` |
| ModelCache from shared RepositoryCache | ✓ | `EngineModelCache(session.getCache(), session, reactor)` |
| (added) twoPhaseBuilding=false | ✓ | effective model in one `build()` (matches spike) |
| (added) systemProperties = System props copy | ✓ | os/jdk profile activation parity; Maven precedence keeps POM props dominant (DESIGN §9) |

## Error shapes vs parent-cycle spike

| Spike case | Maven failure | Mapped to | Asserted essence |
|---|---|---|---|
| E1/E2 parent cycle A→B→A | FATAL "The parents form a cycle: …" | `MavenParsingException` | message contains `cycle` |
| E5a self-parent A.parent=A | FATAL raw validation "cannot have the same groupId:artifactId as the project" | `MavenParsingException` | message contains `groupId:artifactId`, 0 network |
| unresolvable parent/BOM | `UnresolvableModelException` in a ModelProblem | `MavenDownloadingException` (failedOn = parent GAV, per-repo responses) | instanceof, 0 network on known-absent |

Split matches the legacy engine: `downloader.download` failures → `MavenDownloadingException`;
`resolveParentPom` validation (missing/constant version) → `MavenParsingException`. **No cycle-breaking
leniency** (DESIGN §0): cycles/self-parents surface Maven-identically; the spike's E3b MUTATE_ID resolver is
deliberately not built.

## Tests (28, hermetic, no live network)

`EngineEffectivePomTest` (7): (a) parent chain + single/multi-level BOM import over MockWebServer through the
real transport — inheritance, DM entries, `InputLocation` modelIds (parent-declared vs BOM-declared),
gav→repo attribution, 5 pom GETs; (b) warm bytes region — second build on a **fresh engine** + warm
`MavenPomCache` = 0 network; (c) known-absent short-circuit = 0 network → `MavenDownloadingException`;
(d) parent cycle + self-parent → `MavenParsingException` (essence asserted); (e) reactor parent beats remote +
mutate-then-epoch-bump staleness guard (stale without bump, fresh after bump); (f) `${revision}` parent
through the workspace. Plus A2 `ReactorWorkspaceTest` (7) + `SettingsBridgeTest` (14) still green.

## Deviations (justified)

1. **EngineModelCache does not cache reactor GAVs** (rather than epoch-keying them). Maven 3.9 `readParent`,
   on a RAW cache hit whose model carries a `pomFile` — which a workspace parent must, to be read as a
   `FileModelSource` — cross-checks that file against the child's `relativePath`; for an in-memory
   (StringModelSource) root that resolves to none, it distrusts the entry and re-resolves against the
   repositories (empirically: reactor parent silently replaced by the remote one). Skipping reactor GAVs keeps
   every reactor parent flowing through the workspace; the staleness guard is `ReactorWorkspace`'s own model
   cache, cleared on `bumpEpoch()`. Same end state as DESIGN §5.5, cleaner mechanism. Non-reactor GAVs cache
   normally in the shared session cache.
2. **ReactorWorkspace change (2 lines of behavior):** `readModel` now materializes the printed bytes to a temp
   file and `setPomFile`s it (Maven's `readParentLocally` builds `FileModelSource(model.getPomFile())` and NPEs
   on null), and hands out `model.clone()` per call (Maven mutates the raw model it receives; the pristine
   cached copy must be protected or a re-resolution falls through to the repository). Added a public
   `isReactorMember(g,a,v)` accessor for the model cache. A2's contract and its 7 tests are unchanged/green.
   This is the integration gap A2's isolated tests never exercised (they never drove the real ModelBuilder).
3. **systemProperties = System.getProperties() copy** — not listed in §4.1 but required for `<os>`/`<jdk>`
   profile-activation parity (DESIGN §9); Maven's interpolation precedence keeps POM properties dominant, so
   this does not reintroduce the removed "system-property override of POM properties" bug.
4. **Repository iteration in CacheBridge** mirrors `MavenPomDownloader.download`: per-repository
   `ResolvedGroupArtifactVersion(repo.uri,…)` keys, single-repo `resolveArtifact` per iteration (precise
   gav→repo attribution + per-repo negative caching) rather than handing aether a multi-repo request.
5. **`gav→repository` attribution is resolution-time only** — a warm bytes-region hit does not re-derive the
   serving repository (the bytes region is bytes-only per §5.1). B2's BomGavAttributor derives BOM identity
   from `InputLocation`, not this map; the map feeds `ResolvedDependency.repository`/`ResolvedPom` stamping,
   which is a cold-resolution fact.

## API surface B2 consumes

- `EngineEffectivePom(RepositorySystem system, RepositorySystemSession session, List<MavenRepository>
  requestRepositories, Path materializeDir)` — construct once per run.
- `EngineModelBuildingOutcome build(byte[] requestedPomXml, Pom requested, EffectiveSettings, ReactorWorkspace,
  ExecutionContext)` — per project pom. `requestRepositories` is the settings/ctx/central base list; a pom's
  own `<repositories>` are aggregated into the resolver by Maven itself (recessive `addRepository`).
- `EngineModelBuildingOutcome` (`@Value`): `isSuccess()`, `@Nullable ModelBuildingResult getResult()`
  (effective model + `getModelIds()`/`getRawModel(id)` lineage + `getActivePomProfiles(id)`), `@Nullable
  Throwable getFailure()` (`MavenDownloadingException` | `MavenParsingException` — instanceof to route),
  `Map<ResolvedGroupArtifactVersion, MavenRepository> getServedBy()`.
- Supporting (usually not touched directly by B2): `CacheBridge` (constructed internally by `build` from
  `ctx.getPomCache()`), `EngineModelResolver`, `EngineModelCache`, `EngineModelBuilderFactory`,
  `ModelParityErrorMapper.map(ModelBuildingException, CacheBridge)`.
