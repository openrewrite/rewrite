# Phase 2 slice E — full-suite shadow close-out (recipe re-resolution)

2026-07-08, worktree `maven-resolution` (uncommitted). Takes the first full `:rewrite-maven:test` run in SHADOW mode
from **17 failing classes (~90 tests)** to **only 4 reproducible parity failures** (one documented, out-of-scope
divergence class) plus environmental/flaky noise. LEGACY (default) has no functional regressions.

## Root cause (the prime bug): the engine read pre-mutation XML on every non-`UpdateMavenModel` re-resolution

`ResolvedPom.resolve(ctx, downloader)` is the single effective-pom chokepoint, but **`PomXmlRegistry` was only refreshed
on two paths** — `MavenParser` (initial parse) and `UpdateMavenModel` (re-resolution). Recipe code that mutates the
requested `Pom` and calls `resolve()` **directly** never refreshed the registry. The prime example is `ChangeParentPom`'s
**scanner** (`ChangeParentPom.java:305`): `mrr.getPom().withRequested(pom.withParent(newParent)).resolve(ctx, …)`.

At the facade, LEGACY reads the mutated `requested` `Pom` (new parent) and resolves correctly; the ENGINE read
`PomXmlRegistry.get(ctx, sourcePath, gav)` — keyed by path/gav only, with no link to the `Pom` the bytes were printed
from — and got the **stale initial-parse document** (old parent). So `my-app`'s `$.pom.dependencyManagement` diverged:
legacy = the new parent's world (jackson-parent → junit 4.13.1), engine = the old parent's (spring-boot-starter-parent →
antlr/logback/atomikos). The prompt's characterisation was inverted (the engine was the stale side) but the mechanism —
"one resolves pre-mutation state, the other post-mutation" — was exact.

**Fix (root, general).** `PomXmlRegistry` now records the `Pom` each byte payload was printed from; `get(ctx, requested)`
returns bytes **only when the stored source `Pom` still `.equals(requested)`**. A mutated re-resolution no longer matches,
so the facade falls through to `PomToModelConverter(requested)` (already the synthetic-graph path) and builds from the
authoritative post-mutation `Pom`. This covers **every** direct-`resolve()` recipe path uniformly, not just
`ChangeParentPom`. (`PomXmlRegistry`, `MavenParser`, `UpdateMavenModel`, `MavenEngineResolution.engineEffectivePom`.)

This one fix cleared the bulk of the failures (e.g. `ChangeParentPomTest` 26 → the small tail below).

## Per-class disposition of the residual (post-sync-fix) diffs

Every residual below was **pre-existing**, exposed once the dominant DM-staleness diff was removed — not introduced by
the sync fix. Each was root-caused individually.

| # | symptom | root cause | disposition |
|---|---|---|---|
| 1 | `$.pom.properties.<x>: "" != null` (empty props: `empty.property`, `release.arguments`, `sha1`) | The mapper's blanket empty→null was tuned to self-closing `<tag/>` (RawPom→null, MavenParserTest corpus). Open/close `<tag></tag>` is `""` in RawPom; Maven's model collapses both to `""`. | **FIX** `EffectivePomMapper.putAllIfAbsent` consults the declaring RawPom `Pom` (via `declaringPom`, now reactor-aware) for the exact null-vs-`""` representation. Verified empirically: RawPom → `null` for `<t/>`, `""` for `<t></t>`. |
| 2 | inherited empty prop dropped on the child (reactor parent) | `declaringPom` returned null for a reactor parent (served by the WorkspaceModelResolver, never recorded in `servedBy`). | **FIX** `EffectivePomMapper` takes the `ReactorWorkspace`; `declaringPom` falls back to `reactor.findReactorPom(...)`. |
| 3 | `$.pom.properties.maven.repo.local: <missing> != "C:/blah"` | Injected `MavenParser.property(...)` values leaked onto a **downloaded** pom the engine resolved standalone (`ChangeParentPom`'s old/new-parent probe, `ChangeParentPom.java:413`). Legacy bakes injected props only into **project** poms. | **FIX** `mergeProperties` overlays an injected key only where `requested.getProperties()` already carries that value (i.e. project poms). Fixture harness updated to bake injected props like `MavenParser`/`ParityHarness`. |
| 4 | quarkus `version.compiler.plugin` diff (deep BOM lineage) | An imported BOM resolved **during `resolveDependencies`** (`mergeDependencyManagement → Pom.resolve`) hit the facade with `DISPATCHING=false` and got shadow-compared — but Phase 2 says dependency resolution stays 100% legacy. | **FIX (wiring)** `MavenEngineResolution.withoutEngine(...)`; `ResolvedPom.resolveDependencies`/`resolveDirectDependencies` run the whole pass legacy-only. |
| 5 | `dependencyManagement[N].bomGav: <bom> != null` (Quarkus platform g:a:v as `maven.config` props) | `BomGavAttributor.findServedKey` matched an imported BOM by its **raw** `${…}` coordinates against interpolated `servedBy` keys. | **FIX** interpolate the import's coordinates through the merged properties before matching. |
| 6 | `outcome: engine threw 'systemPath … is missing' / '… must specify an absolute path'` | Maven rejects a `system`-scoped dep with an absent or non-absolute `<systemPath>`; legacy tolerates. | **LEDGER** L-P2-E-001, outcome mask `outcome:system-scope-missing-path` (ALIGN_TO_MAVEN, flips at Phase 5). Classifier matches any `dependencies.dependency.systemPath` validation. |
| 7 | `dependencyManagement[N].version: "33.6.0-jre" != "latest"` | Legacy resolves a `latest`/`release` metaversion declared in `<dependencyManagement>` to a concrete version (nondeterministic); Maven — and the engine — keep the literal. Engine is Maven-correct. | **LEDGER** L-P2-E-002. New **value-conditioned** mask `metaversion:$.pom.dependencyManagement` (suppresses only a legacy-resolves/engine-keeps-literal flip; can never hide a real version divergence). |
| 8 | `sonar.host.url` profile prop: legacy stale (old) != engine (changed) | `UpdateMavenModel` never re-read `<profiles>` from the mutated document, so a profile-property edit (`ChangePropertyValue` in a `<profile>`) left legacy's marker stale; the engine reads the fresh document. | **FIX** `UpdateMavenModel` now re-reads each profile's `<properties>` (matched by id) on re-resolution. |
| 9 | `dependencyManagement[1000+]` gav-set superset (infinispan-marshaller-kryo, wsdl4j…) on `spring-boot-dependencies:3.2.4` / `spring-cloud-kubernetes:3.1.5` | The engine's effective DM for a directly-resolved transitive-import-heavy BOM contains a superset whose gav set matches an **older** nested import-BOM (Infinispan ~11 vs the 14 the parent pins) — the engine resolves a nested `${…}` import-BOM version wrongly. ~2000 diff lines. | **LEDGER (OPEN, not masked)** L-P2-E-003. A genuine engine BOM-version-resolution defect; masking would need to blank the whole DM and hide a real bug. Deferred to **collection / Phase 3** nested-import-BOM resolution (the other agent's domain). |

