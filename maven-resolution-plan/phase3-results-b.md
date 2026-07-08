# Phase 3 slice B — dependency graph mapper + L-P2-E-003

2026-07-08, worktree `maven-resolution` (uncommitted). New classes in `org.openrewrite.maven.internal.engine`
(rewrite-maven), projecting the slice-3A `EngineCollectOutcome` into the frozen per-scope `ResolvedDependency` lists.
No facade wiring (3C), no tree-type signature changes. Scoped builds green: `:rewrite-maven:compileJava`, the
`parity.*` + `internal.engine.*` packages (215/215), `MavenParserTest` in shadow (102/102), `licenseMain`/`licenseTest`.

## Deliverables

- **`DependencyGraphMapper`** — `map(EngineCollectOutcome, ResolvedPom engineResolvedPom, Pom requested, ExecutionContext)
  → Map<Scope, List<ResolvedDependency>>`. Per scope: root seeding matches the legacy filter
  (`dScope == scope || dScope.transitiveOf(scope) == scope`, root propagation scope `Compile`); winner-only BFS (loser
  nodes with `NODE_DATA_WINNER` skipped, so each node links to its single nearest-declaring parent — the legacy
  "already-resolved → no second edge" shape); depth = winner-tree BFS depth, order = depth then first-encounter;
  `requested` threads the requested-pom `Dependency` instance at depth 0 and the declaring pom's declared coordinates
  (pre-management version via `getPremanagedVersion`) transitively; `gav.version = getBaseVersion()`; `repository` from
  `servedBy`; licenses from the cached declaring `Pom`; optional/type/classifier from the requested; NO type filter
  (test-jar/war resolve). Instances shared within a scope (single `Node → ResolvedDependency` map), never across scopes
  (fresh tree per scope). Direct failures aggregated legacy-shaped (deduped by root-GA + failed-GAV, the failing scope
  omitted) into `MavenDownloadingExceptions` with `partialResult` (L-P0-004).
- **`ExclusionAttributor`** — `effectiveExclusions` post-pass: walks each scope tree accumulating declared exclusions
  root→leaf, matches each node's declaring-pom dependencies (Maven exact/`*`), attributes the pruned coordinate to the
  shallowest declaring ancestor.
- **Tests**: `EngineDependencyGraphComparisonTest` (fixture differential, `$.scopes`), `DependencyGraphMapperTest`
  (identity probes, membership matrix, partial-failure shape), `NestedImportBomTest` (L-P2-E-003 pinning).

## Fixture differential — `$.scopes` (all 12 fixtures, legacy vs collect+map)

Full `ResolutionSnapshot` `$.scopes` diff, ZERO unexplained. New fixture `scope-matrix` (all four scopes + optional +
provided) added; its four-scope membership matches legacy exactly.

| fixture | `$.scopes` result | ledger |
|---|---|---|
| bom-import-{single,multi}, conflict-{equal,unequal}-depth, exclusions, optional, parent-chain, plugins-executions, property-indirection, scope-matrix | identical to legacy | — |
| classifiers | engine resolves BOTH `multi:1.0` and `multi:1.1:tests`; legacy dedups direct deps by g:a (last wins) → only `multi:1.1:tests`. Every scope list gains the no-classifier entry | L-P0-001 |
| profile-activation | active-profile deps ordered by profile DECLARATION order (Maven: `jdk-dep` then `explicit-dep`); legacy reverses profiles (a1 §1.3) → `explicit-dep` then `jdk-dep`. Pure reorder across all four scopes | **L-P2-B2-002 (new)** |

New ledger row **L-P2-B2-002** (ALIGN_TO_MAVEN): active-profile dependency ordering. Engine follows Maven's
`DefaultProfileInjector`; legacy's profile reversal is the outlier.

## Identity results (`DependencyGraphMapperTest`)

- depth-0 `requested` is `== ` an element of `pom.getRequestedDependencies()`; `getResolvedDependency` resolves it by
  reference (to some scope's instance — it scans scopes in reverse ordinal, so the depth-0 `requested` being SHARED
  across scopes means it returns the highest-scope instance, as legacy does).
- within-scope sharing: a parent's nested child `==` the flat-list instance.
- cross-scope non-sharing: same coordinate, `!=` instance per scope.
- partial-failure: a missing test-scoped direct dep throws `MavenDownloadingExceptions` (one exception, deduped),
  `partialResult` carries Compile/Runtime/Provided fully populated, Test omitted — mirrors
  `PartialFailureCompleteModelTest`.

## L-P2-E-003 — root cause + fix

