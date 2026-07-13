# Phase 3 slice E — the three ledgered residual classes + census burn-down

2026-07-09, worktree `maven-resolution` (uncommitted). Root-caused and fixed the three chartered classes
(L-P3-D-002/003/004) plus three more classes the burn-down surfaced (L-P3-E-001/002/003). All fixes are engine-side
(CacheBridge / collector / mappers / facade); the LEGACY path is untouched by construction and the LEGACY gate is
fully green. The gates are now machine-independent: the shadow census no longer depends on `~/.m2` contents.

## Final gates

| gate | result |
|---|---|
| LEGACY full `:rewrite-maven:test` | **PASS — 1583 tests, 0 failures** (this run even the 3 `repo.jenkins-ci.org` network flakes passed) |
| SHADOW full `:rewrite-maven:test` | **108 failing** (this machine's HEAD baseline: **175**, measured under identical conditions — populated `~/.m2`, same JVM). **Zero unexplained**: every residual maps to a root-caused, ledgered class (breakdown below). NOT zero-failing — see "Residual" for why the remaining classes were not masked |
| engine + parity unit suites (LEGACY) | **PASS** (218 tests, 0 failures) |
| MAVEN-mode worst-3-classes pass rate | **142/185 (77%)**, was 71/153 (46%) at slice D. Composition: `MavenParserTest` (111) + `ChangeDependencyGroupIdAndArtifactIdTest` (60) + `tree.ResolvedPomTest` (14) — the census's worst three by current failure count (slice D's 153-test composition was not recorded) |
| L-P3-D-002 machine-independence | **PASS** — full shadow `MavenParserTest` on this populated `~/.m2`: **zero** `$.scopes.*[i].repo` attribution diffs |

Baseline note: phase3-d reported 172 on its machine state; this machine's HEAD baseline measured 175. All deltas here
are against the locally-measured 175 (apples-to-apples).

## Per-class root cause + fix

### 1. L-P3-D-004 — `dmRequestedRef` `inherited:g:a` vs `bom[i]` (the volume leader) — FIXED
**Root cause was NOT BomGavAttributor membership-stamping.** The divergence only fires on a recipe's *re-resolution*:
`UpdateMavenModel` builds a re-mapped `requested` Pom (fresh `ManagedDependency.Imported` instances) and swaps it onto
the old `ResolvedPom` via `withRequested`; legacy's `ResolvedPom.resolve` no-change identity gate then returns the
CALLER's instance, whose DM entries still thread the PRE-edit declarations. Snapshotted against the new requested pom's
declaration index, legacy renders `inherited:g:a`. The engine's dependency-graph path rebuilt the effective pom fresh
and threaded the new instances (`bom[0]`) — "more correct", but a parity break with legacy's pinned no-change contract.
**Fix**: `MavenEngineResolution.attemptEngineGraph` threads against the caller's `ResolvedPom` whenever the engine's
threading inputs are value-equal (`threadingEqual`: requested dependencies by equals; managed entries aligned by
g:a:classifier:type comparing version/scope/exclusions — repository spelling and the threaded instances excluded, since
`~/.m2` vs `file:///…/.m2` spellings made full `ResolvedManagedDependency.equals` always miss). This also repairs
maven-mode reference-identity for callers (`getResolvedDependency` against the pom the caller actually holds).
The spring-boot-parent-style import-through-parent shape needed no separate fix: both engines resolve it identically
once the no-change contract is honored (verified on the `spring-boot-dependencies`-parent fixtures in the suite).

### 2. L-P3-D-002 — local-repo-shadows-remote `repository` attribution — FIXED
Legacy ground truth (`MavenPomDownloader`, a2 §1): a `file://` repo — `~/.m2` in particular — only *serves* a
jar-packaged artifact (packaging null/jar/bundle) when a non-empty sibling jar exists; a bare pom is skipped, so the
artifact is served AND attributed from the remote. When the jar IS present locally, legacy serves locally and
attributes `MAVEN_LOCAL_USER_NEUTRAL`. The engine's `CacheBridge` attributed whichever repo served bytes, so a
pom-only `~/.m2` entry (metadata-only priming, `mvn dependency:resolve` leftovers) flipped attribution — 19 failures on
a populated `~/.m2`, ~5 on a clean one. **Fix**: `CacheBridge.resolvePom` applies legacy's exact gate
(`jarPresentForJarPackaging`: parse packaging from the served bytes, jar-packaged requires a non-empty sibling jar,
pom/other packaging exempt) to every file repo before serving/attributing. The census stops depending on `~/.m2`.

