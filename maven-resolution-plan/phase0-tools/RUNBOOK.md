# Phase-0 corpus tooling runbook

Standalone Gradle project (PHASE-0-SPEC.md §3, slice B). Final package
`org.openrewrite.maven.parity.corpus`; merges into rewrite-maven test sources at review, at
which point the plain-Jackson snapshot in `CorpusResolutionRunner` is replaced by slice A's
formal `ResolutionSnapshot`.

All commands run from the repo root with the repo's wrapper.

## Local-build wiring (consume THIS worktree, not a release)

phase0-tools resolves `org.openrewrite:rewrite-maven` (and its shaded `rewrite-maven-engine`) from
the **local worktree build**, so the corpus gate exercises the dual-engine code under review.

The intended wiring was a composite build (`includeBuild("../..")` + `dependencySubstitution`), but
Gradle 9.5.1 trips the `DefaultClassLoaderScope … must be locked` bug when including the rewrite
root (its `org.openrewrite.build.*` convention plugins force premature configuration of
`rewrite-maven` while phase0-tools' compileClasspath is being resolved). The local build is healthy
standalone; only cross-build inclusion fails. See the header comment in `settings.gradle.kts`.

So the local build is consumed through **Maven Local**. Publish once (and after any local engine
change), then every corpus task picks it up:

```bash
./gradlew :rewrite-core:publishToMavenLocal :rewrite-xml:publishToMavenLocal \
          :rewrite-java:publishToMavenLocal :rewrite-properties:publishToMavenLocal \
          :rewrite-yaml:publishToMavenLocal :rewrite-maven-engine:publishToMavenLocal \
          :rewrite-maven:publishToMavenLocal
```

`build.gradle.kts` pins the worktree version (`rewriteVersion`, default `8.87.0-SNAPSHOT`, override
with `-PrewriteVersion=…`); `mavenLocal()` is first in the repository list so the SNAPSHOT wins and
the released `8.86.1` is never resolved. **Verify the substitution took:**

```bash
# every org.openrewrite:rewrite-* must show the worktree SNAPSHOT, incl. rewrite-maven-engine
./gradlew -p maven-resolution-plan/phase0-tools dependencies --configuration runtimeClasspath | grep rewrite
```

The runner also prints, at startup, the jar it loaded `ResolutionEngineSelector` from (a class that
exists only in the local build) — a runtime proof the local rewrite-maven is on the classpath.

## Engine selector + shadow census

Every record/replay task accepts `-Pengine=legacy|maven|shadow` (default legacy). It is passed as
the real `org.openrewrite.maven.resolution.engine` system property and threaded by the runner onto
each `ExecutionContext` (`ResolutionEngineSelector.ENGINE_KEY`). In `shadow` REPLAY the runner
catches the facade's per-pom `AssertionError` (unexplained-diff signal) and records it **per entry**
instead of aborting the run, emitting a census to `.corpus/census/`:
`summary.tsv` (`<entry>\t{CLEAN|UNEXPLAINED|…}`) plus a `<entry>.diff` excerpt for each unexplained
entry. Ledgered masks (`rewrite-maven/src/main/resources/parity/masks.txt`) apply inside the
facade, so anything that surfaces as an `AssertionError` is unexplained by definition.

## Pipeline

```bash
# 1. Fetch: materialize corpus.yaml into .corpus/
#    - single-pom entries: pom downloaded from Central THROUGH RecordingHttpSender (RECORD),
#      so the store already holds the pom bytes
#    - reactors: git clone --depth 1 --single-branch at the pinned tag
./gradlew -p maven-resolution-plan/phase0-tools corpusFetch

# 2. Record: resolve every entry with released rewrite-maven, all HTTP through
#    RecordingHttpSender RECORD -> populates .corpus/store/ with every exchange
./gradlew -p maven-resolution-plan/phase0-tools corpusRun -Pmode=record

# 3. Ground truth: real Maven 3.9.16 (downloaded into .corpus/tools/ unless PATH mvn is
#    exactly 3.9.16) captures dependency:tree -Dverbose + help:effective-pom per entry into
#    .corpus/ground-truth/<entry>/. Isolated -Dmaven.repo.local under .corpus/m2; ~/.m2 is
#    never touched.
./gradlew -p maven-resolution-plan/phase0-tools groundTruthCapture

# 4. Replay: hermetic verification. Every entry resolves TWICE with fresh caches, byte-wise
#    compared (determinism), served exclusively from the store (REPLAY has no delegate; a miss
#    throws with the missing URL). The gradle task additionally points http(s).proxyHost at a
#    dead port so any HTTP client that bypassed the sender fails loudly.
./gradlew -p maven-resolution-plan/phase0-tools corpusRun            # -Pmode=replay is default
```

Filter any task to specific entries: `-Pentries=guava-33.0.0-jre,apache-maven-3.9.16`.

### Shadow corpus gate (Phase 2/3 exit)

```bash
# 5. Store top-up: the store above was captured against the legacy request profile. The engine
#    additionally requests checksum sidecars (.sha1/.md5) and HEAD peeks, so hermetic REPLAY would
#    hard-miss. Read-through RECORD with engine=maven adds ONLY those engine-only exchanges across
#    all modules (network). engine=maven, not engine=shadow: a shadow RECORD throws its first-diff
#    AssertionError mid-reactor (parseInputs does not catch Error), aborting before later modules'
#    HTTP is captured; engine=maven runs the engine alone, full traversal, identical HTTP profile.
./gradlew -p maven-resolution-plan/phase0-tools corpusRun -Pmode=record -Pengine=maven

# 6. The repeatable gate: full corpus, shadow, hermetic REPLAY (dead-proxy + no-delegate store),
#    twice per entry (determinism). Unexplained diffs are recorded per entry, never abort the run.
./gradlew -p maven-resolution-plan/phase0-tools corpusRun -Pmode=replay -Pengine=shadow
# -> .corpus/census/summary.tsv + .corpus/census/<entry>.diff ; expect zero replay misses.
# Cross-JVM determinism: run step 6 again and `diff -rq` the two .corpus/census/ trees (byte-equal).
```

Results of the recorded gate run: `maven-resolution-plan/corpus-shadow-results.md`.

Unit tests for the tooling itself (record/replay round-trip, canonicalization, store
determinism, layout): `./gradlew -p maven-resolution-plan/phase0-tools test`.

## Store

`sha256(method + "\n" + canonical-url)` → `store/<2-char-prefix>/<hash>/{meta.json, body.bin}`.
Canonicalization: lower-cased scheme/host, default ports dropped, duplicate path slashes
collapsed, trailing slash stripped, query params sorted, fragment dropped. Delegate exceptions
(connect failures) are recorded as `exception` metas and replayed as `UncheckedIOException`.
RECORD is read-through (store hit never re-fetches); a deliberate refresh = `rm -rf
.corpus/store` and re-record.

## Committed vs cached

| Committed | Cached (never committed) |
|---|---|
| `corpus.yaml`, sources, this runbook | everything under `.corpus/`: HTTP store, downloaded poms, reactor clones, Maven distro + isolated `m2/`, ground truth, snapshots |

CI cache keying suggestion: key = `sha256(corpus.yaml)` (e.g.
`corpus-${{ hashFiles('maven-resolution-plan/phase0-tools/corpus.yaml') }}`) over `.corpus/store`
+ `.corpus/poms` + `.corpus/ground-truth`. Reactor clones and the Maven distro are cheap to
re-materialize and can key on the same hash or be excluded. Editing the manifest deliberately
invalidates the cache → re-fetch/re-record/re-capture; a store refresh without a manifest change
is `rm -rf .corpus/store` + re-record (deliberate re-record policy per spec).

## Sizing notes

`corpusRun` runs with `maxHeapSize = "6g"` — the dubbo reactor (~190 modules) OOMs the default
heap. Reactor snapshots serialize each module's marker with `parent`/`modules` flattened to GAV
references (`parentGav`/`moduleGavs` on the document node); embedding the object graph is
quadratic in module count. The dubbo snapshot is still ~195 MB of pretty-printed JSON; slice A's
formal `ResolutionSnapshot` projection replaces this wholesale at merge time.

## Determinism-run scope note

The twice-run comparison uses fresh `ExecutionContext`/`InMemoryMavenPomCache`/sender instances
per pass but shares one JVM, so JVM-static state inside rewrite-maven is shared across passes.
Cross-JVM determinism is checked by running the replay task itself twice and diffing
`.corpus/snapshots/` (the runner writes snapshots on every replay run; identical bytes expected).
