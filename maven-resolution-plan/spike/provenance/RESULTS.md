# Provenance spike — RESULTS

Empirical verification that real Maven APIs expose enough to reconstruct rewrite's provenance
(`ResolvedPom` / `ResolvedManagedDependency` / `ResolvedDependency`): defining-model attribution, BOM
provenance (`bomGav`), requested-vs-effective state, conflict winners/losers, premanaged state, depth.

## Exact versions used (resolved from Maven Central)

| Artifact | Version |
|---|---|
| `org.apache.maven.resolver:maven-resolver-supplier-mvn3` | **2.0.20** |
| `org.apache.maven.resolver:maven-resolver-{api,util,impl,spi,...}` | 2.0.20 |
| `org.apache.maven:maven-resolver-provider` (transitive) | **3.9.16** |
| `org.apache.maven:maven-model` / `maven-model-builder` / `maven-artifact` | **3.9.16** |
| `org.slf4j:slf4j-api` | 2.0.18 |
| JUnit Jupiter | 5.10.3 |
| Build/run JDK | Zulu 25 (`options.release = 8`) |

So the mvn3 supplier delivers the **Maven 3.9.16 model-builder** and **3.9.16 `DefaultArtifactDescriptorReader`/
`DefaultVersionResolver`** on top of **resolver 2.0.20**. Study A6/A5 said "3.9.x / 2.0.x" — confirmed as 3.9.16 + 2.0.20.

## How to run

```
/Users/jon/Projects/github/openrewrite/rewrite/.claude/worktrees/maven-resolution/gradlew \
  -p /Users/jon/Projects/github/openrewrite/rewrite/.claude/worktrees/maven-resolution/maven-resolution-plan/spike/provenance \
  test
```

No network at test time: P1/P2 drive `DefaultModelBuilder` directly against an on-disk fixtures dir via a
`FilesystemModelResolver`; P3/P4 collect against a synthetic Maven2 repo served as a `file://` `RemoteRepository`.
Verified green under `--offline --rerun-tasks`. (Gradle still downloads the dependency jars from Central once.)

### Final run output tail

```
BomProvenanceTest > multiLevelBomEntryIsAttributedToDefiningParentNotImportedBom(File) PASSED
BomProvenanceTest > singleLevelBomEntryIsAttributedToTheImportingBom(File) PASSED
ModelLocationTrackingTest > managedVersionInheritedFromParentIsAttributedToParent(File) PASSED
VerboseCollectTest > verboseConflictExposesWinnerLoserAndDepth(File, File) PASSED
VerboseCollectTest > verboseManagerExposesPremanagedVersionScopeAndExclusions(File, File) PASSED

BUILD SUCCESSFUL
```
5 tests, 0 failures, 0 skipped (`build/test-results/test/*.xml`: BomProvenance 2, ModelLocationTracking 1, VerboseCollect 2).

---

## Per-claim verdicts

### P1 — LOCATION TRACKING → **PROVEN**

`ModelBuilder` (3.9.16) with `request.setLocationTracking(true)` attributes each effective-model field to the model
that contributed it. A managed dependency version inherited from a parent POM carries an `InputLocation` whose
`InputSource.getModelId()` is the **parent**.

- Test: `ModelLocationTrackingTest.managedVersionInheritedFromParentIsAttributedToParent`
- Key assertions (green):
  - `effective.getDependencyManagement().getDependencies()` entry `com.example:lib` → `getVersion() == "1.2.3"`
  - `managed.getLocation("version").getSource().getModelId() == "com.example:parent:1"` — attributed to the PARENT
  - effective `dependencies` `lib.getVersion() == "1.2.3"` (managed version injected into the requested dependency)
  - `result.getModelIds()` contains both `com.example:parent:1` and `com.example:child:1` (parent chain / lineage)
- Notes: built with `VALIDATION_LEVEL_MINIMAL`, `twoPhaseBuilding=false`; empty `<relativePath/>` forces the parent
  through the injected `ModelResolver` (no filesystem parent lookup). This is the exact seam
  `EffectivePomMapper` uses to attribute managed versions/properties to a defining POM.

### P2 — BOM PROVENANCE ON 3.9 → **PROVEN-WITH-CAVEAT**

Two independent sub-claims:

**(2a) The custom-importer install seam — PROVEN.** A `DependencyManagementImporter` is injectable purely by
subclassing `DefaultModelBuilderFactory` and overriding the protected `newDependencyManagementImporter()` — no DI
container. The recorder intercepts every `scope=import` merge and delegates to the stock
`DefaultDependencyManagementImporter` so the effective model is unchanged.
- Files: `support/RecordingModelBuilderFactory`, `support/RecordingDependencyManagementImporter`.

**(2b) Reconstructing `bomGav` — PROVEN for single-level BOMs; the multi-level case is a documented gap.**

