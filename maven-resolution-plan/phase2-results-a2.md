# Phase 2 slice A2 results — `SettingsBridge` + `ReactorWorkspace`

2026-07-08, worktree `phase2-seams` (branch `phase2-seams`, uncommitted). New internal package
`org.openrewrite.maven.internal.engine` in **rewrite-maven** (adapters need `MavenSettings`/`tree.*`/
`MavenExecutionContextView`, so they live here and reference the engine's relocated
`org.openrewrite.maven.engine.shaded.*` types). Scoped build green:
`:rewrite-maven:compileJava`, `:rewrite-maven:test --tests …engine.SettingsBridgeTest/ReactorWorkspaceTest`
(21 tests, 0 failures), `:rewrite-maven-engine:build`, `:rewrite-maven:license`.

## Dependency-wiring decision (how rewrite-maven consumes the shaded module)

`RewriteShadowPlugin` (javap-confirmed) disables the thin `jar` task and sets the `shadowJar` classifier to `null`,
so the shadow jar is the module's primary artifact. **But** a plain `implementation(project(":rewrite-maven-engine"))`
resolves the engine's `apiElements`/`runtimeElements` **classes-directory secondary variant**
(`build/classes/java/main`) — which holds only the engine's own `.class` files (referencing the **un-relocated**
`org.eclipse.aether.*`) and **none** of the relocated `…engine.shaded.*` types the adapters compile against.

Resolution: consume the shadow variant explicitly —
```kotlin
implementation(project(path = ":rewrite-maven-engine", configuration = "shadowRuntimeElements"))
```
`shadowRuntimeElements` (`bundling=shadowed`, jar-only, **no** classes-directory secondary variant) puts the fat jar,
with the relocated stack inside, on rewrite-maven's compile **and** runtime classpath.

One required fix on the engine module (`rewrite-maven-engine/build.gradle.kts`, committed Phase-1 file):
`shadowRuntimeElements` inherited the toolchain's `org.gradle.jvm.version=21` while `apiElements` correctly advertises
`8` and the jar is Java-8 bytecode (`Java8BytecodeFloorTest`). A Java-8 consumer (rewrite-maven) therefore failed
attribute matching. Added:
```kotlin
configurations.named("shadowRuntimeElements").configure {
    attributes { attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8) }
}
```
This declares the variant's true level (matching `apiElements`) — correct, not a workaround. It is the only change to
a Phase-1 file; no engine source or test touched, engine build still green.

## What was built

