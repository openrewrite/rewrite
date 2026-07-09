# Phase 3 slice G — the final census scrub (corpus gate's four NEW classes + census categories)

2026-07-09, worktree `maven-resolution` (uncommitted). Charter: close the corpus gate's four NEW divergence classes
(corpus-shadow-results.md) and the remaining non-flip census categories, targeting a census where every red entry is a
Phase-5 ALIGN pin, a documented-OPEN mediation divergence, or network. Four engine/input fixes landed (each pinned), the
synthetic request-log assertions were made shadow-aware, and the two remaining census classes (recipe re-resolution,
error-shape) were root-caused and left ledgered OPEN under the drop-safety discipline.

## Corpus reclassification (touched entries) — VERIFIED

Rerun `corpusRun -Pmode=replay -Pengine=shadow` against this worktree's Maven Local build (`8.87.0-SNAPSHOT`,
`[wiring]` line confirms the local jar), each entry replayed twice (deterministic, zero store misses):

| entry | before (slice F) | after (slice G) | disposition |
|---|---|---|---|
| **guava-33.0.0-jre** | UNEXPLAINED (NEW-4 srczip in requested) | **CLEAN** | NEW-4/NEW-2: pinned sources-free `java.home` → `<file><exists>` srczip JDK profile never activates → matches legacy's omission |
| **spark-core-2.13-3.5.0** | UNEXPLAINED, **1540** diff lines (NEW-3) | UNEXPLAINED, **6** diff lines | NEW-3 FIXED repo-mirror attribution (both sides now `…storage-download.googleapis.com…`); residual = L-P3-C-004 (protobuf-java:3.23.4 mediation drop) + L-P3-G-002 os-classifier |
| **netty-handler-4.1.104.Final** | UNEXPLAINED (many `<os>` lines) | UNEXPLAINED, **1** property | NEW-2: deterministic; residual `$.pom.properties.argLine.alpnAgent` — directional profile-activation ALIGN (engine Maven-correct) |
| **hadoop-client-3.3.6** | UNEXPLAINED (many `<os>` lines) | UNEXPLAINED, **1** property | NEW-2: deterministic; residual `$.pom.properties.build.platform` — directional profile-activation ALIGN |
| **apache-dubbo-3.2.10** | UNEXPLAINED (NEW-1 `${project.parent.version}` ×16 + residues) | UNEXPLAINED | NEW-1 FIXED (`project.parent.version` count in the diff **16 → 0**); residual = L-P3-C-004 (micrometer-core/LatencyUtils/slf4j-api drops) + L-P3-C-002 (zookeeper/HdrHistogram exclusions) — ledgered OPEN |

Net: **guava CLEAN** (10→11 corpus clean); spark/netty/hadoop collapsed to a minimal deterministic residue; dubbo's NEW
class eliminated. Every residual is a ledgered class (L-P3-C-004/C-002 mediation, or L-P3-G-002 directional ALIGN).

## Per-item disposition

**NEW-1 (L-P3-G-001) — `${project.parent.version}` interpolation — FIXED.** `DependencyGraphMapper.lineageProperty`
extended to the `project.parent.{groupId,artifactId,version}` (and `parent.*`) namespace, mirroring
`ResolvedPom.getProperty` exactly (reads `pom.getParent()`). Pinned by hermetic fixture `parent-version-expr` (a
transitive declared `<version>${project.parent.version}</version>`; both engines thread `2.5`, zero unexplained diff).
Corpus dubbo `project.parent.version` residual: 0.

**NEW-2 (L-P3-G-002) — `<os>`/`<jdk>`/`<file>` profile activation — EMBEDDER INPUT (flagged to Jon).** Added
`MavenExecutionContextView.setActivationSystemProperties(Map<String,String>)` — a resolution input like `activeProfiles`;
default `null` = host `System.getProperties()` (Maven parity, so no unit-suite behavior change). `EngineEffectivePom`
consumes it engine-side only (one knob, reversible). The corpus runner pins a canonical Linux/amd64/JDK-17 snapshot with
a sources-free `java.home` so os/jdk/file activation is byte-reproducible across machines. netty/hadoop/spark-os now
diverge from legacy by exactly the profile-injected properties (engine Maven-correct, legacy never activates). RECOMMEND
to Jon: a directional `$.pom.properties` ALIGN mask (L-P2-E-003 family) to reclassify these to masked-ALIGN; NOT added
pending sign-off (implemented reversibly per the note).