## Final full-suite verdict

- **LEGACY (default, gate 1): PASS.** Full `:rewrite-maven:test` — only 3 failures, all the environmental
  `repo.jenkins-ci.org` **HTTP 403** flakes (`UpgradeParentVersionTest.nonMavenCentralRepository`,
  `ChangeParentPomTest.{shouldNotAddToDependencyManagement,changeParentShouldResolveDependenciesManagementWithMavenProperties}`).
  These are pure download failures against a repo blocked in this sandbox — they fail identically on `main`. No
  functional regression from any change (including the legacy-path `UpdateMavenModel` profile sync).
- **SHADOW (gate 2): PASS except the 4 L-P2-E-003 tests** (`ChangeParentPomTest$RetainVersions.{bringsDownRemovedManagedVersion,
  bringsDownRemovedProperty,preservesOwnDefinedProperty}`, `UpdateMavenProjectPropertyJavaVersionTest.doNotCrashOnImplicitVersion`) +
  the same 3 network flakes + 1 test-infra flake (`verifyExternalParentPomHandling` — passes in isolation; a full-suite
  mock-parent ordering flake). `MavenParserTest` (111), all `parity.*`, and all `internal.engine.*` suites are **green in
  shadow** — no regression from the mapper/registry/attributor/oracle changes.
- **Gate 3:** every behaviour fix is pinned by the recipe tests that now pass in shadow (and the mapper/engine
  invariants by `MavenParserTest` + the parity suites); every new mask cites a ledger row.

The one remaining reproducible divergence (L-P2-E-003) is a deep nested-import-BOM version-resolution defect squarely in
collection/Phase-3 territory. It is deliberately **left red and unmasked** rather than papered over — masking it would
require blanking `$.pom.dependencyManagement` for a widely-used BOM and hide a real bug (violating "never weaken
parity-correct strictness").

## New ledger rows / masks (`doc/maven-resolution-ledger.md`, `parity/masks.txt`)

| id | class | mask / mechanism |
|---|---|---|
| L-P2-E-001 | ALIGN_TO_MAVEN | outcome mask `outcome:system-scope-missing-path` (systemPath absent or non-absolute) |
| L-P2-E-002 | ALIGN_TO_MAVEN | value-conditioned mask `metaversion:$.pom.dependencyManagement` (legacy resolves a DM metaversion; engine keeps the literal) |
| L-P2-E-003 | engine defect (OPEN, **not** masked) | nested import-BOM resolved to a wrong/old version in a large transitive-import BOM — collection/Phase-3 |

Mask mechanism gained one narrow capability: `ResolutionDiff.masked` now understands a `metaversion:<prefix>` mask that
additionally requires the engine (right) value to be a `latest`/`release` literal — a value condition that can only
suppress the intended flip, never a genuine version divergence.

## Files changed (all `rewrite-maven`)

Main: `internal/engine/PomXmlRegistry.java` (source-`Pom` correspondence), `MavenParser.java` + `UpdateMavenModel.java`
(registry `put` signature; profile-property re-read), `internal/engine/EffectivePomMapper.java` (empty-prop
representation via declaring RawPom, reactor-aware `declaringPom`, injected-property overlay guard, DM bom coordinate
interpolation wiring), `internal/engine/BomGavAttributor.java` (interpolate import coordinates), `internal/engine/
MavenEngineResolution.java` (`withoutEngine` dependency-resolution guard, systemPath outcome class, `get(ctx, requested)`),
`tree/ResolvedPom.java` (`resolveDependencies` legacy-only guard), `internal/parity/ResolutionDiff.java` (value-conditioned
`metaversion:` mask), `resources/parity/masks.txt`, `doc/maven-resolution-ledger.md`. Tests: `EngineFixtureHarness.java`
(bake injected props), `EngineReResolutionTest.java` + `MavenEngineResolutionTest.java` (new `put`/`get` signatures).
