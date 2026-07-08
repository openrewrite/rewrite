# Spike results — Maven Resolver 2.x embeddability (claims 1–3)

Empirical verification that `maven-resolver-supplier-mvn3` can back rewrite-maven's resolution. Six JUnit tests,
all green via:

```
/Users/jon/Projects/github/openrewrite/rewrite/.claude/worktrees/maven-resolution/gradlew \
  -p .../maven-resolution-plan/spike/transport clean test
```

Final run (tail):

```
> Task :test
T1_BootstrapCollectTest > bootstrapWithPlainNewAndCollectGraph(Path) PASSED
T2_HttpInjectionTest > httpUrlConnectionSenderIssuesRealHeadOnTheWire() PASSED
T2_HttpInjectionTest > trafficRoutesThroughPerSessionSender_noBypass(Path, Path) PASSED
T2_HttpInjectionTest > peekMapsToHead_andClassifyDistinguishes404From500() PASSED
T3_DescriptorDecorationTest > cachedDescriptorReaderServesSecondSessionWithZeroNetwork(Path, Path) PASSED
T4_BytecodeVersionTest > everyRuntimeJarTargetsJava8OrLower() STANDARD_OUT
    [T4] scanned 55 jars / 10457 base classes; max classfile major = 52 (Java 8 == 52)
T4_BytecodeVersionTest > everyRuntimeJarTargetsJava8OrLower() PASSED
BUILD SUCCESSFUL
```

## Verdicts

| Claim | Verdict | Test(s) |
|---|---|---|
| 1. Plain-`new` bootstrap + collect, all code at `release=8` | **PROVEN** | `T1`, and `compileJava` runs with `options.release=8` |
| 1b. Every runtime jar ≤ Java 8 bytecode (major ≤ 52) | **PROVEN** | `T4` |
| 2. All remote traffic through an injected `HttpSender`, per-session | **PROVEN** | `T2` (3 tests) |
| 3. Decorate `ArtifactDescriptorReader` → cache short-circuits reads to zero network | **PROVEN** | `T3` |

### Claim 1 — PROVEN
`new SpikeRepositorySystemSupplier(false).get()` (extends the stock `org.eclipse.aether.supplier.RepositorySystemSupplier`)
yields a working `RepositorySystem` with zero DI container. `T1` collects `com.example:app:1 -> lib-a:1 -> lib-b:1`
against a MockWebServer repo and asserts the exact linear graph shape. It also asserts collection requests **POMs only,
never `.jar`** (3 `.pom` GETs). All spike code compiles at `options.release = 8` (Gradle toolchain 17, release flag 8).

### Claim 1b — PROVEN
`T4` reads `configurations.runtimeClasspath` (handed in as a system property), opens every jar, and checks each
classfile's major version, skipping `module-info.class` and `META-INF/versions/**` (multi-release overlays, inert on
Java 8). Result: **55 jars, 10457 base classes, max major = exactly 52**. The whole resolver+maven stack *and*
`rewrite-core:8.86.1`'s transitive tree (jgit, jackson 2.21.4, gson, asm 9.10.1, JavaEWAH) honor the Java 8 floor.

### Claim 2 — PROVEN
`HttpSenderTransporterFactory` (priority 100) is registered as the **only** http/https transport — the stock
`ApacheTransporterFactory` is dropped in `createTransporterFactories()`; `FileTransporterFactory` is kept. The factory
pulls the `HttpSender` from `session.getConfigProperties().get("openrewrite.httpSender")` on **every** `newInstance`,
so it is resolved per session, not baked at bootstrap.
- `trafficRoutesThroughPerSessionSender_noBypass`: one bootstrapped `RepositorySystem`, two sessions each carrying a
  different `RecordingHttpSender(new HttpUrlConnectionSender(...))` and a separate temp local repo. Each recorder
  captures 3 POM GETs; `server.requestCount == recorderA.count + recorderB.count` — **no request bypasses the sender**.
- `peekMapsToHead_andClassifyDistinguishes404From500`: `HttpSenderTransporter.peek()` issues a **HEAD**; `classify()`
  returns `ERROR_NOT_FOUND` for 404 (peek and get) and `ERROR_OTHER` for 500. This is the load-bearing method that
  feeds resolver negative-caching.
- `httpUrlConnectionSenderIssuesRealHeadOnTheWire`: MockWebServer confirms the wire method is `HEAD`.

### Claim 3 — PROVEN
`CachingArtifactDescriptorReader` decorates the stock reader through `createArtifactDescriptorReader()` with a
GAV-keyed `ConcurrentHashMap`. `T3`: session A (fresh local repo) resolves the graph and populates the cache (3 cold
reads, network hit). Session B uses a **different, empty** temp local repo but the same decorator; it resolves the same
graph with **zero new server requests** (`serverRequestCount` unchanged, ≥3 cache hits). This is exactly the seam
rewrite-maven's pluggable `MavenPomCache` would occupy (a5 §7/§8, judgment #2). Cached results are re-homed onto each
request (`copyFor`) so the collector still sees request-scoped repositories.