**NEW-3 (L-P3-G-003) — mirror attribution — FIXED.** Root cause: the collect resolved transitives through
`requested.getEffectiveRepositories()` (the requested pom's OWN repos only), so a parent-inherited repository (spark
declares `gcs-maven-central-mirror` before `central` in spark-parent) was absent from the request universe and bytes
were attributed to canonical Central. Fix: `MavenEngineResolution` sources the collect's request repositories from the
parent-merged effective-model repositories (`modelRepositories`, declaration order), dropping only canonical Central
(re-added by the `addCentralRepository` seam so a ctx-injected `central` — e.g. a mock — is not clobbered). spark diff
1540→6 lines; repo attribution matches legacy. No regression in the hermetic parity/engine suites.

**NEW-4 (L-P3-G-004) — system-scoped profile dependency in requestedDependencies — ARBITRATED (ALIGN, no mapper
change).** The srczip entry is `<file><exists>${java.home}/../src.zip</exists>`-activated (guava). Maven's effective
model genuinely carries it when the file is present (a Maven-semantics difference, not a projection-shape one), so the
engine is Maven-correct and legacy is the outlier — subsumed by NEW-2. The pinned sources-free `java.home` makes guava
CLEAN (srczip never activates → matches legacy). No engine change; the projection is already correct.

**Item 5 — synthetic request-log/HEAD assertions — SHADOW-AWARE.** Added `SyntheticHarness.shadowMode()` (reads the
selector system property) and scoped the transport-exact assertions (`repo.requests()/artifactRequests()` +
index-based `recordedArtifacts()` header checks) to non-shadow mode in AuthFallbackTest, NegativeCachingTest,
HtmlIndexMetadataTest, PomlessJarTest, SnapshotResolutionTest — every resolution-result assertion stays active in all
modes and the LEGACY pins are untouched (each guarded block re-asserts in legacy). AuthFallbackTest, PomlessJarTest,
HtmlIndexMetadataTest.accessDenied now pass in SHADOW. IMPORTANT finding: the majority of the remaining synthetic-shadow
reds are NOT request-log doubling — they are shadow-*facade* divergences thrown inside `resolve()` (two engines sharing
one stateful mock + pomCache produce a not-found-vs-phantom-resolution artifact), a separate class from the request-log
assertions and not addressable by test guards. Those stay red under their existing outcome/error-shape ledger rows.

**Item 6 — recipe re-resolution (ChangeDependencyGroupIdAndArtifactId, ~10) — ledgered OPEN.** Root shape (slice F):
the recipe's post-edit RE-resolution resolves a different coordinate on the two engine paths
(`org.apache.commons:commons-io` vs `commons-io:commons-io`) — a caller-identity/threading question on re-resolution, the
L-P3-D-004 `threadingEqual` family. A blunt change here risks the slice-F 80-test regression; left red+unmasked per
drop-safety, evidence in phase3-results-f item 3.

**Item 7 — error-shape (L-P3-C-005, ~12) — ledgered OPEN (message-text normalization).** The engine collector now
embeds the GAV and the `"<gav> failed. "` prefix (verified in the spark corpus diff:
`"com.google.protobuf:protobuf-java:3.23.4 failed. Unable to download POM: …"`), so consumer-facing message parity is in
place; the residual `$.errors` normalized-text differences ride the existing strictness/outcome rows (L-P2-D-005,
L-P3-C-005) that flip at Phase 5.

## Final gates

