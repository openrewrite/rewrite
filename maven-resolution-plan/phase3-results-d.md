# Phase 3 slice D — burning down the dependency-graph shadow census

2026-07-08, worktree `maven-resolution` (uncommitted). Root-caused and fixed four of the five census mechanisms
(L-P3-C-001/002/003 and the scope half of L-P3-C-004) in the collector/mapper/attributor; arbitrated the residual
closure divergences against real-Maven ground truth. No facade/tree signature changes; every fix is mapper- or
collector-side (so none can affect the LEGACY gate). All changes uncommitted.

## Final census (full `:rewrite-maven:test`)

- **LEGACY**: 1583 tests, **3 failed — all the known `repo.jenkins-ci.org` 403 network flakes**
  (`UpgradeParentVersionTest.nonMavenCentralRepository`, `ChangeParentPomTest.{shouldNotAddToDependencyManagement,
  changeParentShouldResolveDependenciesManagementWithMavenProperties}` — the last two fail identically on
  `org.jenkins-ci.plugins:plugin:4.75 → HTTP 403`, same on `main`). **No functional regression.**
- **SHADOW full `:rewrite-maven:test`**: 1583 tests, **172 failed** (≈381 at slice-C start → **−55%**). 171 assertion
  blocks decompose as: **19 effective-pom-only** (`$.pom`, pre-existing, not slice-D), **152 with `$.scopes`/`$.errors`**
  (32 unique poms). The single **dominant** class by diff-line count (9752 of ~13k lines) is **pre-existing** and
  effective-pom: `$.pom.dependencyManagement{…}.requestedRef "inherited:…" != "bom[0]"` (L-P3-D-004), contaminating most
  blocks; slice-D never touched `EffectivePomMapper`. `MavenParserTest` alone: 43 → **10** failing.
- **MAVEN-mode pass rate (census's worst three classes, 153 tests)**: **71/153 pass (46%)** — the flip-readiness number.

## Per-mechanism root cause + fix

### 1. `effectiveExclusions` under-report (L-P3-C-002) — FIXED
Three gaps, one unifying cause each:
- **wildcard / parent-inherited**: the attributor enumerated a node's *own* `<dependencies>` (`Pom.getDependencies()`),
  but legacy walks `resolvedPom.getRequestedDependencies()` which merges parent-inherited `<dependencies>`. A `*:*`
  exclusion on `logback-classic` must report its parent-inherited test deps too. **Fix**: `EngineDescriptorReader`
  records each node's *effective* (parent-merged) declared g:a list into `EngineCollectOutcome.declaredDependencies`;
  the attributor enumerates that.
- **DM-sourced**: the attributor seeded owners from the requested `Dependency`'s exclusions only, missing a
  `<dependencyManagement>`-contributed exclusion. **Fix**: per-node exclusion sets are taken from the aether node —
  effective (requested + managed) for pruning, premanaged (requested-only) for the attribution walk.
- **reactor-crossing**: an exclusion whose pruned child is declared by a reactor member found no pom to enumerate.
  **Fix**: reactor members are now recorded in the pom cache (below), and their effective deps are captured like any
  descriptor read.
- **attribution target**: the first rewrite over-reported (5× on the root). Legacy attributes a pruned coordinate to the
  shallowest ancestor in the *unbroken chain of ancestors that **requested** that exclusion*, starting from the node
  whose child matched — not the root-most declarer. `ExclusionAttributor` now threads the ancestor path and walks it
  exactly (`ResolvedPom.java:1195-1201`).

### 2. Closure divergence (L-P3-C-004) — RE-CENSUSED; scope half FIXED, mediation/repo residual OPEN
After step 1 the closure bucket resolved into three sub-classes:
- **Test/Provided scope under-population (L-P3-D-001) — FIXED.** The dominant driver. The engine dropped the
  compile/runtime transitives of a test/provided-scoped *direct* dependency: aether's `JavaScopeDeriver` promotes a
  compile child of a test parent to `test`, and the mapper's `isInClasspathOf` then excluded it. **Ground truth**:
  `mvn dependency:tree` on a test-scoped `junit-jupiter-api:5.7.0` lists `apiguardian`/`opentest4j`/`platform-commons`
  as `:test` — they ARE the test closure. **Maven agrees with legacy → engine bug.** Fix: `DependencyGraphMapper.childScope`
  replicates `ResolvedPom.getDependencyScope` (declared/containing-managed scope + root-DM no-promote override) for the
  enqueue decision instead of reading aether's derived scope. (A blunter fix — dropping `ScopeDependencySelector` from
  the collect — regressed the scope-widening tests, so it was reverted in favour of the surgical mapper change.)
