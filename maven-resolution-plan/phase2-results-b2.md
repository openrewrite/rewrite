# Phase 2 slice B2 — effective-POM mappers + first legacy-vs-engine differential

2026-07-08, worktree `maven-resolution` (uncommitted). New classes in `org.openrewrite.maven.internal.engine`
(rewrite-maven), composed on B1 (`EngineEffectivePom`/`EngineModelBuildingOutcome`) and the shaded engine module.
Scoped builds green: `:rewrite-maven:compileJava`/`compileTestJava`, `:rewrite-maven:test --tests …engine.*` (38, 0
failures), `--tests …parity.*` (comparison 11, identity 46, determinism 23, synthetic suites all green),
`:rewrite-maven:licenseMain`/`licenseTest`.

## Deliverables

| class | role |
|---|---|
| `EffectivePomMapper` | `ModelBuildingResult` → `ResolvedPom` (§4.1). Raw-lineage `properties` + injected overlay; effective-model `dependencyManagement` with `requested`/`requestedBom`/`bomGav` threaded by `InputLocation`; GA-keyed child-wins `requestedDependencies` threading original instances; effective repositories/plugins with the super-POM filtered; `sameIfUnchanged` no-change identity gate |
| `BomGavAttributor` | membership-stamping `bomGav`/`requestedBom`: resolves each `scope=import` BOM's effective DM through the cached `EngineEffectivePom` and stamps by import order (first-wins) — multi-level BOMs attribute to the directly-imported BOM, not the `InputLocation` defining parent |
| `PomToModelConverter` | synthetic `Pom.builder()` graph → shaded raw `Model`; wired into `ReactorWorkspace`'s null-bytes seam (the one allowed touch) |
| `PluginConfigurations` | bidirectional `Xpp3Dom` ↔ Jackson `JsonNode` matching `MavenXmlMapper`'s plugin-config shape |
| `EngineResolvedPomComparisonTest` | the acceptance gate: legacy (`ParityHarness`) vs B1+B2, `$.pom`-section `ResolutionSnapshot` diff, per-fixture expected-diff list citing ledger rows |

## The differential — per-fixture `$.pom` diff classification (the centerpiece)

| fixture | pom-section diffs | classification |
|---|---|---|
| bom-import-single | none | clean — `bomGav=org.parity:bom:1.0` reproduced by membership stamping |
| bom-import-multi | none | clean — `bomGav=org.parity:bom-outer:1.0` (directly-imported BOM, not the `bom-inner` defining parent) |
| classifiers | none | clean — both `Defined` entries thread by reference |
| conflict-equal-depth | none | clean (empty pom section) |
| conflict-unequal-depth | none | clean |
| exclusions | none | clean (exclusion lives on the requested dep, a scope-section fact) |
| optional | none | clean |
| parent-chain | none | clean — inherited DM + `in-pom` repo match; super-POM `central` filtered |
| profile-activation | none | clean — active profiles contribute only deps (scope section); pom section identical |
| property-indirection | none | clean — `properties["dep.version"]` stays the raw `${dep.version.actual}` (DESIGN §4.1 raw-lineage, not interpolated); `getValue()` resolves it lazily |
| **plugins-executions** | `$.pom.plugins[*]` (whole array) | **L-P0-002 + L-P0-003** |

