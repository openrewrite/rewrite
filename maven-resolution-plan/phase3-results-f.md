# Phase 3 slice F — closing the OPEN engine gaps (metaversion, dedup arbitration, mediation residue)

2026-07-09, worktree `maven-resolution` (uncommitted). Charter: fix the remaining OPEN engine gaps so the shadow
census contains only flip-at-Phase-5 categories. One clean engine fix landed (direct-dependency metaversion resolution,
L-P3-E-004); the duplicate scope/classifier family (L-P3-E-005 / L-P0-001) was arbitrated against real-Maven ground
truth and confirmed ALIGN_TO_MAVEN (engine already Maven-correct, legacy pins in place); the 14 `$.scopes` gav
divergences were each extracted and arbitrated — most are genuine transitive-closure value divergences left OPEN
(drop-safety), one root-caused to a new engine bug (L-P3-E-006) whose blunt fix regressed 81 tests and was reverted.

## Final gates

| gate | result |
|---|---|
| LEGACY full `:rewrite-maven:test` | **PASS — 1583 tests, 0 failures** (network flakes passed this run) |
| SHADOW full `:rewrite-maven:test` | **101 failing** (slice-E baseline 108 → **−7**; oscillates 100–102 on the documented ±5-10 flake shuffle). Every residual maps to a root-caused, ledgered class (breakdown below) |
| engine + parity unit suites (LEGACY) | **PASS** (`internal.engine.*`, `EngineDependencyGraphComparisonTest`, `EngineResolvedPomComparisonTest`, `IdentityContractsTest`) |
| MAVEN-mode worst-3 rate | **143/185 (77%)** — `MavenParserTest`(111) + `ChangeDependencyGroupIdAndArtifactIdTest`(60) + `tree.ResolvedPomTest`(14); unchanged from slice E (the worst-3 classes are not metaversion-heavy) |
| metaversion pin (hermetic) | **PASS** — `MetaversionResolutionTest` (MAVEN mode, mock metadata) |

## Item 1 — L-P3-E-004 direct-dependency metaversion — FIXED