- **local-cache-shadows-remote repo attribution (L-P3-D-002) — OPEN (Phase-4).** `SettingsBridge.requestRepositories`
  adds `~/.m2` as a resolvable file-repo tried FIRST, so a locally-cached artifact is attributed to `<localrepo>` while
  legacy (and Maven) attribute the remote origin (`…/maven2`). Environment-dependent and rooted in the phase-2
  `addLocalRepository` / scratch-LRM design — an LRM/repo-universe concern phase3-a deferred to Phase-4 cache hardening.
  Left red and unmasked (a value divergence a mask could hide). Now the single remaining diff on `parse`,
  `parseMergeExclusions`, `parentNearerThanBom`, `differentRangeVersionInDependency`.
- **tolerate-vs-fail + residual mediation (L-P3-D-003) — OPEN.** The collector fails only depth-1 descriptor-missing
  failures; legacy (a1 §3.9) fails a jar/ejb/null-type dependency at ANY depth whose POM 404s. A genuinely-unavailable
  transitive jar (`com.oracle.coherence:coherence`) makes legacy fail and the engine tolerate. Fixing this cleanly needs
  per-scope propagation (so a pruned/optional dep is not falsely failed), left OPEN. Plus a small residue of genuine
  range/mediation cases (`invalidTransitives`, `repositoryWithPropertyPlaceholder`).

