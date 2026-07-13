# Phase 4 slice A — closure-divergence deep dive (L-P3-C-004)

2026-07-09, worktree `maven-resolution` (uncommitted). Charter: resolve the last big semantic gap — the closure/mediation
OPEN class (L-P3-C-004 + the C-002 residue + the D-004/recipe-re-resolution family) — to ZERO or to
individually-arbitrated-with-proof ALIGN rows before any flip. Method: full inventory → cluster by mechanism →
per-cluster hermetic repro + `mvn 3.9.14 dependency:tree` ground truth → surgical fix or arbitrate.

## Headline

**The "engine drops members legacy resolves" framing was mostly wrong.** A dual-engine probe (resolve the same pom under
LEGACY and MAVEN, diff the flat per-scope sets) proved that the recurring corpus victims — slf4j-api, error_prone,
micrometer/LatencyUtils/HdrHistogram, jackson, opensearch, junit, guava, log4j, lombok, spring-boot — resolve to
**identical flat sets** on both engines. The divergence is in the **nested tree shape / sibling order / per-scope
attribution**, where the engine (real maven-resolver 3.9) matches `mvn dependency:tree` and legacy is the outlier. These
are genuine value divergences (recipes walking the nested graph could differ) but they are **ALIGN_TO_MAVEN, legacy is
wrong, flip at Phase 5** — not engine drops. This dissolves the "unarbitrated bulk residue."

**Exactly one genuine engine bug was found and fixed** (L-P3-A-001, below): compile-scope under-population of a
widest-scope conflict winner.

## Mechanism taxonomy (full SHADOW census, deduped by pom + drop/add signature)

| # | mechanism | representative | count* | verdict |
|---|---|---|---|---|
| 1 | **same-set tree-shape / order / scope-attribution divergence** — engine==Maven, legacy outlier | opensearch (53=53), spring-boot (34=34), junit, guava, log4j, lombok, jackson-databind closure, micrometer/LatencyUtils/HdrHistogram subtree, kotlin-stdlib | ~35 | **ALIGN**, flip P5. Proven same-set by probe + `mvn` |
| 2 | **compile-scope under-population of a widest-scope winner** — ENGINE BUG | grpc-core → error_prone (compile), kotlin → jetbrains:annotations | ~5 | **FIXED (L-P3-A-001)**; kotlin residue is a legacy version-mediation outlier (below) |
| 3 | **duplicate g:a scope/classifier dedup** — L-P3-E-005 / L-P0-001 | `lastListedDependencyIsUsedForScope` jackson-core dup; guava dup; guice classifier | ~18 | **ALIGN**, flip P5 |
| 4 | **classifier `""` vs `null` requested threading** | netty-tcnative-boringssl-static | ~17 (1 sig) | ALIGN/normalization (`$.scopes.*.classifier`/`requestedRef` trailing colon) |
| 5 | **profile / comparison-fixture flips** — L-P2-B2-002 / L-P0-001 | `org.parity:multi` classifier, profile-activation | ~17 | **ALIGN**, flip P5 (also the ±flake pool) |
| 6 | **recipe re-resolution** — L-P3-D-004 family | `commons-io` groupId change (`org.apache.commons:commons-io` vs `commons-io:commons-io`) | ~2 | OPEN (caller-identity/threading; slice-F 80-test-regression zone) |
| 7 | **engine-adds in a transitive descriptor** — L-P3-E-006 + mediation | moxy `persistence.core`, `maven-site-plugin`, `flux-capacitor:common` | ~3 | OPEN (intra-pom last-wins; documented) |
| 8 | **jenkins-ci network** | `jenkins-war`/`slf4j-jdk14` vs `xerces` | ~2 | environment (legacy 403s on the war closure) |
| 9 | **synthetic/infra** — dual-engine byte-fetch + shared-mock facade | NegativeCaching, SnapshotResolution, FileRepo, RecipeBundle, LocalCache, Marketplace | ~30 | test-shape, fail at HEAD too |

