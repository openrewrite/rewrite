# Corpus shadow gate — Phase 2/3 exit results

Dual-engine SHADOW resolution of the recorded real-world corpus (18 Maven Central poms +
apache-maven + apache-dubbo reactors), every difference ledger-classified. This is the flip's
safety mechanism per the no-compat-flags decision: what the SHADOW facade cannot explain with a
ledgered mask surfaces as an `AssertionError` and is root-caused here.

Run against the **local worktree build** `org.openrewrite:rewrite-maven:8.87.0-SNAPSHOT`
(engine shaded in), branch `phase3-corpus`. Hermetic REPLAY, network off (dead-proxy), each entry
resolved twice per JVM and the whole gate run in two JVMs — **zero replay misses, byte-identical
reports** (determinism verdict below).

## Census table

`clean/masked` = the facade accepted the resolution (identical, or every diff explained by a
`parity/masks.txt` row applied inside the facade — indistinguishable from outside, by design).
`UNEXPLAINED` = the facade threw; the diff is not covered by any mask and is classified below.

| entry | kind | verdict | classification |
|---|---|---|---|
| spring-boot-starter-web-3.2.0 | pom | clean/masked | — |
| quarkus-bom-3.6.0 | pom | clean/masked | — |
| jackson-databind-2.16.1 | pom | clean/masked | — |
| junit-jupiter-5.10.1 | pom | clean/masked | — |
| hibernate-core-6.4.1.Final | pom | clean/masked | — |
| spring-cloud-dependencies-2023.0.0 | pom | clean/masked | — |
| opentelemetry-sdk-1.34.1 | pom | clean/masked | — |
| kotlin-stdlib-1.9.22 | pom | clean/masked | — |
| scala-library-2.13.12 | pom | clean/masked | — |
| spring-boot-dependencies-3.2.0 | pom | clean/masked | — |
| jetty-server-11.0.19 | pom | UNEXPLAINED | **L-P3-C-005** (ledgered, OPEN) — error-message shape |
| log4j-core-2.22.1 | pom | UNEXPLAINED | **L-P3-C-004 / L-P3-D-003(b)** (ledgered, OPEN) — engine drops slf4j-api + reorders |
| awssdk-s3-2.22.0 | pom | UNEXPLAINED | **L-P3-C-004 / L-P3-D-003(b)** (ledgered, OPEN) — engine drops slf4j-api/eventstream closure |
| grpc-core-1.60.1 | pom | UNEXPLAINED | **L-P3-C-004 / L-P3-D-003(b)** (ledgered, OPEN) — engine drops error_prone + grpc-core self-transitive |
| apache-maven-3.9.16 | reactor | UNEXPLAINED | **L-P3-C-004 / L-P3-D-003(b)** (ledgered, OPEN) — engine drops commons-codec/error_prone closure |
| guava-33.0.0-jre | pom | UNEXPLAINED | **NEW-1** — profile-activated `system`-scoped `jdk:srczip` retained in requested |
| hadoop-client-3.3.6 | pom | UNEXPLAINED | **NEW-2** — `<os>`-profile-activated properties injected |
| netty-handler-4.1.104.Final | pom | UNEXPLAINED | **NEW-2** (same family) — `<os>`-profile property override + `${os.detected.classifier}` DM interpolation |
| spark-core-2.13-3.5.0 | pom | UNEXPLAINED | **NEW-3** repo-mirror attribution (473/1540 lines) + L-P3-C-002/L-P3-C-004 residue |
| apache-dubbo-3.2.10 | reactor | UNEXPLAINED | **NEW-4** `${project.parent.version}` not interpolated in threaded `requested` + L-P3-C-004/L-P3-C-002 residue |

**10 clean/masked, 10 unexplained.** Full per-entry AssertionError excerpts:
`.corpus/census/<entry>.diff`; machine-readable roll-up: `.corpus/census/summary.tsv`.

Deferred/skipped: `snapshot-consumer-deferred` (SNAPSHOTs not on Central), `apache-camel-4.4.0`
(fetch-only per manifest).

### dubbo (highest-value, 130-module `${revision}` reactor)

First unexplained module is `dubbo-cluster:3.2.10` (a reactor abort at the first diff — see the
granularity note below). Its diff has three distinct root classes; the dominant NEW one:

```
$.scopes.Compile[4].requestedRef:
  "val:org.apache.dubbo:dubbo-common:3.2.10"          (legacy)
  != "val:org.apache.dubbo:dubbo-common:${project.parent.version}"   (engine)
```

The engine threads the raw declared version `${project.parent.version}` onto a transitive
`ResolvedDependency.requested` **without interpolating it** (16 such lines). Legacy interpolates
it to `3.2.10`. The remaining dubbo lines are cascades of two ledgered residues: engine
transitive-closure drops (the `io.micrometer:micrometer-core` subtree, `LatencyUtils`) —
L-P3-C-004/L-P3-D-003(b) — and `effectiveExclusions` positional residue — L-P3-C-002.

## New ledger-row evidence (do NOT fix here — for the engine-fix worktree)