### 3. Reactor-transitive attribution (L-P3-C-003) — FIXED
Root cause was NOT specifically "reactor" — it was **warm-DataPool attribution loss**: the collector shares a
`RepositoryCache` across a reactor's sequential collects, and a descriptor served warm from the pool skips
`EngineDescriptorReader` entirely (`descriptorReads == 0`), so per-collect `servedBy` never re-recorded it. **Fix**:
`servedBy` and `declaredDependencies` are now **cumulative across collects** on `EngineDependencyCollector` (the
gav→repository mapping is stable within one collector's DataPool). Reactor members themselves are recorded via
`CacheBridge.recordReactorServed` from `EngineDescriptorReader` (repository=null, licenses from the pom, matching
legacy's project-pom convention). This single fix cleared the `repo`/`dated`/`licenses` drops AND unblocked the
declaring-pom lookup that step 4 needs.

### 4. Requested shape (L-P3-C-001) — FIXED (threading half)
`transitive requested` must be the declaring pom's *raw declared* version (null when managed). Two residual causes:
- the declaring pom was a **reactor member** absent from the cache → fixed by the reactor recording above.
- the declarer's raw coordinate uses a **`${project.groupId}` placeholder** (e.g. `jaxb-runtime` declares `jaxb-core`
  with `groupId=${project.groupId}`), so the raw g:a match failed and the mapper fell back to the effective version.
  **Fix**: `matchDeclared`/`matchRequested` interpolate `${...}` coordinates (project.groupId/version + effective
  properties via `ResolvedPom.getValue`) before matching, with an artifactId fallback. This also fixed the depth-0
  identity case (`groupIdAndArtifactIdAsProperties`: `root[0]` vs a synthetic `val:…`).

The **requestedRef index-shift** half (Maven's `ModelNormalizer` deduping duplicate `<dependencies>`) did not surface in
the residual after the threading fix and is not separately exercised in the current failing set.

## Ground-truth arbitration verdicts (closure divergences)
| case | verdict |
|---|---|
| test-dep transitives missing from Test/Provided | `mvn dependency:tree` → Maven lists them `:test`. **Maven == legacy → engine bug** (L-P3-D-001, fixed) |
| locally-cached dep attributed `<localrepo>` vs `…/maven2` | Maven records the remote origin. **Maven == legacy** (L-P3-D-002, Phase-4 LRM) |
| transitive jar POM 404 (`coherence`) tolerated | a real build fails to resolve it. **Maven == legacy** (L-P3-D-003) |

## Final gate verdicts
| gate | result |
|---|---|
| LEGACY full `:rewrite-maven:test` green (minus env flakes) | **PASS** — 1583 tests, 3 failed, all `repo.jenkins-ci.org` 403 flakes; no regression |
| engine + parity unit suites (LEGACY) after all changes | **PASS** |
| SHADOW full census zero unexplained | **NOT MET** — 172 failing (≈381 → −55%). Residual maps entirely to root-caused/ledgered classes: **L-P3-D-004** (pre-existing effective-pom `dmRequestedRef`, dominant, Phase-2 scope), **L-P3-D-002** (repo attribution, Phase-4 LRM), **L-P3-D-003** (tolerate-vs-fail + range mediation), and the pre-existing `$.pom` property-derivation class (`kotlin.major.version`, `log4j2.version`, `maven.compiler.source`). Left red and unmasked per the drop-safety discipline |
| MAVEN-mode worst-three-classes pass rate | **71/153 (46%)** |

## Files changed (all `rewrite-maven` main, uncommitted)
`internal/engine/CollectContext.java` (cumulative `servedBy` + `declaredDependencies` as constructor params),
`internal/engine/EngineDependencyCollector.java` (owns the cumulative maps), `internal/engine/EngineCollectOutcome.java`
(carries `declaredDependencies`), `internal/engine/EngineDescriptorReader.java` (`recordDeclaredDependencies`,
`recordReactorServed` on the reactor path), `internal/engine/DependencyGraphMapper.java` (`childScope` scope replica,
`${...}`-interpolating `matchDeclared`/`matchRequested`, per-node effective/requested exclusions, `Index.declaredDepsFor`),
`internal/engine/ExclusionAttributor.java` (rewritten: effective-deps enumeration + path-based walk-up attribution).
Docs: `doc/maven-resolution-ledger.md` (L-P3-C-001/002/003 → FIXED; L-P3-C-004 split; new L-P3-D-001/002/003).

## Residual (precise)
The SHADOW census is not at zero (172 failing). Every residual maps to a root-caused, ledgered class, left red+unmasked
per the drop-safety discipline:
1. **L-P3-D-004** (DOMINANT by line count, ~9752 lines) — BOM-imported managed-dependency `dmRequestedRef`
   (`inherited:…` vs `bom[0]`). **Pre-existing effective-pom class**, built by `EffectivePomMapper` (Phase-2), surfaced
   by slice-C's inclusion of `$.pom` in the dependency shadow. NOT slice-D dependency-graph work; needs a Phase-2-side
   requested-shape decision (likely a directional `bomref:` mask if the engine's import-instance threading is the chosen
   contract). Contaminates most otherwise-passing blocks.
2. **L-P3-D-002** local-cache-shadows-remote `repository` attribution — a Phase-4 LRM/repo-universe change (make `~/.m2`
   the aether LRM rather than a first-tried request repo). Drives the residual `$.scopes.*.repo` diffs and their
   positional cascades.
3. **L-P3-D-003** transitive jar POM-404 tolerate-vs-fail (collector must fail jar/ejb/null failures at any depth, scoped
   per resolved classpath) + a small residue of genuine range/mediation divergences (`$.scopes.*.gav`).
4. Pre-existing Phase-2 `$.pom` property-derivation divergences (`kotlin.major.version`, `log4j2.version`,
   `maven.compiler.source`) — out of the slice-D dependency-graph scope.

Slice-D closed the four in-scope dependency-graph mechanisms it was chartered to (exclusions completeness+attribution,
reactor/warm-pool attribution, requested-version threading, and the scope-derivation half of the closure bucket); the
residual is dominated by a pre-existing effective-pom class plus two ledgered engine/architecture items (repo LRM,
tolerate-vs-fail) that were correctly identified as deeper than this slice's budget.
