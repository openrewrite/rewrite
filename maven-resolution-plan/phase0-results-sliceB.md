# Phase 0 slice B results — corpus tooling (record/replay, manifest+fetch, ground truth)

2026-07-08. Built per PHASE-0-SPEC.md §3 in `maven-resolution-plan/phase0-tools/` (standalone
Gradle project, package `org.openrewrite.maven.parity.corpus`). Legacy engine resolved from
`org.openrewrite:rewrite-maven:latest.release` → **8.86.1**. Ground truth from **Apache Maven
3.9.16** (bin distro auto-downloaded into `.corpus/tools/`; PATH mvn was 3.9.14 → not used).

## What was built

| File | Notes |
|---|---|
| `RecordingHttpSender.java` | RECORD (read-through: store hit never re-fetches; refresh = delete store) / REPLAY (no delegate at all; miss → `IllegalStateException` with the missing method+URL). Key `sha256(method + "\n" + canonical-url)` → `store/<2-char>/<hash>/{meta.json, body.bin}`. Canonicalization: scheme/host case, default ports, duplicate-slash collapse, trailing slash, sorted query, fragment dropped. Delegate `UncheckedIOException`s (dead hosts) are recorded as `exception` metas and re-thrown on replay. meta.json is timestamp-free with sorted headers (subset: Content-Type/Length, ETag, Last-Modified, Location) so re-recording is byte-identical. |
| `corpus.yaml` | 18 single-pom entries (the 13 assigned + grpc-core-1.60.1 **real range user** `[1.60.1]`, spark-core_2.13-3.5.0 **wide+deep+exclusions**, hadoop-client-3.3.6 **exclusion-heavy**, spring-boot-dependencies-3.2.0 **packaging pom/property indirection**, jetty-server-11.0.19 **deep parent chain** — turned out to double as an error-path fixture, see findings), 1 deferred SNAPSHOT placeholder (Central hosts none → slice C synthetic corpus), 3 reactors (maven-3.9.16, dubbo-3.2.10 `${revision}`, camel-4.4.0 fetch-only benchmark subject). Manifest extensions beyond spec format: boolean `deferred`/`fetchOnly` so tools skip mechanically (spec had these only as notes). |
| `CorpusFetch.java` | Poms fetched from Central *through* the recording sender; reactors `git clone --depth 1 --single-branch` at pinned tags. |
| `GroundTruthCapture.java` | Pinned plugin invocations (`maven-help-plugin:3.4.0:effective-pom`, `maven-dependency-plugin:3.6.1:tree -Dverbose`), isolated `-Dmaven.repo.local=.corpus/m2` (never ~/.m2). Single-pom entries: effective-pom of the artifact's own pom (`mvn -f`) **and** tree via a generated consumer stub (sole dependency; `<type>pom</type>` auto-added for pom-packaging). Reactors: aggregated effective-pom + `-DappendOutput=true` tree. 15-min/entry timeout. |
| `CorpusResolutionRunner.java` | `-Pmode=record` populates the store via full MavenParser resolution; `-Pmode=replay` (default) resolves each entry **twice** with fresh `ExecutionContext`/`InMemoryMavenPomCache`/sender, byte-compares, writes `.corpus/snapshots/<entry>.json`. Snapshot = plain-Jackson projection of the `MavenResolutionResult` marker (sorted keys, NON_NULL, marker UUID normalized, `ParseExceptionResult` captured; reactor `parent`/`modules` flattened to `parentGav`/`moduleGavs` — see findings). Slice A's formal `ResolutionSnapshot` replaces this projection at merge time. `setAddLocalRepository(false)` pinned so ~/.m2 can't leak into results. |
| `RUNBOOK.md` | fetch → record → capture → replay commands, committed-vs-cached policy, CI cache key = hash of `corpus.yaml`. |
| Unit tests | 11 green: record/replay round-trip (status/body/headers), 404 + exception round-trip, replay-miss message, read-through, store byte-determinism across re-record, 2-char/64-hex layout, query-order + trailing-slash + default-port + duplicate-slash canonicalization, equivalent-URLs-share-one-entry. |

