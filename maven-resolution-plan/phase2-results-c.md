# Phase 2 slice C — engine wired into `Pom.resolve`, SHADOW oracle alive

2026-07-08, worktree `maven-resolution` (uncommitted). Wires the B1/B2 effective-pom machinery into the resolution
facade behind `ResolutionEngineSelector`, and brings SHADOW (dual-engine differential) alive. Dependency resolution
(`resolveDependencies`) stays 100% legacy in every mode; this slice is effective-pom construction only.

## Shadow gate results (headline)

- **`MavenParserTest` in SHADOW (real-world poms, network):** 109 run (2 skipped) — **clean 41 / masked-expected 37 / open 31.**
  (68 fail with masks off → 41 clean; 31 fail with masks on → 37 rescued by the ledgered masks.)
- **Parity fixture suites in SHADOW** (`DeterminismTest`, `IdentityContractsTest`, `EngineResolvedPomComparisonTest`,
  `SerializedLstCompatibilityTest`): **green** — only ledgered masks absorb diffs.
- **LEGACY (default):** parity + engine + cache + `MavenParserTest` all green (nothing changed by default).
- **MAVEN mutate-parent-then-re-resolve** (`EngineReResolutionTest`): green.

## Wiring architecture (short)

Single chokepoint: **`ResolvedPom.resolve(ctx, downloader)`** — the effective-pom builder both `Pom.resolve` (initial
parse) and `UpdateMavenModel` (re-resolution) funnel through. Its `.resolver(…).resolve()` construction is wrapped by
`MavenEngineResolution.effectivePom(requested, activeProfiles, downloader, ctx, legacyLambda)`:

- **LEGACY** → runs the legacy lambda untouched.
- **MAVEN** → `EngineEffectivePom` + `EffectivePomMapper` (+ `BomGavAttributor`), composing `SettingsBridge`
  (`requestRepositories`/`effectiveSettings`) and a fresh `ReactorWorkspace` from `downloader.getProjectPoms()` + the
  registry bytes. Engine bootstrap (`MavenEngine` + one session + scratch dir) is **cached on the ctx**
  (`computeMessageIfAbsent`); a fresh `ModelCache` per build keeps `servedBy` complete on warm runs.
- **SHADOW** → runs legacy and engine, snapshots both `ResolvedPom`s' `$.pom` sections
  (`ResolutionSnapshot`/`ResolutionDiff`), masks with `parity/masks.txt`, throws `AssertionError` on any unexplained
  diff (or on an asymmetric throw — report-both-outcomes), and **returns the legacy result** (shadow never changes
  behavior).

Supporting pieces:
- **Deliverable 1** — `ResolutionSnapshot`/`ResolutionDiff`/`SnapshotNormalizer`/`RecordingResolutionListener` moved from
  test sources to `rewrite-maven` main under `org.openrewrite.maven.internal.parity` (the shadow comparator ships inside
  the facade); `parity/masks.txt` moved to main resources. Jackson + tree types only. Every test updated to import from
  the new package; all prior parity/engine/cache tests stay green unmodified in behavior.
- **Deliverable 2 (XML-first plumbing)** — `PomXmlRegistry` (ctx-message-backed, keyed by source path + GAV): `MavenParser`
  stores each project pom's printed `Xml.Document` bytes + the parser `.property()` map at parse time; `UpdateMavenModel`
  refreshes the mutated document's printed bytes, re-sets injected properties, and `bumpEpoch()`s on re-resolution. The
  registry also carries the reactor epoch (seeds a fresh `ReactorWorkspace` each resolve). Synthetic `Pom.builder()`
  graphs (no bytes) fall through to `PomToModelConverter`. No public API changes; additive internal only.
- **Re-entrancy guard** — the legacy resolver imports BOMs/parents via nested `Pom.resolve` (ResolvedPom ~L941); a
  ThreadLocal makes only the outermost facade call dispatch, so those legacy-internal resolutions never re-enter the
  engine (they have no engine equivalent — the engine imports BOMs inside `DefaultModelBuilder`).

Two engine-side robustness fixes this slice made (both cache-warmth bugs the B1/B2 cold harness never exercised):
1. `EffectivePomMapper.knownModelIds` now derives from the model **lineage** (`getModelIds()` minus the empty super-POM
   id), not `servedBy` — so a parent served from the warm bytes cache still keeps its `pluginManagement`.
2. `EngineEffectivePom.build` uses a **fresh per-build `ModelCache`** so the model-build walk (and thus `servedBy` for
   `BomGavAttributor`) is complete on every resolve; byte downloads stay avoided by the `MavenPomCache` bytes region.

### CI invocation