*Counts are per-signature over one clean census; cross-test attribution under JUnit `concurrent` mode makes the failing
test *name* unreliable, so clustering is keyed on the mismatch **pom GAV + drop/add g:a set**, which is reliable.

## The one engine bug — L-P3-A-001 (FIXED)

**Root cause (instrumented).** `io.grpc:grpc-core:1.60.1` declares `error_prone_annotations:2.20.0` at **`runtime`**
directly (nearest, depth 1); `grpc-api` declares the same version at **`compile`** (depth 2). Maven selects a
coordinate's effective scope as the **widest across every path to its winning version**, so it keeps error_prone
**compile** ("scope not updated to compile"). Aether records that on the winner edge (`edgeScope=compile`), but
`DependencyGraphMapper.childScope` **re-read the raw declared scope** from the nearest containing pom
(`matchDeclared(grpc-core, error_prone) = runtime`) and excluded it from the compile classpath — a genuine engine DROP.

**Ground truth (arbitration).** `mvn 3.9.14 dependency:tree` on grpc-core (compile) lists
`error_prone_annotations:2.20.0:compile`; the LEGACY engine lists it too (legacy reaches it via the depth-2 compile edge
because the depth-1 runtime edge fails the compile-membership test). **Maven == legacy → engine bug.**

**Fix** (`DependencyGraphMapper.childScope`, 12 lines): trust aether's edge scope when it is **wider** (higher
precedence) than the raw-declared/legacy-computed scope. When aether instead PROMOTED it *narrower* — a compile child of
a test/provided parent that its `JavaScopeDeriver` moved to test/provided — the declared scope is wider and wins,
**preserving slice-D's L-P3-D-001 test-closure population**. `Scope.maxPrecedence(aether, legacy) == aether && aether !=
legacy ? aether : legacy`.

**Verification.** Dual-engine probe: grpc-core compile `4 → 4 SAME SET` after the fix (error_prone back in compile).
`internal.engine.*` deterministic unit suite green. Comparison-fixture parametrized flake unchanged (stable 4 failures,
same as HEAD — confirmed by two identical back-to-back runs shuffling *which* 4 ALIGN fixtures fail). No new census
divergence signature. LEGACY path untouched by construction (engine-only file). Pinned by
`CompileScopeWideningTest.wideningToCompileMatchesMaven` (MAVEN mode, real grpc-core; **fails pre-fix, passes post-fix**).

**Hermetic-fixture note.** A `parity/fixtures/` fixture proved elusive: reproducing the drop needs aether to place the
conflict winner under the runtime edge while a same-version compile duplicate exists AND a competing lower version forces
the relink — grpc-core's exact three-version topology. Two constructed fixtures failed to trigger it (aether resolved the
compile edge directly). Pinned by the deterministic real coordinate instead (immutable on Central).

**kotlin residue (NOT fixed, arbitrated).** `rewrite-testing-frameworks:1.6.0` → `org.jetbrains:annotations`: legacy
resolves **13.0** in compile, `mvn` resolves **21.0.1:compile** (13.0 "omitted for conflict with 21.0.1"). Legacy is a
**version-mediation outlier** (nearest-declaration wins 13.0); even a perfect engine (21.0.1) cannot match legacy's 13.0.
Unfixable-to-parity → ALIGN, legacy is wrong, flip P5.

## Ground-truth arbitration verdicts

| case | `mvn 3.9.14` | legacy | engine | verdict |
|---|---|---|---|---|
| grpc-core error_prone scope | `compile` | `compile` | `runtime`→dropped from compile | **engine bug → FIXED (L-P3-A-001)** |
| opensearch/spring-boot/junit/guava/log4j/lombok flat set | = engine | = engine | = Maven | ALIGN: legacy tree-shape outlier only |
| kotlin jetbrains:annotations version | `21.0.1:compile` | `13.0:compile` | (21.0.1, dropped pre-fix) | ALIGN: legacy version-mediation outlier |

## Final census

- **SHADOW full `:rewrite-maven:test` (with fix, this machine):** 100 failed / 101 mismatches, but this run had **24
  network `Unable to download` failures** (the environment's Central access degraded mid-slice) that cascade into extra
  mismatches via partial-result divergence + shared-mock cross-attribution. The slice-start clean baseline (0 downloads)
  was **93 failed / 94 mismatches**. Signature-by-signature the with-fix run contains **no new divergence class** vs the
  baseline — every drop/add cluster (jackson dup-scope, jackson-databind/junit/guava/lombok/micrometer closures,
  commons-io re-resolution, jenkins, moxy, kotlin-annotations) is pre-existing; the count delta is the network noise. A
  clean 0-download apples-to-apples full census could not be obtained in this environment (repeated runs varied 8–24
  download failures; one run degraded to 45 min / 499 failures under Central rate-limiting).
- **LEGACY full:** unaffected by construction (the change is engine-only, never invoked in LEGACY mode); baseline is
  green (slice G: 1593 tests, 0 failures). A confirming run was network-gated at write time.
- **Deterministic hermetic gates (network-free, MAVEN mode):** `internal.engine.*` **GREEN**; parity comparison suites
  stable at the 4 known-ALIGN parametrized flakes (identical HEAD vs fix).

**Honest stance on the gate.** The OPEN closure class is now **arbitrated, not a bulk residue**: every remaining
`$.scopes` red is one of — (a) an ALIGN same-set tree-shape/scope divergence where `mvn` agrees with the engine and
legacy is the proven outlier (flip P5), (b) an ALIGN dedup/classifier/profile pin (flip P5), (c) a documented-OPEN recipe
re-resolution / intra-pom-duplicate item (L-P3-D-004 / L-P3-E-006, out of safe budget), or (d) synthetic/network
test-shape. The single genuine engine drop is fixed and pinned. Nothing is masked.

## Corpus states

Corpus reclassification (spark/dubbo) requires republishing changed modules to Maven Local and rerunning the shadow
replay per phase0-tools/RUNBOOK.md. **Not re-run this slice** — the environment's Central flakiness (which the corpus
top-up also depends on) made a deterministic replay unreliable, and slice A's mapper change is `$.scopes`-scope-only.
Expected effect from L-P3-A-001 on the recorded corpus: the dubbo/spark residues are dominated by micrometer/protobuf
**tree-shape** divergences (cluster #1, ALIGN) and `effectiveExclusions` residue (L-P3-C-002), not by the compile-scope
drop this slice fixed, so their entry-level UNEXPLAINED verdict is unchanged; the arbitration above reclassifies their
`$.scopes` residue from "engine drop" to "ALIGN legacy tree-shape outlier." Recommend a corpus rerun once the
environment's network stabilises.

## maven-mode worst-3 rate

Not cleanly re-measured (network-gated). The fix is net-neutral-to-positive for maven-mode: it adds a Maven-correct
compile-scope member (grpc-family) and changes nothing else; the worst-3 classes (`MavenParserTest`,
`ChangeDependencyGroupIdAndArtifactIdTest`, `tree.ResolvedPomTest`) are dominated by dedup/profile ALIGN and
recipe-re-resolution, which this slice did not touch. Baseline 143/185 (77%) expected to hold.

## Files changed (uncommitted)

- `rewrite-maven/src/main/java/org/openrewrite/maven/internal/engine/DependencyGraphMapper.java` — `childScope` trusts
  aether's wider edge scope (L-P3-A-001), 12 lines.
- `rewrite-maven/src/test/java/org/openrewrite/maven/parity/CompileScopeWideningTest.java` — new deterministic pin.
- `doc/maven-resolution-ledger.md` — L-P3-C-004 → ARBITRATED; new L-P3-A-001 (FIXED).

## Discipline notes

- The fix is surgical (12 lines, one method), engine-only, and preserves L-P3-D-001 by construction (verified the
  test-parent narrowing path is untouched).
- Deliberately did **not** touch `winners()` / the version-threading path (the slice-F 81-test-regression zone). The
  earlier hypothesis that losers-carry-compile-reachability was wrong: the winner edge already carries Maven's widened
  scope; only `childScope`'s re-read of the raw declared scope was undoing it.
- No mask added. No mask could hide a drop; the fix restores the dropped member instead.
