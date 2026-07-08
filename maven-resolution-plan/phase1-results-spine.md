# Phase 1 (spine) results — `rewrite-maven-engine`

2026-07-08. The shaded engine module + bootstrap facade + production `HttpSenderTransporter` landed in the
`agent-a41d14b0f5b3d6f2c` worktree. `gradlew :rewrite-maven-engine:build` is green: 16 tests pass, `licenseFormat`
clean, javadoc clean, Java-8 bytecode floor holds, published POM exposes zero unrelocated Maven/aether packages.

Scope built = DESIGN §8 Phase-1 row **minus** the later slices (`SettingsBridge`, `ReactorWorkspace`, `CacheBridge`,
`EngineLocalRepositoryManager`): the module + shading, `MavenEngine` session template, `HttpSenderTransporter(Factory)`,
the scratch LRM (via `withLocalRepositoryBaseDirectories`), and the guarantee tests.

## What was built

`rewrite-maven-engine/` (registered in `settings.gradle.kts`), package `org.openrewrite.maven.engine`:

| Class | Role |
|---|---|
| `MavenEngine` | Bootstrap facade (`Closeable`): one plain-`new` `RepositorySystem`, shared `RepositoryCache`, `EngineOptions`, session template mirroring Maven 3.9's factory. |
| `EngineRepositorySystemSupplier` | `RepositorySystemSupplier` subclass: transports = file + `HttpSenderTransporterFactory` only; `createRemoteRepositoryFilterSources()` → empty (RRF off). |
| `HttpSenderTransporter` | Sole network transport. `classify()`, timeout-only Failsafe retry, auth→anonymous fallback, per-server headers/timeouts, run-scoped unreachable set, PEEK→HEAD, PUT→unsupported, `ResolutionTimeRecorder` hook. |
| `HttpSenderTransporterFactory` | Per-`newInstance` resolution of sender + credentials (via `AuthenticationContext`) + headers/timeouts (via `ConfigUtils`/`ConfigurationProperties`) + unreachable set + recorder. Priority 100 (beats stock Apache 5.0). |
| `EngineOptions` | Engine-wide options; `gradleMetadataInjection` flag present but **inert**. |
| `SessionConfig` | Per-session inputs (sender, unreachable set, recorder) stamped onto session config properties. |
| `ResolutionTimeRecorder` | Functional interface = the `recordResolutionTime` seam; host binds it to `MavenExecutionContextView`. |

## Shading decisions

- **Plugin.** DESIGN/brief said "use `com.gradleup.shadow`". The repo's `org.openrewrite.build.shadow` convention
  **already wraps `com.gradleup.shadow` 9.0.0-beta7** (confirmed in the build-plugin POM; rewrite-java uses it). So I
  applied the convention — this *is* gradleup shadow, and applying a second raw `id("com.gradleup.shadow")` would
  collide (double `ShadowJar` registration). **Resolution: convention only.** Mirrors rewrite-java exactly
  (`import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar`).
- **Convention set.** `org.openrewrite.build.language-library` (java compile @ release 8 via `options.release.set(8)`,
  license, publish, metadata) + `shadow`. The recipe-marketplace tasks it adds are benign no-ops for a recipe-free
  module — `recipeCsvValidate` passes, matching the `rewrite-gradle-tooling-model` precedent.
- **What is bundled.** Only a dedicated `mavenStack` configuration (which `compileOnly`/`testImplementation` extend, so
  it never reaches the published POM) — the rewrite-java checkstyle pattern. `rewrite-core` stays a normal `api` project
  dep. **Verified:** consumer `runtimeClasspath` = `rewrite-core` + `failsafe` + `slf4j-api` only; the whole Maven
  stack is absent from the POM.
- **Excluded from the shade entirely:** `maven-resolver-transport-apache`, `httpclient`, `httpcore`, `commons-codec`
  (HttpSender is the sole transport); `slf4j-api` (logging must bind to the HOST's slf4j, so it is a normal
  `implementation` POM dependency instead — pinned `2.0.+` → 2.0.18, since `latest.release` pulls a 2.1 alpha);
  `error_prone_annotations` (annotation-only CLASS-retention metadata gson drags in, not needed at runtime).
  All verified absent from both the resolved classpath and the jar.
- **Stripped:** `META-INF/sisu/**` (no DI container — plain-`new` bootstrap), `META-INF/maven/**`, signature files,
  `**/module-info.class` (root and multi-release — they name unrelocated modules and are inert here).
  OSGi bundle headers vanish because the shadow jar carries the project manifest, not per-jar manifests.
- **Aggregated:** `mergeServiceFiles()` (contents rewritten to relocated names) + `append` of `NOTICE(.txt)`/`LICENSE(.txt)`.
- **`dev.failsafe` duplicated** as an engine `implementation` dep (not shaded) to match `MavenPomDownloader.retryPolicy`
  byte-for-byte.

### Final relocation list → `org.openrewrite.maven.engine.shaded.*`