**plugins-executions actual flip (legacy != engine):**
- plugin order — legacy `[own-plugin, enforcer]` vs engine `[enforcer, own-plugin]` (**L-P0-003**: legacy re-appends a parent-merged plugin to the end; Maven keeps inherited-first order)
- enforcer `shared` goals — legacy `[display-info, enforce, enforce-child]` (HashSet iteration) vs engine `[enforce-child, display-info, enforce]` (**L-P0-002**: Maven's order-preserving goal union)

Both are pre-existing `ALIGN_TO_MAVEN` ledger rows; the engine now produces the Maven-correct order, and the comparison
test asserts this is the *only* surviving diff (and that it is non-vacuous). **Bottom line: 10 fixtures clean, 1
expected-flip-only, 0 unexplained diffs, 0 legacy bugs found.**

## Reconciliations that keep the pom section legacy-matching (investigated to root cause)

- **Raw vs interpolated properties** — `properties` come from `getRawModel(id)`/active-profile props (first-wins,
  child→parent) + injected overlay, NOT `effectiveModel.getProperties()`. Using the effective (interpolated) map would
  have flipped `property-indirection`'s `dep.version` to `2.0`; DESIGN §4.1 mandates raw, so it matches legacy.
- **Super-POM injection (new ledger row L-P2-B2-001, KEEP_REWRITE)** — Maven's effective model always injects the
  super-POM's 4 default `pluginManagement` plugins + `central` repository + `central` pluginRepository; legacy carries
  none. Root cause surfaced as a diff on *every* fixture initially. Fix: the mapper keeps only entries whose
  `InputLocation` modelId is a real fetched/root model (the super-POM is never fetched), dropping all super-POM
  contributions uniformly — after which repositories/pluginRepositories/pluginManagement match legacy everywhere.
- **`bomGav` for multi-level BOMs** — `InputLocation` alone attributes `managed-y` to `bom-inner` (SPIKE-RESULTS §4b);
  `BomGavAttributor` resolves `bom-outer`'s effective DM and stamps membership, reproducing legacy's directly-imported
  `bom-outer`.

## Identity-contract verification (study a3 §2) — all PASS

- `getResolvedManagedDependency(requested)`/`(requestedBom)` match by `==`; the BOM-flattened entries thread the root
  `Imported` instance (`requestedBom` is the same object) and the defining `Defined`; the root `Defined` entry threads
  the same object (`EffectivePomMapperTest.managedDependencyReferenceIdentity`, `…ThreadingHolds` over 4 fixtures).
- depth-0 `requestedDependencies` thread the exact declaring `Pom.dependencies` instances
  (`…requestedDependenciesThreadDeclaringInstances`).
- injected `parity.repo.url` visible via `getProperties()`/`getValue()` and does not clobber a lineage property
  (`…injectedPropertiesAreVisible`); raw chained placeholder preserved (`…propertiesAreRawNotInterpolated`).
- `sameIfUnchanged` returns the previous instance on equal re-map, fresh on change (`…sameIfUnchangedIdentity`).
- `BomGavAttributorTest` single- and multi-level; `PomToModelConverterTest` field carry-over + synthetic reactor-parent
  inheritance through the null-bytes seam.

## Deviations (justified)

1. **Mapper collaborators are constructor-injected** (`pomCache`, `BomGavAttributor`); `map(outcome, requested,
   activeProfiles, injectedProperties)` keeps the specified signature. `BomGavAttributor` holds the `EngineEffectivePom`
   service to build each import BOM's effective DM (cached bytes → no network).
2. **DM `requested` join** — line/column into `getRawModel(modelId)` for lineage models (root + parents), a within-pom
   GACT fallback for imported-BOM models (whose ids are not in `getModelIds()`, so `getRawModel` is unavailable). The
   effective→declaring de-dup hazard the DESIGN warns against does not apply within a single raw pom's DM, and for BOM
   entries the snapshot provenance is carried by `requestedBom` (the root `Imported`), not the requested instance.
3. **Super-POM exclusion** keyed on "modelId is root or a fetched pom". Reactor-declared plugins/repos (no fixture has
   them) would need reactor GAVs added to that known-model set — a one-line extension when `ReactorWorkspace` exposes
   its GAVs.
4. **`ReactorWorkspace` seam** — `readModel` now converts a null-bytes (synthetic) pom via `PomToModelConverter`, backing
   its `pomFile` with `DefaultModelWriter`-printed bytes. The A2 placeholder test
   `syntheticPomWithoutXmlBytesReturnsNullModel` (whose own comment declared it "the seam slice B's PomToModelConverter
   fills") was updated to `…ResolvesThroughConverter`; the other 6 A2 tests are unchanged/green.
5. **Shaded-string note** — Maven relocates `org.apache.maven.plugins` inside its super-POM resource, so super-POM plugin
   groupIds surface with the `…engine.shaded…` prefix. Irrelevant here (super-POM plugins are excluded; all fixtures
   declare explicit plugin groupIds), but a production default-groupId path may need the prefix stripped — flagged for
   Phase 3/5.

## New ledger row

`L-P2-B2-001` — super-POM plugin/repository injection excluded from the projected pom (KEEP_REWRITE), pinned by
`EngineResolvedPomComparisonTest`.