**The premise was inverted.** It is NOT a nested-import-BOM version bug. Both engines resolve
`infinispan-bom:14.0.27.Final` (the version spring-boot-dependencies:3.2.4 pins via `infinispan.version`). Ground truth
on the actual `child`-with-parent-3.2.4 pom: **engine DM = 1466 entries, legacy DM = 1462 — a strict +4 superset**, the
extra four being `org.infinispan:infinispan-marshaller-{kryo,kryo-bundle,protostuff,protostuff-bundle}:14.0.27.Final`,
all verified present in infinispan-bom:14.0.27.Final. (The earlier "engine resolves an OLDER version" reading was a
position-shift artifact: +4 inserted entries shift ~2772 index positions.)

**Root cause**: those four live in infinispan-bom's `<profile id="community">`, activated by a property-value NEGATION
(`<name>release-mode</name><value>!downstream</value>` → active when `release-mode` is not `downstream`, i.e. when
unset). Maven's model builder activates it and includes them (Maven-correct). Legacy's profile activation reads
`System.getenv` (a1 §2.3) and drops them. So **the engine is Maven-correct; legacy is the outlier** — an ALIGN_TO_MAVEN
flip in the DESIGN §9 profile-activation class, not an engine defect.

**Fix**:
1. `ResolutionDiff` now aligns `dependencyManagement` by `gact` (it is a gact-keyed, gact-sorted collection on both
   sides), so a set difference is 4 discrete entries, not a positional cascade. For equal sets this is identical to the
   index diff; MavenParserTest shadow (102) and the parity suites are unchanged.
2. New **directional** mask `dm-superset:$.pom.dependencyManagement` (masks.txt, cites L-P2-E-003): suppresses only
   entries the ENGINE adds that legacy dropped (`left == <missing>`). It can never hide a value divergence on a shared
   gact nor an engine DROP — the narrowest mask that admits this Maven-correct superset.
3. Ledger L-P2-E-003 reclassified `engine defect (OPEN)` → `ALIGN_TO_MAVEN` with the profile-activation root cause.

**Proof**: the four shadow tests are green with `-Dorg.openrewrite.maven.resolution.engine=shadow`:
`ChangeParentPomTest$RetainVersions` (6/6, incl. `bringsDownRemovedManagedVersion`, `bringsDownRemovedProperty`,
`preservesOwnDefinedProperty`, `dependencyWithExplicitVersionRemovedFromDepMgmt`) and
`UpdateMavenProjectPropertyJavaVersionTest.doNotCrashOnImplicitVersion`. (The two `…ExternalParentPom…` failures that
surface only when that class runs together are the pre-existing mock-parent/scratch-LRM ordering flake from
phase2-results-e — each passes in isolation.) Hermetic regression:
`NestedImportBomTest.profileNegationActivatedImportsAreIncluded` pins the engine including a property-negation-activated
profile's managed entries from a transitively imported BOM.

## Ledger rows added / changed

- **L-P2-B2-002** (new, ALIGN_TO_MAVEN): active-profile dependency ordering.
- **L-P2-E-003** (reclassified engine-defect → ALIGN_TO_MAVEN): profile-activated managed entries in a transitively
  imported BOM; mask `dm-superset:$.pom.dependencyManagement`.
- L-P0-001 (classifier dedup) and the type-filter removal are exercised by the mapper as built (classifier flip cited on
  the `classifiers` fixture; no fixture exercises non-jar types).

## datedSnapshotVersion (L-P0-005) — reproduced; consumer findings

The mapper reproduces legacy's shape: a REMOTE pom's `datedSnapshotVersion = artifact.getVersion()` (the dated form for
a snapshot, the plain version DUPLICATED for a release); a project/reactor pom (null repository) → `null`. Consumers of
`getDatedSnapshotVersion()`:

- **Artifact cache / download path** (`MavenArtifactCache:56-58`, `LocalMavenArtifactCache:87`,
  `MavenArtifactDownloader:102`, `MavenRecipeBundleResolver:62`): all collapse `dated == version` to the plain-version
  behavior (`MavenArtifactCache` explicitly `datedSnapshotVersion == null || version.equals(datedSnapshotVersion)`). The
  duplicate-on-release shape is **harmless** here.
- **Search data tables** (`DependenciesInUse`/`DependenciesDeclared`/`ExplainDependenciesInUse` via `FindDependency`,
  `DependencyInsight`, gradle `DependencyInsight`): emit `datedSnapshotVersion` as a column documented "null if not a
  snapshot". A release with `dated == version` therefore surfaces the version instead of null — the **only wire-visible
  consumer** of the L-P0-005 shape. Reproducing legacy keeps parity with current data-table output; the flip decision
  stays OPEN.
