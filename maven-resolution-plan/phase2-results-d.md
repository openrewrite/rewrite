# Phase 2 slice D — burn down the 31 open shadow findings to zero-unexplained on `MavenParserTest`

2026-07-08, worktree `maven-resolution` (uncommitted). Takes `MavenParserTest` in SHADOW from **clean 41 / path-masked
37 / open 31** to **open 0**: 22 findings fixed in the mappers/engine, 9 classified as Maven-identical strictness via
a new outcome-mask mechanism. All parity suites stay green in SHADOW; everything stays green in LEGACY/MAVEN.

## Headline census (`MavenParserTest`, SHADOW)

- **111 tests, 2 skipped, 109 run, 0 failures.** Down from 31 open findings to **0 unexplained**.
- Disposition of the 31: **22 code-fixed** (now clean / carried by a pre-existing path mask), **9 ledger-classified
  outcome mismatches** (new `outcome:*` masks). The pre-existing 41 clean + 37 path-masked carry forward unchanged.
- Parity suites in SHADOW: **green** (`DeterminismTest` 23, `IdentityContractsTest` 46, `EngineResolvedPomComparisonTest`
  **11/11** — 10 clean + `plugins-executions` explained, `SerializedLstCompatibilityTest` 6, synthetic.* all green).
- LEGACY (default): `MavenParserTest` 111, parity + engine internals (`Engine*`, `EffectivePomMapper*`, `BomGavAttributor*`,
  `ReactorWorkspace*`, `SettingsBridge*`), and MAVEN-mode `EngineReResolutionTest` all green.
- New pinning test `EngineEffectivePomProjectionTest` (4 tests, MAVEN mode, hermetic) pins the mapper/engine fixes directly.

## Per-finding disposition

### (iii) `${...}` projected gav — 11 findings — MAPPER FIX (`EffectivePomMapper.withInterpolatedGav`, ledger L-P2-D-007)

Root cause: the mapper set `ResolvedPom.requested` to the raw requested pom, so `getGav()` kept `${revision}` /
`${project.version}` / g:a-as-property literal; legacy interpolates the projected gav once properties merge
(`ResolvedPom.Resolver` ~L448). Fix: interpolate the projected gav through `ResolvedPom.getValue` (same properties, same
result as legacy — including partial resolution of cyclic `${project.build.version}` and the declared-`project.version`
override). Tests: `propertyFromMavenConfig`, `propertyFromMavenConfigFromParentPomCanBeUsedInChild`,
`directoryStyleRelativePathWithCiFriendlyVersion`, `ciFriendlyVersionsStillWorkAfterUpdateMavenModel`,
`canConnectProjectPomsWhenUsingCiFriendlyVersions`, `cyclicPropertyReferenceDoesNotStackOverflow`,
`multiModulePropertyVersionShouldAddModules`, `groupIdAndArtifactIdAsProperties`, `circularMavenProperty`,
`multipleCiFriendlyVersionPlaceholders`, `multiModuleProjectVersionPropertyInInterModuleDependency`.

Several of these also needed the **raw/interpolated modelId reconciliation** below: `getModelIds()` are interpolated
g:a:v, but an `InputLocation`'s source modelId is the RAW g:a:v (holds `${...}`), so declared DM entries in a
`${revision}`/`${project.version}` pom failed to thread to their declaring instance (`requestedRef "inherited:*"`).
Fix: `EffectivePomMapper.lineageIdBySourceId` maps every source-id form (raw `toId` + interpolated) to its lineage id;
`knownModelIds` includes both spaces so declared repos/plugins in such poms are not dropped.

### (ii) DM type/classifier variant collapse — 1 finding — MAPPER FIX (`EffectivePomMapper`, ledger L-P2-D-006)