### 3. L-P3-D-003 — transitive jar POM-404 tolerance depth — FIXED (mediation residue OPEN)
Arbitration (phase3-d, a4 #64): legacy AND real Maven fail a jar-type dependency whose POM 404s at ANY depth. **Fix**:
`EngineDependencyCollector.outcome` now fails a missing descriptor at any depth when the node is a *resolved-graph
member*: jar/ejb/empty type, non-optional, and a conflict-WINNER (losers are superseded versions legacy never
downloads — the first, blunter any-depth cut regressed 30+ tests by failing losers; the winner/optional/type guards are
the per-scope-safety the slice-D notes called for). The failure's `root` is its depth-1 ancestor, so the mapper fails
only the scopes that ancestor participates in. The `coherence` ground-truth case now matches legacy exactly
(`invalidTransitives`, `propertyFromMavenConfig`'s error set). Pin updated: `missingJarTransitiveFails` replaces
`missingTransitiveTolerated` (which pinned the pre-arbitration tolerance). The **mediation residue** (14 `gav`
divergences, e.g. `jaxbRuntime`-style angus/persistence ordering) shrank with the E-002 fix below; the remainder is
per-case range/mediation ground-truth work, left OPEN and red.

### Additional classes root-caused while burning down (all pre-existing, surfaced by the census)
- **L-P3-E-001 (FIXED)** — duplicate direct declarations: legacy retains BOTH duplicate g:a declarations of a pom
  (threading the resolved dep to the LAST), the mapper deduped by GA → `root[N] != root[0]` across dozens of blocks.
  `EffectivePomMapper.mergeRequested` now mirrors `mergeRequestedDependencies` exactly.
- **L-P3-E-002 (FIXED)** — parent-inherited transitive declarations: legacy's `getRequestedDependencies()` is
  parent-merged; the engine's `matchDeclared` read only the raw pom's own `<dependencies>` and fell back to aether's
  managed version (`val:jackson-core:null != val:jackson-core:2.17.1`). `DependencyGraphMapper` now walks the declaring
  pom's ancestor lineage (`Index.lineageFor`, child-wins) and interpolates placeholders through the lineage exactly as
  `ResolvedPom.getValue` (unresolvable stays literal). Also fixed several mediation-shape diffs (`jaxbRuntime`,
  `differentRangeVersionInParent`) as a side effect of correct declared-scope/version lookups.
- **L-P3-E-003 (FIXED)** — shaded super-POM default plugin groupId
  (`org.openrewrite.maven.engine.shaded.org.apache.maven.plugins`) stripped by `EffectivePomMapper.unshade`
  (phase2-b2 deviation #5, now closed).

## Residual (108, every entry ledgered; none masked)
Census decomposition (mechanically classified from the failure XMLs, one row per test):

| class | count | ledger | why not masked |
|---|---|---|---|
| engine-fails-legacy-tolerates | 15 | L-P2-D-003/005 via descriptor reads + pomless-jar/synthesized-pom (FileRepoTest family) | outcome differences on the graph path; a mask could hide a real failure regression |
| gav-mediation residue | 14 | L-P3-D-003(b) | genuine closure value divergences |
| request-log-exact | 10 | shadow-mode artifact: the engine's own byte fetches break legacy-exact request-log assertions (`NegativeCachingTest`, `AuthFallbackTest`, `SnapshotResolutionTest`); fail at HEAD too (verified individually) | test-shape, not parity; masks are value-level, not applicable |
| transitive-requested residue | 9 | L-P3-E-002 edge cases (lineage pom absent from servedBy) | value divergence |
| snapshot dated | 6 | L-P0-005 (Phase-3 decision pending) | value divergence |
| classifier dedup | 6 | L-P0-001 | ALIGN_TO_MAVEN, flips at Phase 5 |
| metaversion direct dep | 5 | L-P3-E-004 (new) | engine gap — needs the metadata path wired into dependency version resolution |
| profile order | 4 | L-P2-B2-002 | ALIGN_TO_MAVEN, flips at Phase 5 |
| error message shape | 3 | L-P3-C-005 | text normalization |
| type/scope duplicate dedup | 2 | L-P0-001 (type-filter) + L-P3-E-005 (new) | ALIGN_TO_MAVEN |
| pom-properties + misc `$.pom` | 4 | pre-existing Phase-2 property-derivation | value divergence |
| other (cross-test state, parameterized attribution shifts, marketplace jar-resolution family) | ~30 | members of the above classes surfacing through recipe re-resolutions | — |

"Zero unexplained" is met in the root-cause sense — every failing test's diff belongs to a ledgered, root-caused class —
but not in the zero-failing sense: the remaining classes are value divergences or outcome differences that the
drop-safety discipline forbids masking (L-P2-E-003 precedent), plus two ALIGN_TO_MAVEN flips deferred to Phase 5 by
design. The honest number is 175 → 108 (−38%) on identical machine state, with the three chartered classes at zero.

## Flakiness note (full-suite runs)
Full-suite shadow runs shuffle ±5-10 failures between parameterized fixture indices and exact-request-log tests
(engine prefetch interleaving + cross-test `~/.m2`/negative-cache state). Every individually-suspect flip was re-run
scoped: none is a deterministic regression from this slice (e.g. `propertyWithNullValue` passes scoped;
`deterministicNotFoundIsNegativelyCached`/`credentialsResolvedByServerId` fail identically at HEAD).

## Files changed (all uncommitted)
`internal/engine/CacheBridge.java` (file-repo jar gate), `internal/engine/EngineDependencyCollector.java`
(winner/optional/type-aware any-depth failure split + per-ancestor root attribution),
`internal/engine/DependencyGraphMapper.java` (declaring-lineage walk: `Index.lineageFor`, lineage-aware
`matchDeclared`/`transitiveRequested`/`childScope`, `getValue`-mirroring interpolation),
`internal/engine/EffectivePomMapper.java` (`mergeRequested` legacy-exact duplicate handling; `unshade`),
`internal/engine/MavenEngineResolution.java` (`threadingEqual` no-change contract on the graph path),
test: `EngineDependencyCollectorTest` (`missingJarTransitiveFails` pin). Docs: ledger rows L-P3-D-002/003/004 → FIXED,
new L-P3-E-001..005.