## Materialization status (all four stages unless noted)

- **Fetched:** 18/18 poms, 3/3 reactors (camel-4.4.0 = 386 MB shallow clone).
- **Recorded:** 18 poms + maven + dubbo. Store: **2,417 exchanges, 33 MB** (2,144 GET / 254 HEAD / 19 OPTIONS; 1,911×200, 474×404, 20×401, misc 4xx/5xx, 4 exception entries = dead-repo probes: nexus.codehaus.org DNS-dead, maven.java.net expired cert — recorded and replayed as exceptions).
- **Ground truth:** 20/20 capture targets (18 poms both flavors + maven reactor 15 modules + dubbo reactor 128 modules), 23 MB, in 2m18s. Verbose trees carry conflict annotations (hadoop 95 / spark 133 "omitted for" lines; grpc shows range/scope notes).
- **Replayed:** 20/20 entries green under REPLAY. Camel: fetch-only as specified (no ground truth/record/resolution — noted, benchmark subject).

## Determinism findings

- **No nondeterminism found.** In-JVM twice-run with fresh caches: byte-identical for all 20 entries. Cross-JVM: two full replay task invocations produced byte-identical `.corpus/snapshots/` (`diff -r` clean), including the 195 MB dubbo snapshot. Caveat: the twice-run shares one JVM, so rewrite-maven JVM-static state is shared; the cross-JVM diff covers that gap.
- Two tooling-side OOMs during bring-up (not engine nondeterminism): default heap too small for dubbo, and — the real bug — serializing each module's marker with live `parent`/`modules` references embeds the whole reactor into every module's JSON (quadratic in module count). Fixed with a Jackson mixin + GAV-reference linkage; `corpusRun` now runs at 6g.

## Replay hermeticity proof

Three independent mechanisms: (1) REPLAY-mode sender has **no delegate** — network is unreachable by construction; (2) any store miss throws with the missing URL — zero misses across all 20 entries; (3) the replay gradle task sets `http(s).proxyHost=127.0.0.1:1`, so any HTTP client bypassing the sender fails loudly — none did. Replay is fast enough to prove no network is involved (dubbo: 190-module resolution ×2 in 4.7s; full corpus ×2 ≈ 30s vs ~25 min recording).

## Corpus findings (ledger-relevant)

- **jetty-server-11.0.19**: Central genuinely 404s `org.eclipse.jetty.tests:jetty-http-tools:11.0.19` (a test-scoped *direct* dependency). rewrite-maven fails the entire resolution — `ParseExceptionResult`, **no marker at all**, discarding the resolvable Compile/Runtime scopes. Real Maven: `help:effective-pom` on the same pom succeeds; a consumer sees nothing (test deps don't propagate). Whether Maven-parity requires all-scopes-fail vs per-scope containment is a Phase-2 ledger question; the entry is kept as a deterministic error-path fixture (replays byte-identically).
- Legacy engine normalizes Central to `repo.maven.apache.org` (fetch layer used `repo1.maven.org`; both recorded).
- Legacy engine emits `datedSnapshotVersion == version` for release artifacts in the serialized marker (visible in every snapshot) — snapshot-shape quirk for slice A to be aware of.

## Deviations from spec

- Manifest: added `deferred`/`fetchOnly` booleans (machine-readable skips). Ground truth lives in `.corpus/ground-truth/<entry>/` beside the store rather than inside hash dirs (store is keyed by request hash; ground truth is per-entry).
- Snapshots are the interim plain-Jackson projection, not slice A's `ResolutionSnapshot` (per instruction; integration at merge).
- Slice-A fixture entries not yet in the manifest (slice A developed in parallel); add at merge.
- Record mode is read-through rather than always-overwrite; refresh = delete store (matches the spec's "deliberate re-record").