| Relocated prefix | Note |
|---|---|
| `org.eclipse.aether` | whole resolver 2.0.20 stack (709 classes) |
| `org.apache.maven` | **whole tree** — superset of DESIGN's enumerated `model(.building)`/`artifact`/`repository.internal`; actual 3.9.16 resolution also drags `org.apache.maven.{repository, repository.legacy, utils}` (from maven-artifact + maven-model-builder) and `org.apache.maven.artifact.repository.metadata` (repository-metadata). Parent relocation is complete + future-proof. |
| `org.codehaus.plexus.util` | plexus-utils 3.6.1 |
| `org.codehaus.plexus.interpolation` | plexus-interpolation 1.29 |
| `com.google.gson` | gson 2.14.0 (via resolver-spi) — ubiquitous on plugin classpaths |
| `org.objectweb.asm` | asm 9.9.1 (via maven-model-builder) — Gradle hosts ship their own asm |

**The jar is held to an empty allowlist**, not a deny-list of known packages: `RelocationJarScanTest` asserts every
class in the jar — including multi-release overlays under `META-INF/versions/**` — lives under `org/openrewrite/**`,
so a dependency bump can never silently reintroduce an unrelocated package. It additionally asserts the excluded http
transport and slf4j are absent *even in relocated form*, and that `META-INF/sisu` is stripped.

## Session-template knob table (knob → Maven 3.9 `DefaultRepositorySystemSessionFactory` reference)

Base defaults (ClassicDependencyManager, verbose-capable `ConflictResolver` chain, `ArtifactTypeRegistry`,
`DependencySelector`) are inherited unchanged from `SessionBuilderSupplier`, which reproduces
`MavenRepositorySystemUtils.newSession()`. Explicit knobs:

| Knob | Value | Mirrors |
|---|---|---|
| `setCache` | shared `DefaultRepositoryCache` | factory L160 `session.setCache(request.getRepositoryCache())` |
| `USER_AGENT` config prop | engine UA string | factory L163 / `getUserAgent()` |
| `withLocalRepositoryBaseDirectories` | per-run scratch dir | factory `setUpLocalRepositoryManager` + DESIGN §0 (never `~/.m2`) |
| `setChecksumPolicy` | `CHECKSUM_POLICY_IGNORE` | factory L173 (value per DESIGN §5.3: rewrite never validated) |
| `setUpdatePolicy` | `null` | factory L179 (neither `-nsu` nor `-U`) |
| `setResolutionErrorPolicy` | `SimpleResolutionErrorPolicy(0, CACHE_NOT_FOUND)` | factory L182-190 with default request flags |
| `setIgnoreArtifactDescriptorRepositories` | `false` | factory L353 (descriptor repos aggregated — DESIGN §4.2) |
| `setArtifactDescriptorPolicy` | `SimpleArtifactDescriptorPolicy(true, true)` | `newSession()` L124 (missing/invalid tolerance) |
| Proxy selector | **not set** | DESIGN §0 (sender owns transport end-to-end) |
| Auth/mirror selectors | **deferred to `SettingsBridge`** | factory L211-327 — later slice; spine tests set auth on the `RemoteRepository`/transporter directly |
| Per-session sender/unreachable/recorder | config props | owned seam (not a factory knob) |

## Tests (16, all green; JUnit 5 + MockWebServer, hermetic)

| Class | # | Covers |
|---|---|---|
| `MavenEngineBootstrapTest` | 3 | bootstrap+collect through shaded stack; no-bypass (server count == Σ recorders) + per-session sender isolation; shared-cache warm-vs-cold |
| `RemoteRepositoryFilteringOffTest` | 1 | zero `prefixes.txt` requests (RRF off) |
| `HttpSenderTransporterClassifyTest` | 5 | exhaustive `classify()` / `isDeterministicClientError` over 0–599 |
| `HttpSenderTransporterAuthFallbackTest` | 5 | auth→anon success; both-denied → original 401; anon 500 → retry wins; no-creds no-fallback |
| `RelocationJarScanTest` | 1 | jar-scan: empty allowlist (every class under `org/openrewrite/**`, MR overlays included), no excluded-transport/slf4j leak even relocated, sisu stripped |
| `Java8BytecodeFloorTest` | 1 | every class in the shaded jar ≤ major 52 |

## Deviations from DESIGN §2 (each justified above)

1. **Whole-`org.apache.maven` relocation** instead of the enumerated sub-packages — required to catch
   `repository`/`repository.legacy`/`utils` stragglers the real resolution pulls; documented superset.
2. **Shadow via the repo convention**, not a raw `com.gradleup.shadow` apply — the convention already *is* gradleup
   shadow; a second apply would collide. Satisfies the requirement without breakage.
3. **`classify()` hardened** beyond the spike's 404/410 to all deterministic 4xx-except-408/425/429 → `NOT_FOUND`, per
   the DESIGN §2 class-table spec and matching `MavenPomDownloader.isClientSideException` (drives negative caching).
4. **`recordResolutionTime` as a `ResolutionTimeRecorder` seam** (not a direct `MavenExecutionContextView` call) — the
   engine depends on rewrite-core only; the host binds the hook.
5. **Relocation set widened beyond DESIGN's enumeration** to gson + asm (collision hazard on plugin classpaths), with
   slf4j-api excluded/host-bound and error_prone_annotations dropped — enforced by the empty-allowlist jar scan.

No existing module was modified except the one-line `settings.gradle.kts` registration.