`-Dorg.openrewrite.maven.resolution.engine=shadow ./gradlew :rewrite-maven:test --tests "org.openrewrite.maven.parity.*"`
Gradle does not forward `-D` to the forked test JVM by default; `rewrite-maven/build.gradle.kts` now forwards this one
property to the `Test` task (the only build change).

## New ledger rows (all appended to `doc/maven-resolution-ledger.md`, masks in `parity/masks.txt`)

| id | area | class | mask / mechanism |
|---|---|---|---|
| L-P2-C-001 | pluginManagement Maven-effective merge/order/interpolation (same class as plugins L-P0-002/003) | ALIGN_TO_MAVEN | `$.pom.pluginManagement` |
| L-P2-C-002 | legacy `ResolvedPom.resolve` drops `<modules>` via its `emptyList()` scaffold; engine preserves them | KEEP_REWRITE | `$.pom.subprojects` |
| L-P2-C-003 | missing `<modelVersion>` — engine defaults it to 4.0.0 (mirrors `RawPom`) so it reads rewrite's lenient model | KEEP_REWRITE | `MavenEngineResolution.ensureModelVersion` (not a mask) |
| L-P2-C-004 | engine projects declared/inherited repos only; legacy prepends embedder `local`/`central` (extends L-P2-B2-001) | KEEP_REWRITE | `$.pom.repositories` |

## Open findings (NOT masked — root-caused but unresolved within budget)

All 31 open `MavenParserTest`-shadow failures are **"legacy resolved / engine threw"** or a small set of genuine
`$.pom` content diffs. None are the reverse direction.

**18 outcome mismatches — the engine's `DefaultModelBuilder` is stricter than rewrite's lenient parser / not yet wired
for some version resolution:**
- **9 `Non-resolvable import POM`** — BOM imports at version `RELEASE`/`LATEST` (`spring-boot-dependencies:release|latest`),
  a reactor-sibling BOM (`org.openrewrite.maven:c`), and a local-repo snapshot BOM (`org.openrewrite:sam-bom`). The
  engine's version-metadata + reactor-BOM resolution isn't wired for these on the effective-pom path (collection
  territory — Phase 3).
- **~8 packaging / ~4 must-be-pom / ~2 aggregator** — Maven requires aggregator/parent packaging to be `pom`; legacy
  tolerates `jar`.
- **2 recursive expression cycle** — a self-referential `${revision}`; Maven rejects, legacy tolerates.
- **1 self-parent** — parent GA equals project GA; Maven rejects, legacy tolerates.

These are the DESIGN §9 "fail where Maven fails" tension against rewrite's deliberate snippet-parsing leniency. Whether
each becomes ALIGN_TO_MAVEN (legacy too lenient) or KEEP_REWRITE (relax the engine) needs Maven ground-truth per case —
Phase-3 ledger triage, not a mask.

**13 pom-section diffs (genuine effective-pom content):**
- **`$.pom.dependencyManagement` (14 diff lines)** — DM entries that differ only by type/classifier
  (`io.flux-capacitor:common` as `test-jar` / `sources` / `javadoc` / `pom`) collapse/reorder in the engine vs legacy's
  distinct entries. Excerpt: `dm[5..7]: <missing>` on the engine side; `dm[2].gact: "common::test-jar" != "common::jar"`.
  Engine mapper DM-threading gap — needs investigation (Phase 3 DM/graph).
- **`$.pom.gav` (12 diff lines)** — the engine keeps `${revision}` literal in the requested gav; legacy interpolates it.
  Excerpt: `$.pom.gav: "net.sample:sample:0.0.0-SNAPSHOT" != "net.sample:sample:${revision}"`. The mapper should
  interpolate the requested gav's version from the merged properties, as `ResolvedPom.Resolver` does.
- **`$.pom.properties` (1)** — a genuine value divergence `project.version: "9.9.9" != "1.2.3"` (builtin vs declared
  `project.version` precedence). The other 73 property diffs seen mid-triage were the benign empty-`<tag/>`
  representation (`null` vs `""`) and are now fixed in the mapper (empty raw value → `null`, matching `RawPom`).

## Acceptance gates

| gate | result |
|---|---|
| a. LEGACY: parity + engine + cache (+ MavenParserTest) green by default | PASS |
| b. Parity fixture suites green in SHADOW (only ledgered masks) | PASS |
| c. `MavenParserTest` in SHADOW triaged | clean 41 / masked-expected 37 / open 31 |
| d. Mutate-parent-then-re-resolve under MAVEN picks up the mutation | PASS (`EngineReResolutionTest`) |
| 6. Registry + mode-routing (×3) + shadow report-both-outcomes unit tests | PASS (`MavenEngineResolutionTest`) |