- **`SettingsBridge`** — pure translation, no I/O. Constructed from `(MavenExecutionContextView, @Nullable MavenSettings,
  @Nullable List<String> activeProfiles)`; derives `mirrors = ctx.getMirrors(settings)`,
  `credentials = ctx.getCredentials(settings)` exactly as the downloader constructor does.
  - `(a) requestRepositories(pomRepos, addLocal, addCentral, properties)` — order local → settings/ctx → pom → central,
    id-keyed later-wins, then per-repo URI interpolation (`ResolvedPom.placeholderHelper`) → `MavenRepositoryMirror.apply`
    → `MavenRepositoryCredentials.apply`, then de-dup by resulting id (first-wins). Mirrors
    `distinctNormalizedRepositories` + the settings-presence seam trap in `getRepositories` verbatim (I/O-only steps —
    file-URI normalization, https probe, `knownToExist` — are intentionally omitted; they belong to the transport layer).
  - `(b) mirrorSelector()` — a `MirrorSelector` delegating to the **same** `MavenRepositoryMirror.apply`, converting
    aether `RemoteRepository` ↔ `MavenRepository`, returning `null` on no-match. Agrees with (a) by construction.
    `authenticationSelector()` — `DefaultAuthenticationSelector` populated from the (already-decrypted) settings
    servers + ctx credentials, keyed by id.
  - `(c) serverConfigProperties()` — per-server `aether.transport.http.headers.<id>` (map),
    `…connectTimeout.<id>` / `…requestTimeout.<id>` (ms ints, both from `<server><configuration><timeout>`), keyed with
    the shaded `ConfigurationProperties` constants — the exact keys `HttpSenderTransporterFactory` reads.
  - `(d) effectiveSettings(userProperties)` → `EffectiveSettings{externalProfiles, activeProfiles, userProperties}`;
    settings profiles converted to shaded `Model` `Profile`s (id, activation, repositories — settings profiles carry no
    `<properties>` in rewrite's model), active ids = settings `<activeProfiles>` + passed ids. **No ProxySelector**
    (DESIGN §0).
  - static `addLocalRepository/addCentralRepository(ctx)` = `!FALSE.equals(...)` (parse-context default).
- **`EffectiveSettings`** — `@Value` holder of the three `ModelBuildingRequest` inputs (slice B consumes it).
- **`ReactorWorkspace`** — implements shaded `MavenWorkspaceReader` (`WorkspaceReader` + `findModel`) and
  `WorkspaceModelResolver`. Three-tier match ported from `MavenPomDownloader.download` (exact resolved GAV → raw g:a +
  raw version → property-merged version = the `${revision}` raw-GAV-before-interpolation behavior), including the
  ported `projectPomsByGav`/`mergeProperties`/`getAncestryWithinProject`/`getParentWithinProject`. Serves raw
  `Model`s parsed from printed XML bytes via `Function<Path, byte[]>` and `DefaultModelReader`. `<relativePath>`
  resolution is **not** implemented (Maven owns it now; only in-reactor poms match, so a `.m2`-relative parent is never
  a reactor member). Monotonic `epoch()` + `bumpEpoch()` (clears the model cache; epoch is folded into the
  `WorkspaceRepository` key so aether never serves a stale printed model). `PomToModelConverter` seam: a `null` byte
  source yields a `null` model — slice B fills it.

## Semantics decisions (flag any you want changed)

1. **`resolveEffectiveModel` returns `null`.** The workspace serves *raw* models only; effective-model building is the
   model builder's job. A reactor module imported as a BOM therefore defers to slice B rather than getting a
   half-built model here. If you want reactor-BOM effective resolution wired now, that is a slice-B decision.
2. **`addLocal/addCentral` default = parse-context (`!FALSE.equals`).** `SettingsBridge` targets the Maven-parsing path
   (project-poms constructor, default true). The Gradle-side `MavenPomDownloader(ctx)` constructor uses the opposite
   default (`TRUE.equals`, default false); when the engine serves that path, the caller passes the already-resolved
   boolean — the assembly is boolean-driven, so both are expressible.
3. **URI interpolation source.** (a) interpolates repo URIs against the supplied user-property map (settings-level),
   not a containing pom — the model builder handles pom-property interpolation of descriptor-declared repos during
   model building.

## Tests (21, hermetic, no network)

`SettingsBridgeTest` (14): repo ordering; addLocal/addCentral tri-state matrix (4) + default resolution;
ctx-repos-survive-without-settings vs the settings-presence trap (drop unrelated ctx repo, enrich same-id);
URI interpolation; mirror applied but never to local; **mirror agreement** across a `central`/`external:*`/localhost/
`file:` matrix ((a) `MavenRepositoryMirror.apply` vs (b) `mirrorSelector().getMirror`); credential path (decrypted
server creds → request-list `MavenRepository` **and** the session `AuthenticationSelector`, extracted via
`AuthenticationContext`); per-server config properties; `EffectiveSettings` external profiles/active ids/user props.
`ReactorWorkspaceTest` (7): exact-GAV; `${revision}` raw+interpolated; child-`${revision}`-from-in-project-parent
ancestry; `findModel`/`findVersions`; unknown→null; synthetic (no-XML)→null model; epoch monotonic + model-cache
invalidation (mutate printed bytes, stale until `bumpEpoch`).