Root cause: **real Maven keeps every raw `<dependencyManagement>` entry** — its `DefaultModelNormalizer.mergeDuplicates`
dedups only `<dependencies>`, never `dependencyManagement` (verified in maven-model-builder 3.9.x source). Legacy keeps
one entry per `g:a:classifier:type` (a4 #5402). Not a Maven-semantics divergence — an output-shape projection. Fix: the
mapper dedups the projected DM by `g:a:classifier:type` first-wins (KEEP_REWRITE); `getManagedDependency` looks up by the
same 4-tuple, so duplicates carry no information. Test: `allDependencyManagementEntryVariants_allDependencyVariants`
(8 raw → 5 distinct) + `EngineEffectivePomProjectionTest.dependencyManagementDedupedByGroupArtifactClassifierType`.

### (iv) properties precedence — 1 finding — MAPPER FIX (`EffectivePomMapper.mergeProperties`, ledger L-P2-D-008)

Root cause: `MavenParser` bakes `.property(...)` values into every project pom at parse time (`putAll` override), so a
builder property beats a POM-declared property of the same name; the mapper overlaid injected props with `putIfAbsent`.
Fix: overlay with `putAll` (override). Test: `projectVersionPropertyOverriddenByBuilderProperty` +
`EngineEffectivePomProjectionTest.builderPropertyOverridesPomProperty`.

### (i-b) real engine gaps — 10 findings — ENGINE FIX

- **Reactor-sibling / inherited BOM import (8):** Maven consults the workspace for `scope=import` only via
  `WorkspaceModelResolver.resolveEffectiveModel`, which `ReactorWorkspace` defers (returns null), so an in-reactor BOM
  fell through to the repository and 404'd. Fix (mirroring parent resolution): `EngineModelResolver.resolveModel` serves a
  reactor member's raw XML (`ReactorWorkspace.reactorPomXml`, model-version-ensured) and lets the builder build the
  import; `CacheBridge.recordReactorServed` registers it in `servedBy` + the pom regions so `BomGavAttributor` /
  `EffectivePomMapper` attribute its gav→repo and thread its managed entries uniformly. `BomGavAttributor.inheritedImports`
  now walks the requested pom's ancestry (reactor + cache) so a BOM imported by a **parent** is attributed
  (`indirectBomImportedFromParent`). Tests: `DependencyManagement.{simple,withType,twoDependencyManagementEntries,
  twoDependencyManagementEntries_dependencyWithType,twoDependencyManagementEntries_twoDependencies}`, `parentNearerThanBom`,
  `indirectBomImportedFromParent`, and the my-app half of `allDependencyManagementEntryVariants_*` +
  `EngineEffectivePomProjectionTest.reactorSiblingBomImportResolvesAndAttributes`.
- **RELEASE/LATEST metaversion in a BOM import (2):** `CacheBridge.resolveHighestMatchingVersion` only handled ranges.
  Fix: resolve `RELEASE`/`LATEST` (case-insensitive, as legacy up-cases them) through the engine's `VersionResolver`
  (`system.resolveVersion`, metadata path). Tests: `latestOrReleaseVersionInDependencyManagement[latest|release]`.

### (i-a) parity-correct strictness — 9 findings — LEDGER-CLASSIFIED (new outcome masks)

The engine's `ModelBuilder` is Maven-identically stricter than rewrite's lenient snippet parser; legacy resolves the
effective pom, the engine throws exactly where Maven throws. Not weakened — the shadow comparator now classifies a
legacy-resolves/engine-throws mismatch and, when a ledgered `outcome:<category>` mask covers it, returns legacy's result
(the tolerance tests flip at the Phase-5 cutover). `MavenEngineResolution.classifyEngineStrictness` + `isOutcomeMaskLedgered`.

| category | ledger | tests |
|---|---|---|
| `outcome:self-parent` | L-P2-D-001 | `selfRecursiveParent` |
| `outcome:expression-cycle` | L-P2-D-002 | `selfReferencingPropertyDoesNotStackOverflow` |
| `outcome:parent-packaging` | L-P2-D-003 | `nestedParentWithDownloadedParent`, `repositoryWithPropertyFromParent`, `differentRangeVersionInParent`, `managedDependenciesInParentInfluenceTransitives` |
| `outcome:aggregator-packaging` | L-P2-D-004 | `circularImportDependency`, `childDependencyDefinitionShouldTakePrecedence` |
| `outcome:missing-dependency-version` | L-P2-D-005 | `invalidDirect` |

## New ledger rows / masks

`doc/maven-resolution-ledger.md`: L-P2-D-001..005 (ALIGN_TO_MAVEN, outcome masks, flip at Phase 5) and L-P2-D-006..008
(KEEP_REWRITE output-shape/parity projections, pinned by tests — no mask). `parity/masks.txt`: five new `outcome:*`
entries (the only new masks; all cite a ledger row).

## Files changed (all `rewrite-maven` main, no `rewrite-maven-engine` change)

`internal/engine/EffectivePomMapper.java` (gav interpolation, DM dedup, injected override, raw/interpolated modelId
reconciliation), `EngineModelResolver.java` (+reactor), `ReactorWorkspace.java` (`reactorPomXml`/`findReactorPom`),
`CacheBridge.java` (RELEASE/LATEST, `recordReactorServed`), `BomGavAttributor.java` (inherited imports),
`MavenEngineResolution.java` (outcome classification, `ensureModelVersion` package-visible). Tests:
`internal/engine/EngineEffectivePomProjectionTest.java` (new, 4). Resources: `parity/masks.txt`, `doc/maven-resolution-ledger.md`.

## Acceptance gates

| gate | result |
|---|---|
| a. previously-green suites green in LEGACY | PASS (`MavenParserTest`, parity, engine internals) |
| b. parity suites green in SHADOW | PASS (only ledgered masks absorb diffs) |
| c. `MavenParserTest` in SHADOW: 0 open findings | PASS — clean/path-masked + 9 ledger-classified outcome masks |
| d. fixture differential `EngineResolvedPomComparisonTest` | PASS 11/11 (10 clean + `plugins-executions` explained) |