Precise, reproducible evidence for divergences **not** covered by any current ledger row. Each is
directional (engine differs from legacy in a specific, characterizable way).

### NEW-1 — `${project.parent.version}` not interpolated in threaded transitive `requested`
- **Entry:** apache-dubbo-3.2.10 (`dubbo-cluster`). **Family:** L-P3-C-001 / L-P3-E-002 (both
  marked FIXED for plain `<properties>` and `${project.groupId}`; this is the residual for the
  Maven built-in project-model expression `${project.parent.version}`).
- **Evidence:** `$.scopes.Compile[4].requestedRef: "val:...dubbo-common:3.2.10" !=
  "val:...dubbo-common:${project.parent.version}"` (also `dubbo-serialization-api`,
  `dubbo-remoting-api`, `dubbo-metrics-api`). The declarer's raw coordinate uses
  `<version>${project.parent.version}</version>`; the engine's `matchDeclared`/`interpolate`
  (mirroring `ResolvedPom.getValue`) resolves plain properties and `${project.version}` but not
  `${project.parent.version}`, so the placeholder survives into the projected `requested`.
- **Class:** KEEP_REWRITE (mirror legacy's interpolated threading) — extend the interpolator to the
  `project.parent.*` expression namespace.

### NEW-2 — `<os>`-condition profile activation injects/overrides model properties
- **Entries:** hadoop-client-3.3.6, netty-handler-4.1.104.Final. **Family:** profile activation
  (cf L-P2-E-003, where legacy activates via `System.getenv` not the model).
- **Evidence (hadoop):** `$.pom.properties.os.detected.classifier: <missing> != "osx-x86_64"`,
  `$.pom.properties.build.platform: <missing> != "Mac_OS_X-${sun.arch.data.model}"`.
  **(netty):** `$.pom.properties.tcnative.artifactId: "netty-tcnative" !=
  "netty-tcnative-boringssl-static"`, `$.pom.properties.argLine.alpnAgent: null != "-javaagent:…"`,
  and `$.pom.dependencyManagement{io.netty:netty-tcnative:${os.detected.classifier}:jar}` present
  in legacy but `<missing>` in engine (the engine interpolated the classifier key to the concrete
  `osx-x86_64`, so it no longer matches the literal-`${…}` key).
- **Note:** engine is Maven-correct (real Maven activates `<os>` profiles), but the values are
  **machine-dependent** (`osx-*` here; `linux-x86_64` on CI). Directional, engine-superset like
  L-P2-E-003. Flag whether the ledger row masks it directionally or the engine suppresses
  `os.detected.*` for reproducibility.

### NEW-3 — remote repository mirror attribution
- **Entry:** spark-core-2.13-3.5.0 (dominant: 473 of 1540 lines). **Family:** repo attribution
  (cf L-P2-C-004 repo projection, L-P3-D-002 local-cache attribution).
- **Evidence:** `$.scopes.Compile[*].repo:
  "https://maven-central.storage-download.googleapis.com/maven2/" !=
  "https://repo.maven.apache.org/maven2"` across nearly every resolved node. spark's effective
  model declares the Google GCS Central mirror (via its parent); legacy resolves+attributes to the
  declared mirror, the engine to canonical Central. Identical bytes, different attributed origin —
  a repository-preference/ordering divergence.
- **Class:** TBD (repo-universe/ordering) — decide whether attribution follows declared-repo order
  (legacy) or the engine's central-first order.

### NEW-4 — profile-activated `system`-scoped dependency retained in `requestedDependencies`
- **Entry:** guava-33.0.0-jre (clean sentinel — this is its *entire* divergence). **Family:**
  system-scope handling (L-P2-E-001) + profile activation.
- **Evidence:** every `requestedRef` shifts by exactly +1 (`root[N]` legacy vs `root[N+1]` engine)
  across all four scopes. Root cause (confirmed by dumping both requested lists): the engine's
  `requestedDependencies[0]` is `jdk:srczip:999` (scope `system`), a guava-parent
  profile-activated system dep that **legacy omits** from the requested list. The resolved graph is
  otherwise identical; only the requested-index pointer shifts.
- **Class:** ALIGN — decide whether the projected `requestedDependencies` includes the
  profile-activated system dep (engine) or omits it (legacy).

## Confirmed ledgered residues (no new row — cite existing)

- **L-P3-C-005** (error-message shape, OPEN): jetty-server-11.0.19 —
  `$.errors[0].message: "…jetty-http-tools:11.0.19 failed. Unable to download POM: …" != "Unable to
  download POM: …"`. Legacy's `"<gav> failed. "` prefix is absent from the engine collector's text.
- **L-P3-C-004 / L-P3-D-003(b)** `gav-mediation` residue (OPEN): log4j-core, awssdk-s3, grpc-core,
  apache-maven, apache-dubbo — the engine drops a transitive node/subtree the legacy closure keeps
  (recurring victims: `org.slf4j:slf4j-api` with a managed/null requested version,
  `com.google.errorprone:error_prone_annotations`, `io.micrometer:micrometer-core`), producing an
  index shift and downstream `gav`/`dated`/`licenses`/`children` cascades. Per the ledger these
  engine DROPs are deliberately left red and unmasked.
- **L-P3-C-002** `effectiveExclusions` residue (OPEN-ish): spark-core, apache-dubbo — a handful of
  pruned coordinates (`org.slf4j:slf4j-api`, `org.apache.zookeeper:zookeeper`,
  `org.hdrhistogram:HdrHistogram`) present on one side only.

## Store top-up stats

The recorded store was captured against the legacy request profile; the engine additionally
requests checksum sidecars (`.sha1`/`.md5`) and issues HEAD peeks, so a naive hermetic REPLAY would
hard-miss. Top-up sequence (network on):

| step | task | store `meta.json` count |
|---|---|---|
| fetch | `corpusFetch` (poms recorded through the sender, reactors cloned) | 18 |
| legacy record | `corpusRun -Pmode=record` (engine=legacy) | 2417 |
| engine top-up | `corpusRun -Pmode=record -Pengine=maven` | **2628** (+211 engine-only) |

`engine=maven` (not `engine=shadow`) is used for the reactor top-up on purpose: a SHADOW record
throws its `AssertionError` at the **first** diffing reactor module, aborting the parse batch
before later modules' engine HTTP is captured (`MavenParser.parseInputs` does not catch `Error`).
`engine=maven` runs only the engine — no shadow assertion, full traversal — and its HTTP profile is
identical to the engine's contribution under shadow (engine requests depend only on the engine's
own logic, not on whether legacy runs alongside). Legacy exchanges are already present from the
legacy record; RECORD is read-through, so the top-up only fetches the ~211 engine-only exchanges.
4 exchanges are recorded as `exception` metas (genuine 404s, e.g. jetty-http-tools).

