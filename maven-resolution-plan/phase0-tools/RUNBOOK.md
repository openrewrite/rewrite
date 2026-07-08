# Phase-0 corpus tooling runbook

Standalone Gradle project (PHASE-0-SPEC.md §3, slice B). Final package
`org.openrewrite.maven.parity.corpus`; merges into rewrite-maven test sources at review, at
which point the plain-Jackson snapshot in `CorpusResolutionRunner` is replaced by slice A's
formal `ResolutionSnapshot`.

All commands run from the repo root with the repo's wrapper. Resolved legacy engine:
`org.openrewrite:rewrite-maven:latest.release` → **8.86.1** at the time of recording.

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