- Single-level BOM (BOM declares versions directly — the common real case: spring-boot-dependencies, junit-bom):
  - Test: `BomProvenanceTest.singleLevelBomEntryIsAttributedToTheImportingBom`
  - `factory.recorder.gaToBomModelId.get("com.example:lib") == "com.example:bom:1"` — recorder reconstructs `bomGav`.
  - The **default build's** `InputLocation` agrees:
    `managed.getLocation("version").getSource().getModelId() == "com.example:bom:1"`.
    → On single-level imports, `bomGav` is recoverable from plain per-field `InputLocation` alone; the custom
    importer is not even required.
- Multi-level BOM (BOM inherits its `<dependencyManagement>` from a parent BOM):
  - Test: `BomProvenanceTest.multiLevelBomEntryIsAttributedToDefiningParentNotImportedBom`
  - Both the `InputLocation` **and** the recorder resolve the entry to `com.example:parentbom:1` (the pom that
    **defined** the version), **not** `com.example:bom2:1` (the directly-imported BOM).
  - But rewrite's `bomGav` is the **directly-imported** BOM (`ResolvedPom.java:940-944` maps
    `bom.getDependencyManagement()` and stamps `.withBomGav(bom.getGav())`, `bom` = the BOM named in the
    `scope=import` entry). So on 3.9 **neither `InputLocation` nor the importer-via-location recovers the
    directly-imported BOM GAV through a multi-level chain.**

**Discrepancy vs study A6:** A6 §4 said 3.9 InputLocation points "into the BOM file" and that multi-level chains
"see the final defining POM but not the import path" — this spike makes that precise and shows it directly
contradicts rewrite's `bomGav` semantics for inherited-management BOMs. The importer seam does **not** close the gap
because `importManagement(target, sources, ...)` is handed only a `List<DependencyManagement>` — the importing BOM's
GAV is never passed, and `importDependencyManagement` (which *does* know `imported = g:a:v`) is `private`, so the
factory override cannot reach it. This is exactly what Maven 4's `importedFrom` fixes.

Root-cause evidence: `maven-3.9.x/maven-model-builder/.../DefaultModelBuilder.java:1105-1264` (importer call at
1264 drops the per-source GAV); `DefaultDependencyManagementImporter` copies `Dependency` objects without
rewriting their `InputLocation`.

### P3 — VERBOSE COLLECT → **PROVEN** (all of a, b, c, d)

Collection via the mvn3 supplier's default `RepositorySystem` + `SessionBuilderSupplier` session, with
`ConflictResolver.CONFIG_PROP_VERBOSE = Verbosity.STANDARD` and `DependencyManagerUtils.CONFIG_PROP_VERBOSE = true`.

- Test (a,b,d): `VerboseCollectTest.verboseConflictExposesWinnerLoserAndDepth`
  (graph: root → a:1 → c:1.0, root → b:1 → c:2.0; both `c` at depth 2)
  - **(a) winner + why:** the winning `c` node has version `1.0`; nearest-wins, and at equal depth the
    first-declared (`a` before `b`) wins. Asserted `winner.version == "1.0"`, `winner.depth == 2`,
    `winner.parentId == "a"`.
  - **(b) loser retained + pointer:** verbose `STANDARD` keeps the loser `c:2.0` (childless) under `b`; asserted
    `loser.version == "2.0"`, `loser.parentId == "b"`, and
    `loser.getData().get(ConflictResolver.NODE_DATA_WINNER)` is a `DependencyNode` whose version is `1.0`.
  - **(d) depth + declaring parent:** DFS records depth (root=0) and the declaring parent's artifactId; asserted
    `a`/`b` at depth 1 with parent `root`, `c` nodes at depth 2 with parents `a`/`b`.