## Dependency versions used

- `org.apache.maven.resolver:maven-resolver-supplier-mvn3:2.0.20` (unchanged from the brief; 2.0.20 is on Central).
  Transitively: resolver `2.0.20` (api, util, spi, named-locks, impl, connector-basic, transport-file, transport-apache);
  `maven-resolver-provider`, `maven-model-builder`, `maven-model`, `maven-artifact`, `maven-builder-support`,
  `maven-repository-metadata` all **3.9.16**; `plexus-utils:3.6.1`, `plexus-interpolation:1.29`, `asm:9.10.1`;
  `httpclient:4.5.14`, `httpcore:4.4.16`, `commons-codec:1.22.0`, `jcl-over-slf4j:2.0.18`; `slf4j-api:2.0.18`;
  `gson:2.14.0` + `error_prone_annotations:2.48.0` (via resolver-spi).
- `org.openrewrite:rewrite-core:latest.release` → resolved **8.86.1**.
- Test: `org.junit:junit-bom:5.11.4` / `junit-jupiter`, `com.squareup.okhttp3:mockwebserver:4.12.0`,
  `junit-platform-launcher`, `org.slf4j:slf4j-nop:2.0.16` (test-only, kept off the scanned runtime classpath).
- All main+test compiled with `options.release = 8`.

## Discrepancies vs. the study reports

1. **Remote Repository Filtering is ON by default and emits out-of-band traffic — including to Maven Central.**
   The mvn3 supplier registers `GroupIdRemoteRepositoryFilterSource` and `PrefixesRemoteRepositoryFilterSource` with
   `DEFAULT_ENABLED = true`. With them active, the first collect made the injected sender issue GETs for
   `<repo>/.meta/prefixes.txt` **and** for `/maven2/.meta/prefixes.txt`, `.sha1`, `.md5` — Maven Central's context path,
   with default (non-ignore) checksum policy — even though the request only listed the mock repo. The mock server never
   saw those `/maven2` requests (different authority); they were handed to our sender out of band. a5 §1.1 lists several
   `create*()` methods that return empty "extension points" but does **not** flag that the *filter sources* are enabled
   and do network I/O. **Plan impact:** the migration must override `createRemoteRepositoryFilterSources()` (or set
   `aether.remoteRepositoryFilter.{groupId,prefixes}.enabled=false`) or builds will emit surprise Central traffic and be
   non-deterministic. The spike disables them in `SpikeRepositorySystemSupplier`.

2. **`asm` is present, not excluded.** a7 §1a states the supplier excludes ASM. Reality: `org.ow2.asm:asm:9.10.1`
   resolves transitively via `maven-model-builder:3.9.16`. Harmless (ASM base classes are ≤ 52, T4 passes), but the
   "no ASM" footprint claim is wrong for the Gradle-resolved graph. Likewise `maven-resolver-spi` now drags
   `gson` + `error_prone_annotations`, not in a7's ~20-jar list.

3. **`transport-apache` (httpclient 4.5.14) is dead weight with a custom `HttpSender` transport.** It stays on the
   classpath as a supplier transitive but is unused once `ApacheTransporterFactory` is dropped. The real migration can
   exclude `maven-resolver-transport-apache` + httpclient/httpcore/commons-codec if `HttpSender` is the sole transport.

4. **`HttpUrlConnectionSender` HEAD works; the trap is test-harness, not the sender.** I initially expected rewrite's
   `HttpUrlConnectionSender` to break on HEAD because `send()` forces `setDoOutput(true)` on every non-GET method.
   Empirically it does **not** — it issues a real HEAD on the wire (test `httpUrlConnectionSenderIssuesRealHeadOnTheWire`).
   The real failure was MockWebServer serving a response **body on a HEAD**, whose bytes poisoned the next keep-alive
   request (`ProtocolException: Unexpected status line: <project/>HTTP/1.1 404`). Mock dispatchers must omit the body on
   HEAD. Not a study discrepancy, but a concrete gotcha for the migration's test infrastructure.

## Files

- Production-shaped: `src/main/java/spike/{HttpSenderTransporter,HttpSenderTransporterFactory,RecordingHttpSender,CachingArtifactDescriptorReader}.java`
- Test bootstrap + harness: `src/test/java/spike/{SpikeRepositorySystemSupplier,TinyMavenRepo,Spike}.java`
- Tests: `src/test/java/spike/{T1_BootstrapCollectTest,T2_HttpInjectionTest,T3_DescriptorDecorationTest,T4_BytecodeVersionTest}.java`