**Root cause.** The effective model keeps a `RELEASE`/`LATEST` dependency version literal (Maven defers concrete
selection). `EngineDependencyCollector.collectRequest` seeded the literal, so `EngineDescriptorReader` requested
`guava-latest.pom` → 404 → the direct dependency failed the collect (`Unable to download POM: …:latest`). Legacy resolves
the metaversion through merged metadata before download; Maven's `DefaultArtifactDescriptorReader` resolves it via
`versionResolver` (which the engine's custom descriptor reader replaces, so the resolution was never happening).

**Fix.** `collectRequest` resolves a `RELEASE`/`LATEST` seed through `CacheBridge.resolveHighestMatchingVersion`
(the same merged-metadata path parents/BOMs already use — `RELEASE`→`<release>`, `LATEST`→`<latest>`) before seeding, so
the collected node carries the concrete version. On resolution failure it leaves the literal (aether then fails exactly
as before — no regression). The `requested` still threads the raw declared metaversion; only the resolved gav is
concrete, matching legacy.

**Verification.** `UpgradeDependencyVersionTest.doesNotClobber{Direct,Managed}LatestWithSpecificVersion` and
`AddDependencyTest.doesNotClobber*` now green in SHADOW. New hermetic pin `MetaversionResolutionTest`
(`releaseMetaversionResolvesToReleaseVersion`→1.5, `latestMetaversionResolvesToLatestVersion`→2.0) runs the engine in
MAVEN mode against a mock `maven-metadata.xml` distinguishing `<release>` from `<latest>`. `retainReleaseOrLatestIfUsed`
is no longer a metaversion failure — it reduces to a clean L-P3-E-005 scope-dedup ALIGN case (lombok `RELEASE`
compile + `LATEST` test → Maven keeps the last/test declaration).

## Item 2 — L-P3-E-005 / L-P0-001 duplicate scope/classifier — ARBITRATED (ALIGN, no engine change)

Ground truth (`mvn 3.9.14 dependency:tree`, isolated LRM):
- **compile+test guava duplicate** (same g:a:type:classifier) → Maven WARNS "must be unique" and keeps the **last**
  (test). Engine-identical; legacy keeps both/compile. RESOLUTION semantics → **ALIGN_TO_MAVEN**.
- **jar+war** (different type) → Maven keeps **both**. Engine-identical; legacy type-filter drops war. **ALIGN**.
- **plain+classifier** guice/guice:no_aop → Maven keeps **both**. Engine-identical; legacy g:a-dedup drops one. **ALIGN**.

All three are RESOLUTION-semantics questions (which coordinates end up in the graph), so Maven wins and the engine is
already correct — no engine change. The current legacy behavior is pinned by tests that are **green in LEGACY today** and
flip to Maven-identical at Phase 5: `MavenParserTest.lastListedDependencyIsUsedForScope` (per-scope last-wins),
`IdentityContractsTest.classifierShadowedByGaDeduplication` (classifier), `RemoveDuplicateDependenciesTest.keepDependencyWithType`
(type filter). Ledger row L-P3-E-005 updated with the ground-truth verdict.

## Item 3 — the 14 `$.scopes` gav divergences — arbitrated individually

| test | shape | verdict |
|---|---|---|
| `MavenParserTest.Profiles.activeByDefault…` | same set, 2 deps swapped | ALIGN: active-profile ordering (L-P2-B2-002) |
| `MavenParserTest.repositoryWithPropertyPlaceholder` | engine +`persistence.core` in compile | **engine bug L-P3-E-006** (below) |
| `MavenParserTest.repositoryWithPropertyPlaceholder` root cause | moxy 4.0.0 declares `persistence.core` compile-then-test; ModelNormalizer keeps last (test), engine's `matchDeclared` reads first (compile) | Maven==legacy → engine bug |
| `ChangeParentPomTest.{shouldNotAddToDependencyManagement,changeParentShouldResolve…}` | engine +`jenkins-war/*`, legacy +`xerces`; `.repo` trailing-slash `maven2/`≠`maven2` | jenkins-ci network (legacy 403s on the war closure); membership is environment-dependent, not a resolution bug |
| `MavenParserTest.twoDependencyManagementEntries_twoDependencies` | version mediation (`aether-util:1.7`≠`1.13.1`) + engine +`maven-site-plugin` | genuine mediation value divergence — OPEN |
| `MavenParserTest.rewriteCircleci`, `ChangeDependencyGroupIdAndArtifactIdTest.changeOnlyArtifactId` | legacy +`jetbrains:annotations`, engine +`kotlin-stdlib-common` | kotlin-stdlib closure mediation — OPEN |
| `MavenParserTest.allDependencyManagementEntryVariants…` | engine +`io.flux-capacitor:common` | mediation/closure — OPEN |
| `ExcludeDependencyTest.excludeAlsoWhereConflictOmitted`, `excludeJUnitVintageEngineSpringBoot2_3` | legacy keeps jackson/httpclient/slf4j that engine prunes | exclusion×conflict-omission mediation — OPEN |
| `ChangeManagedDependencyGroupIdAndArtifactIdTest.doesNotMakeChange…`, `ChangeDependencyGroupIdAndArtifactIdTest.managedToUnmanagedExternalizedDepMgmt` | legacy keeps zipkin/bouncycastle/spring-cloud-context that engine prunes | spring-cloud closure mediation — OPEN |
| `ChangeDependencyGroupIdAndArtifactIdTest.{changeGroupIdOnWildcardArtifacts,doNotChangeUnless…}` | `org.apache.commons:commons-io`≠`commons-io:commons-io` | recipe re-resolution: the two paths resolve different post-edit coordinates — OPEN |

**L-P3-E-006 (new engine bug, root-caused).** `DependencyGraphMapper.matchDeclared` returns the *first* raw declaration
of a coordinate; Maven's `ModelNormalizer` (and legacy) keep the *last* when a transitive pom declares the same
g:a:type:classifier twice (moxy: `persistence.core` compile-then-test → effectively test → excluded from compile). The
engine threads the first (compile) and over-includes it. A blunt last-wins fix to `matchDeclared` **cleared moxy but
regressed 81 tests** — real-world poms (petclinic/logback) where legacy's `getDependencyScope` coincides with first-wins,
and `matchDeclared` is shared with version-threading. Reverted; ledgered OPEN as L-P3-E-006. A correct fix must dedup
*only genuine intra-pom g:a:type:classifier duplicates* and *only at the scope lookup*, not touch the version-threading
path — deferred (localized but out of this slice's safe budget).

The remaining mediation divergences are genuine version/conflict/exclusion-closure value differences between legacy's
walk and aether's; per the L-P2-E-003 drop-safety precedent they are left **red and unmasked** (a mask could hide a
drop). Arbitrating a cleanfix for each is per-case mediation work spanning multiple slices.

## Final SHADOW census (101, every entry ledgered; none masked)

| class | count | ledger | flip-at-P5? |
|---|---|---|---|
| synthetic/infra (request-log-exact + HEAD-failing test-shape: Auth/NegativeCaching/HtmlIndex/Snapshot/Mirror/RecipeBundle/Marketplace/PomDownloader/ArtifactDownloader/LocalCache) | 32 | test-shape artifacts of shadow byte-fetch, fail at HEAD too | no (not parity value divergence) |
| ALIGN classifier/type/scope dedup (tests + `classifiers` fixture) | 20 | L-P0-001 / L-P3-E-005 | **yes** |
| gav-mediation residue (incl. L-P3-E-006 moxy; excl. jenkins) | 13 | L-P3-D-003(b) / L-P3-E-006 | no (genuine value divergence) |
| error-shape / strictness outcome | 12 | L-P3-C-005, L-P2-D-005 | partial (strictness flips; message-shape is normalization) |
| other (recipe re-resolution / misc `ChangeDependencyGroupIdAndArtifactId`) | 10 | recipe-path re-resolution mediation | no |
| ALIGN profile-activation (fixture + `activeByDefault`) | 9 | L-P2-B2-002 | **yes** |
| gav-mediation / jenkins network | 3 | L-P3-D-003(b) | no (environment) |
| engine-tolerate outcome (FileRepo/PomlessJar) | 3 | L-P2-D-003/005 | partial |

The "only flip-at-P5" target is **not** fully met (matching slice-D/E's honest stance): ~29 failures are clean
flip-at-P5 ALIGN pins (dedup + profile), the strictness/tolerate rows partly flip, but ~32 synthetic request-log/HEAD
artifacts, ~16 genuine mediation value divergences, and ~10 recipe re-resolution divergences remain. None is masked; all
are root-caused and ledgered.

## Files changed (all uncommitted)
- `internal/engine/EngineDependencyCollector.java` — metaversion seed resolution (`collectRequest` +
  `CacheBridge.resolveHighestMatchingVersion`, `isMetaVersion` helper). (`DependencyGraphMapper.java` last-wins change
  reverted — no net mapper diff.)
- test `parity/synthetic/MetaversionResolutionTest.java` (new, hermetic L-P3-E-004 pin).
- `doc/maven-resolution-ledger.md` — L-P3-E-004 → FIXED (slice F); L-P3-E-005 ground-truth verdict + legacy pins;
  new L-P3-E-006 (transitive-duplicate scope mis-read).