**Hermetic gate:** `corpusRun -Pmode=replay -Pengine=shadow` — **zero replay misses** (no store
miss threw; the dead-proxy `http(s).proxyHost=127.0.0.1:1` belt-and-suspenders never tripped).

## Determinism verdict — PASS

- **In-JVM:** every entry resolved twice with fresh `ExecutionContext`/`InMemoryMavenPomCache`/
  sender; clean entries byte-identical, unexplained entries produced identical AssertionError text
  both passes. No `NONDETERMINISTIC` finding.
- **Cross-JVM:** the whole shadow gate was run in two separate JVMs; `.corpus/census/` (per-entry
  `.diff` excerpts + `summary.tsv`) is **byte-identical** across the runs (`diff -rq` exit 0).

## Local-build wiring

phase0-tools consumes **this worktree's** rewrite-maven (+ shaded rewrite-maven-engine), not the
released artifact. Intended path was a composite build — `includeBuild("../..")` with an explicit
`dependencySubstitution` onto `:rewrite-maven`. **Gradle 9.5.1 cannot include the rewrite root as a
build here:** resolving phase0-tools' `compileClasspath` forces configuration of the included
build's `rewrite-maven` project through the root project's buildscript `classpath` configuration,
tripping the known `DefaultClassLoaderScope … must be locked before it can be used to compute a
classpath` bug (the root applies the `org.openrewrite.build.*` precompiled convention plugins).
The local build is healthy in isolation (`:rewrite-maven:jar`, `:rewrite-maven-engine:shadowJar`
succeed); only cross-build inclusion trips it.

So the local build is consumed through **Maven Local** instead (documented in `settings.gradle.kts`
and the RUNBOOK):

```bash
./gradlew :rewrite-core:publishToMavenLocal :rewrite-xml:publishToMavenLocal \
          :rewrite-java:publishToMavenLocal :rewrite-properties:publishToMavenLocal \
          :rewrite-yaml:publishToMavenLocal :rewrite-maven-engine:publishToMavenLocal \
          :rewrite-maven:publishToMavenLocal
```

pinned to the worktree's `8.87.0-SNAPSHOT` (override with `-PrewriteVersion=…`), which resolves
only from `~/.m2` — never the released `8.86.1`. **Verification** — the substitution is proven two
ways: (1) `./gradlew -p …/phase0-tools dependencies --configuration runtimeClasspath` shows every
`org.openrewrite:rewrite-*` at `8.87.0-SNAPSHOT` including `rewrite-maven-engine`; (2) the runner
prints, at startup, the jar `org.openrewrite.maven.internal.ResolutionEngineSelector` loaded from —
a class absent from any released rewrite-maven:

```
[wiring] ResolutionEngineSelector loaded from file:/…/.m2/repository/org/openrewrite/rewrite-maven/8.87.0-SNAPSHOT/rewrite-maven-8.87.0-SNAPSHOT.jar
```

## Reactor census granularity (known limitation)

The SHADOW `AssertionError` is thrown from inside `MavenParser.parseInputs`, which does not catch
`Error`, so on a reactor the **first** diffing module aborts the whole batch and the runner records
that one module's excerpt for the entry. Single-pom entries are unaffected (the assertion fires
after that pom's full resolution). Per-module reactor census would need the facade to accumulate
rather than throw; out of scope for the gate, which classifies at entry granularity.