| gate | result |
|---|---|
| LEGACY full `:rewrite-maven:test` | **PASS — 1593 tests, 0 failures** (BUILD SUCCESSFUL, network flakes passed this run) |
| SHADOW full `:rewrite-maven:test` census | **94 failing** (slice-F baseline 101 → **−7**; within the documented ±5-10 flake band). Every residual maps to a ledgered class (breakdown below) |
| hermetic parity + engine unit (LEGACY) | **PASS** — `parity.*` + `internal.engine.*` (228 tests) incl. new `parent-version-expr` fixture + `MetaversionResolutionTest` |
| corpus gate (touched entries, shadow replay ×2) | **PASS** — deterministic, zero store misses; guava CLEAN, spark 1540→6, netty/hadoop→1 property, dubbo NEW-1 residual 0 |
| MAVEN-mode worst-3 rate | **143/185 (77%)** — `MavenParserTest`(89/111) + `ChangeDependencyGroupIdAndArtifactIdTest`(49/60) + `tree.ResolvedPomTest`(5/14); unchanged from slice E/F (NEW-3 net-neutral in MAVEN mode) |

## Final SHADOW census (94, every entry ledgered; none masked)

| class | count | ledger | flip-at-P5? |
|---|---|---|---|
| synthetic/infra (request-log-exact + shared-mock/cache facade artifacts: NegativeCaching, SnapshotResolution, RecipeBundleReader, PomDownloader/ArtifactDownloader, LocalCache, HtmlIndex, FileRepo, CachingBundleResolver, Mirror, PomlessJar, MarketplaceGenerator) | 31 | test-shape artifacts of dual-engine byte-fetch; the request-log ones are now legacy-scoped, the rest are facade shared-state | no |
| ALIGN classifier/type/scope dedup (IdentityContracts + RemoveDuplicate) | 12 | L-P0-001 / L-P3-E-005 | **yes** |
| ALIGN comparison-under-shadow flips + profile (EngineResolvedPom/DepGraph/Determinism fixtures + `Profiles`) | 9 | L-P0-001 / L-P2-B2-002 (classifiers, profile-activation, plugins-executions, scope-matrix) | **yes** |
| recipe re-resolution (`ChangeDependencyGroupIdAndArtifactId`) | 8 | recipe-path re-resolution (L-P3-D-004 family) | no |
| gav-mediation / closure residue (MavenParser, ResolvedPom, RemoveRedundant, Exclude, ChangeParent, Upgrade/Add/Remove misc) | 31 | L-P3-C-004 / L-P3-D-003(b) / L-P3-E-006 | no (genuine value divergence, drop-safe) |
| error-shape / strictness outcome (MavenDependencyFailures) | 3 | L-P3-C-005 / L-P2-D-005 | partial |

**Target stance (honest, per slice D/E/F):** ~21 are clean flip-at-P5 ALIGN pins (dedup + profile + comparison-flip);
the strictness/outcome rows partly flip; ~31 synthetic request-log/facade artifacts, ~31 genuine mediation value
divergences, and ~8 recipe re-resolution divergences remain. None is masked; all are root-caused and ledgered. The four
corpus NEW classes are closed (guava CLEAN; spark/netty/hadoop/dubbo collapsed to a ledgered residue). NEW-2 is the one
open recommendation flagged to Jon (embedder knob implemented + pinned; directional ALIGN mask deferred to sign-off).

## Files changed (all uncommitted)
- `rewrite-maven/.../MavenExecutionContextView.java` — `setActivationSystemProperties`/`getActivationSystemProperties` (NEW-2 input).
- `rewrite-maven/.../internal/engine/EngineEffectivePom.java` — consume the pinned activation properties (default host).
- `rewrite-maven/.../internal/engine/DependencyGraphMapper.java` — `project.parent.*` interpolation (NEW-1).
- `rewrite-maven/.../internal/engine/MavenEngineResolution.java` — collect through parent-merged model repositories (NEW-3).
- `rewrite-maven/src/test/resources/parity/fixtures/parent-version-expr/**` — NEW-1 hermetic pin.
- `rewrite-maven/.../parity/synthetic/{SyntheticHarness,AuthFallback,NegativeCaching,HtmlIndexMetadata,PomlessJar,SnapshotResolution}Test.java` — shadow-aware transport assertions.
- `maven-resolution-plan/phase0-tools/.../CorpusResolutionRunner.java` — `CANONICAL_ACTIVATION_PROPERTIES` (NEW-2 reproducibility).
- `doc/maven-resolution-ledger.md` — L-P3-G-001..004.