- Test (c): `VerboseCollectTest.verboseManagerExposesPremanagedVersionScopeAndExclusions`
  (root's `<dependencyManagement>` forces transitive `c` → `1.5`, `scope=runtime`, `+exclusion test:extra`)
  - `c.getArtifact().getVersion() == "1.5"` and `DependencyManagerUtils.getPremanagedVersion(c) == "1.0"`;
    `MANAGED_VERSION` bit set.
  - `c.getDependency().getScope() == "runtime"` and `getPremanagedScope(c) == "compile"`; `MANAGED_SCOPE` bit set.

`ConflictResolver.getVerbosity` on 2.0.20 accepts a `Verbosity` enum (also `Boolean`/`String` → `STANDARD`/`NONE`);
we pass the enum. `ClassicDependencyManager` (`deriveUntil=2, applyFrom=2`) applied root management to the depth-2
transitive `c` exactly as Maven 3 does.

### P4 — MANAGED EXCLUSIONS (stretch) → **PROVEN**

- Same test as P3(c): `MANAGED_EXCLUSIONS` bit set on `c`; `c.getDependency().getExclusions()` contains
  `test:extra`; `getPremanagedExclusions(c)` does **not** contain it (original was empty); and `test:extra` is
  pruned from the collected graph (`withArtifactId(all,"extra").isEmpty()`).

---

## What `DependencyGraphMapper` / `EffectivePomMapper` can rely on (concrete API calls)

**Model building (embed `new DefaultModelBuilderFactory().newInstance()`, request `setLocationTracking(true)`):**

| rewrite provenance | Maven 3.9 API call | Confidence |
|---|---|---|
| Defining POM of a managed version / property / dependency (attribute to a POM in the lineage) | `model.getDependencyManagement().getDependencies().get(i).getLocation("version").getSource().getModelId()` (and `getLocation(field)` on any element) → `"g:a:v"` | High |
| Parent chain / lineage | `ModelBuildingResult.getModelIds()` (ordered) + `getRawModel(id)` | High |
| Requested (as-declared) vs effective | effective from `getEffectiveModel()`; as-declared from `result.getRawModel()` / `getRawModel(modelId)` | High |
| `requestedBom` (the `scope=import` node as written) | read the app's **raw** model `<dependencyManagement>` entries with `type=pom, scope=import` | High |
| `bomGav` = **directly-imported** BOM GAV | **single-level BOM:** `managed.getLocation("version").getSource().getModelId()`. **multi-level BOM:** NOT recoverable on 3.9 (points at the defining parent). Reconstruct as rewrite does today — for each `scope=import` raw entry, resolve that BOM's effective `getDependencyManagement()` via the same `ModelBuilder` and stamp its GAV over those GA keys. Or move to Maven 4 `importedFrom`. | Med (single-level: High) |
| active profiles per model | `ModelBuildingResult.getActivePomProfiles(modelId)` | High (per A6; not re-tested here) |

**Dependency collection (mvn3 `RepositorySystemSupplier` + `SessionBuilderSupplier`; verbose `STANDARD` +
`dependencyManager.verbose=true`; use `system.collectDependencies` — do NOT resolve, verbose graphs aren't resolvable):**

| rewrite provenance | Maven resolver 2.0.20 API call | Confidence |
|---|---|---|
| conflict winner + version | node with no `NODE_DATA_WINNER`; `node.getArtifact().getVersion()` | High |
| conflict loser + its requested version + pointer to winner | loser retained under STANDARD; `node.getData().get(ConflictResolver.NODE_DATA_WINNER)` → winning `DependencyNode` | High |
| original (pre-derivation) scope/optionality of a node | `node.getData().get(ConflictResolver.NODE_DATA_ORIGINAL_SCOPE / NODE_DATA_ORIGINAL_OPTIONALITY)` | High (keys present; not asserted here) |
| premanaged version | `DependencyManagerUtils.getPremanagedVersion(node)` (+ `getManagedBits() & MANAGED_VERSION`) | High |
| premanaged scope | `DependencyManagerUtils.getPremanagedScope(node)` (+ `MANAGED_SCOPE`) | High |
| premanaged exclusions | `DependencyManagerUtils.getPremanagedExclusions(node)` (+ `MANAGED_EXCLUSIONS`); effective on `node.getDependency().getExclusions()` | High |
| depth + declaring parent ("who declared it") | traverse `node.getChildren()` recording depth (root=0) and parent; resolver gives no back-pointer, so the mapper owns the walk | High |

## Discrepancies vs the study reports (summary)

1. **A6 §4 (bomGav):** confirmed *and sharpened* — 3.9 `InputLocation` gives the **defining** model, which equals the
   directly-imported BOM only for single-level imports. For inherited-management BOMs it diverges from rewrite's
   `bomGav`, and the `DependencyManagementImporter` factory seam cannot fix it (importing GAV not passed;
   `importDependencyManagement` is private). Maven 4 `importedFrom` is the real fix. **This should shape the plan:**
   on the 3.9 path, keep rewrite's "resolve each import BOM and stamp its GAV" step; do not expect the importer
   override alone to yield `bomGav`.
2. **A5 §4.4 (verbose values):** `ConflictResolver.getVerbosity` on 2.0.20 accepts the `Verbosity` enum directly
   (not only Boolean/String) — pass `Verbosity.STANDARD` for clarity; `true` also maps to STANDARD.
3. Everything else in A5/A6 that this spike touched held exactly: `locationTracking` attribution to defining model,
   `NODE_DATA_WINNER`, `DependencyManagerUtils` premanaged accessors + `getManagedBits()`, `ClassicDependencyManager`
   depth-≥2 management of transitives, mvn3 supplier delivering 3.9.16 model-builder over resolver 2.0.20.
