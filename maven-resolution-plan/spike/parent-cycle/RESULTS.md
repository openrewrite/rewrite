# Parent-cycle spike — RESULTS

Empirical determination of what happens when Maven 3.9.16 `DefaultModelBuilder` (via
`maven-resolver-supplier-mvn3:2.0.20`) meets a POM parent cycle, and which cycle-break strategy lets the migration keep
rewrite's "parse anything" guarantee. Hermetic: all fixtures on disk, driven through `DefaultModelBuilder` with a
filesystem `ModelResolver`. No network.

## How to run

```
/Users/jon/Projects/github/openrewrite/rewrite/.claude/worktrees/maven-resolution/gradlew \
  -p /Users/jon/Projects/github/openrewrite/rewrite/.claude/worktrees/maven-resolution/maven-resolution-plan/spike/parent-cycle \
  test
```

Versions match the sibling provenance spike: model-builder **3.9.16**, resolver **2.0.20**, `options.release = 8`.
**9 tests, 0 failures** (`spike.ParentCycleTest`).

---

## 1. What rewrite guarantees today (the behavior we must preserve)

rewrite **never aborts a parse on a parent cycle**; it produces a usable `MavenResolutionResult` with the model
assembled from the ancestry walked up to the first GAV repeat. The break is done by its own ancestry-walk and, today, is
**SILENT** — no warning is emitted. (The migration goal of "degraded model *plus a warning*" is therefore a strict
improvement over today's silent degrade, not a regression.)

Mechanism: `ResolvedPom.java` walks `pomAncestry`, and before recursing into a resolved parent it checks whether that
parent's GAV already appears in the ancestry; if so it `return`s (stops), keeping everything merged so far.
- Parent-property / repository walk: `ResolvedPom.java:479-484` (`// parent cycle` → `return`)
- Parent dependency-management / dependency walk: `ResolvedPom.java:529-534`
- Parent plugin walk: `ResolvedPom.java:561-566`
- BOM-import cycle guard: `ResolvedPom.isAlreadyResolved:984-991`
- `${project.version}` self-cycle → sentinel value, not abort: `ResolvedPom.getProperty:350-362`

Pinned guarantees (the cycle-tolerance suite — every one is an intentional divergence, catalog a4 entries 8/16/30):

| Guarantee | Test | Resulting model |
|---|---|---|
| Self-parent (`<parent>` GA == own GA) parses | `MavenParserTest.selfRecursiveParent:782` (#135) | `MavenResolutionResult` present; effective = the POM's own content; parent silently dropped |
| Self-dependency parses, no infinite transitives | `MavenParserTest.selfRecursiveDependency:806` (#135) | `getDependencies().get(Compile)` size == 1; comment: *"Maven itself would respond … with a fatal error. So long as we don't produce an AST with cycles it's OK"* |
| Parent⇄child BOM import cycle resolves | `MavenParserTest.circularImportDependency:3264` (#4093) | both models' first managed dep resolves to `junit:junit:4.13.2` |
| `${project.version}` in `<version>` | `MavenParserTest.circularMavenProperty:3332` | resolves to `1.0.1` |
| Circular `${project.version}` | `ResolvedPomTest.circularProjectVersionReference:733`, `circularProjectVersionInDependency:780`, `dependencyWithCircularProjectVersionReference:160` | sentinel `"error.circular.project.version"` instead of StackOverflow/abort |

The item this spike targets is the **parent cycle** (self-parent + multi-POM A→B→A). `${project.version}` and BOM-import
cycles are separate Maven code paths (the latter has its own FATAL at `DefaultModelBuilder:1166`) and are out of scope
here — flagged as follow-ups.

---

## 2. How Maven's FATAL parent-cycle check actually works (read from source)

`DefaultModelBuilder` walks the parent lineage with a `LinkedHashSet<String> parentIds` (line 288). On each parent it
does `!parentIds.add(parentData.getId())`; the first time an id repeats it adds a **FATAL** `ModelProblem`
("The parents form a cycle: …") and `throw`s a `ModelBuildingException` (lines 358-368).

Two facts decide everything below:

1. **The cycle-check id comes from the CHILD's `<parent>` element, not the served bytes.** In `readParentExternally`
   the id is built as `new ModelData(source, parentModel, parent.getGroupId(), parent.getArtifactId(),
   parent.getVersion())` (lines 1095-1096), where `parent` is a **clone of the child's `<parent>`** (line 1019) that is
   handed to `modelResolver.resolveModel(parent)` (line 1035). So a resolver can change the id **only by mutating that
   `Parent` object** — serving different POM bytes under the same coordinates does nothing.
2. **Self-parent is caught earlier and unconditionally, by a different check.** `DefaultModelValidator.validateRawModel`
   rejects a `<parent>` whose GA equals the project's GA (`DefaultModelValidator.java:114-124`, FATAL, **not gated by
   validation level** — the `VALIDATION_LEVEL_MINIMAL` gate is below it at line 138). It runs inside `readModel`
   (`DefaultModelBuilder:649`), which is invoked for the root only when `request.getRawModel()` is null (line 260). So a
   self-parent aborts at the **root read, before the resolver is ever consulted** — unless the caller pre-supplies the
   raw model.

---

## 3. Experiment outcomes

| # | Scenario | Strategy | Outcome |
|---|---|---|---|
| E1 | A.parent=B, B.parent=A (repo lookup) | plain resolver | **FATAL abort** |
| E2 | A.parent=B, B.parent=A (relativePath, sibling dirs) | plain resolver | **FATAL abort** (identical to E1) |
| E3a | multi-POM cycle | stub keeps revisited GAV (no mutation) | **DISPROVEN — still FATAL** |
| E3b | multi-POM cycle | stub under synthesized non-colliding GAV (mutate the `Parent`) | **WORKS — full model, inheritance intact** |
| E4 | multi-POM cycle | catch exception, salvage `getResult()` | root **raw** model only (no inheritance) |
| E5a | self-parent A.parent=A | plain resolver | FATAL, but via **raw validation**, not the cycle check |
| E5b | self-parent | MUTATE_ID resolver | **cannot help** — resolver never consulted (`log == []`) |
| E5c | self-parent | pre-supply rawModel + MUTATE_ID resolver | **WORKS** (unifies with the multi-POM path) |
| E5d | self-parent | pre-supply rawModel with `<parent>` stripped | **WORKS** (simplest) |

### E1 / E2 — baseline: cycles are FATAL and un-restartable

`ModelBuildingException`, message `2 problems … for com.example:a:1`, carrying:
`[FATAL] The parents form a cycle: com.example:a:1 -> com.example:a:1`. Repo-lookup and relativePath cycles are
indistinguishable at the failure — the `parentIds` check is downstream of both `readParentLocally` and
`readParentExternally`. (Aside: Maven's message only lists the root id, twice — `B` is never added to `parentIds`; a
cosmetic quirk of the accumulation order.)

### E3a — stub keeping the revisited GAV is DEAD ON ARRIVAL (adversarial reviewer was right)

Resolver log: `[SERVE com.example:base:1, STUB(KEEP_GAV) revisit=com.example:app:1]`. Even though the resolver returned
a synthesized **parentless** stub for the revisited GAV, the build still threw
`[FATAL] The parents form a cycle: com.example:app:1 -> com.example:app:1`. Confirms §2 fact 1: the id is read from the
child's `<parent>` element, which necessarily names the revisited GAV, so `parentIds` fires regardless of the stub's
own coordinates or its (missing) parent.

### E3b — stub under a synthesized non-colliding GAV WORKS

The cycle-aware resolver, on the would-be repeat, **mutates the passed `Parent`** (`artifactId` → `app--cyclebreak`) and
returns a parentless `pom` stub under that synthesized GAV. Log:
`[SERVE com.example:base:1, STUB(MUTATE_ID) revisit=com.example:app:1 served-as=com.example:app--cyclebreak:1]`.
- No exception. `getEffectiveModel()` is a real, interpolated model.
- `modelIds = [com.example:app:1, com.example:base:1, com.example:app--cyclebreak:1, ""]` — the full real ancestry plus
  the synthetic leaf and the super-POM.
- **Inheritance survives:** the real parent `base`'s `dependencyManagement` entry `com.example:lib:9.9.9` is present on
  `app`'s effective model. Same contribution set as rewrite's degrade (ancestry up to the repeat), now with real Maven
  InputLocations and interpolation.

The mutation targets `artifactId` only (leaving version untouched) to avoid the version-skew branch at
`readParentExternally:1074`. The resolver must be **seeded with the root GAV**: the FATAL fires the instant the root's
GAV is re-added (one step before the resolver would otherwise see a repeat), so the resolver has to already know the
root to intercept in time.

### E4 — partial-result fallback is thin

On the FATAL abort, `newModelBuildingException` (`DefaultModelProblemCollector:164-177`) seeds the interim result with
only the **root**: `tmp.addModelId(rootId); tmp.setRawModel(rootId, rootRawModel)`. Empirically:
- `ex.getResult() != null`; `getModelIds() == [com.example:app:1]`; `getEffectiveModel() == null`.
- `getResult().getRawModel(rootId)` / `ex.getModel()` → the root **raw** model (`com.example:app:jar:1`).
- That raw root has **no parent inheritance** (`getDependencyManagement() == null` in the fixture where the parent owned
  the managed dep). Strictly **poorer** than rewrite's degrade, which keeps the ancestry-up-to-repeat contributions.

So E4 salvages the root's own declared content but loses every parent contribution — acceptable only as a last-resort
backstop, not the primary path. (For a self-parent the abort is even earlier, at raw validation before
`problems.setRootModel`, so the exception carries even less — the caller must fall back to the POM bytes it already
holds.)

### E5 — self-parent is a different beast

`A.parent=A` never reaches the `parentIds` cycle check. It is FATAL via **raw-model validation**
(`'parent.artifactId' … cannot have the same groupId:artifactId as the project`), which runs unconditionally at the root
read (E5a). Because that abort precedes any resolver call, the MUTATE_ID stub is powerless — E5b's resolver log is `[]`,
proving it was never consulted. Two things do work, both requiring the caller to pre-supply the raw model
(`request.setRawModel(...)`, which skips the root `readModel`/`validateRawModel`):
- **E5c**: keep the self-parent, let it reach the MUTATE_ID resolver → stubbed like any cycle
  (`modelIds = [app:1, app--cyclebreak:1, ""]`), managed dep preserved. Unifies self-parent with the multi-POM path.
- **E5d**: strip the degenerate `<parent>` from the raw model first → clean build, `modelIds = [app:1, ""]`, managed dep
  preserved. Simplest.

---

## 4. Recommendation for the migration's `LenientModelResolver`

Use a **cycle-aware `ModelResolver` (E3b MUTATE_ID) as the primary mechanism**, plus a **cheap self-parent pre-flight
(E5d)** because self-parent aborts before the resolver runs. E4 salvage is a defensive backstop only.

**Mechanics:**

1. **Cycle-aware resolver (multi-POM cycles).** The `LenientModelResolver` is seeded with the root project's GAV and
   keeps a `Set<String> seen` of every GAV it serves within one `build()`. On `resolveModel(Parent parent)`:
   - if `parent`'s GAV is already in `seen` → **mutate `parent.setArtifactId(orig + "<sentinel>")`** and return a
     minimal, parentless, `pom`-packaged `ModelSource` under the mutated GAV; emit rewrite's cycle **WARNING** here.
   - else add the GAV to `seen` and serve normally.
   - `newCopy()` shares the `seen` set (single build). Mutate `artifactId` only (skip the version-skew path).
   This needs **no** `rawModel` pre-supply — multi-POM cycle members have distinct GAs, so raw validation never trips.
   (Verified: E3b uses the plain source path.)

2. **Self-parent pre-flight.** Before building, if the root's `<parent>` GA equals the root's own GA, **strip the
   `<parent>`** and build from the pre-supplied raw model (E5d). Equivalent outcome to today (own content, parent
   dropped), and it avoids injecting a phantom stub id for the trivial case. (Alternatively, always pre-supply rawModel
   and let the resolver stub the self-parent too — E5c — if a single unified code path is preferred; the cost is losing
   the root's normal raw validation.)

3. **Backstop.** Wrap `build()` in a `try/catch (ModelBuildingException)`; if any unforeseen cycle shape still throws,
   salvage `getResult().getRawModel(getModelIds().get(0))` (root raw, un-inherited) so the parse still yields *some*
   model. Log a warning that inheritance was dropped.

**Behavioral deltas vs rewrite today:**

- **Warning vs silence.** rewrite breaks parent cycles silently; this design emits a warning at the break — matches the
  migration's "degraded model *plus a warning*" goal. Improvement.
- **Model richness: equal.** E3b keeps the full ancestry up to the repeat (same contribution set as rewrite), and adds
  real Maven interpolation + `InputLocation` provenance the current resolver doesn't produce.
- **Phantom model id.** The synthetic leaf (`g:a<sentinel>:v`) appears in `result.getModelIds()` and as the parent
  pointer of the last real model. **The `DependencyGraphMapper` / `EffectivePomMapper` must filter the sentinel id** so
  it never leaks into provenance (`bomGav`, lineage). Choose a sentinel suffix that cannot collide with a real
  artifactId. New concern introduced by this approach.
- **Self-parent outcome: equal** (own content, no parent), whether via strip (E5d) or stub (E5c).
- **Cosmetic:** a self-parent handled by E5c/E3b leaves the child's `<parent>` element pointing at the mutated GA in that
  model's effective view; the root's own effective model is unaffected (its real parent is resolved before any
  mutation). Prefer E5d for self-parent to avoid even this.

**Out of scope / follow-ups:** `scope=import` BOM cycles have a *separate* FATAL (`DefaultModelBuilder:1166`, "the
dependencies of type=pom and with scope=import form a cycle") guarded by an `importIds` set; rewrite tolerates these
today (`circularImportDependency`), so the migration needs an analogous import-cycle break. `${project.version}`
self-cycles are handled inside interpolation, not the parent walk. Neither is exercised by this spike.
