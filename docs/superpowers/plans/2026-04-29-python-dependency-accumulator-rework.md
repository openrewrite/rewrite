# Python Dependency Recipe Accumulator Rework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move per-project dependency-file and lock-file state from `ExecutionContext` into the recipes' `Accumulator`, and run the package-manager regeneration (`uv lock` / `pipenv lock`) inside `ScanningRecipe.generate()` so that the visitor phase becomes a pure lookup that is independent of file visit order.

**Architecture:** Each of the 5 dependency recipes (`AddDependency`, `RemoveDependency`, `ChangeDependency`, `UpgradeDependencyVersion`, `UpgradeTransitiveDependencyVersion`) gets a per-project `ProjectState` value object on its `Accumulator`. Scanner captures the sibling lock-file content and runs a recipe-specific match predicate. The visitor applies the recipe-specific trait edit to the **live** deps-file tree (so it composes correctly with edits from prior recipes in a composite), refreshes the marker via `PyProjectHelper.refreshMarker`, runs the matching package manager via `PyProjectHelper.regenerateLockContent`, and caches the regenerated lock content on the `ProjectState`. When the visitor encounters the corresponding lock-file later in the same pass, it emits the cached regeneration via `PyProjectHelper.reparseToml` / `reparseJson`. Failure during regeneration is captured as a per-project error string and rendered via `Markup.warn` on the deps-file when emitted.

> **Plan revision (2026-04-29):** This plan originally placed regeneration in `generate()` to make lock-file emission visit-order-independent. **That design does not work for `CompositeRecipe` chains** because the framework runs all recipes' `getScanner` / `generate` phases over the **original** sources before any visitor edits land — so `generate()` of recipe B cannot see recipe A's edit. See `RecipeRunCycle.scanSources` / `generateSources` / `editSources` in `rewrite-core`. The corrected design (above) does regeneration in the visitor on the live, possibly chain-modified tree, with the lock-file visit reading a cache the deps-file visit populated.
>
> **Plan revision 2 (2026-04-30):** The within-pass visit-order assumption is also unreliable: `RewriteTest` groups source specs by parser type, so a `pyproject.toml` (PyProjectTomlParser) and a sibling `uv.lock` (TomlParser) end up in different parser groups with non-deterministic visit order. The implemented design uses a **lazy-compute pattern with an `ExecutionContext` side channel** keyed by deps-file path:
>
> - **Scanner** captures both the deps tree (`capturedDepsFile`) and the lock content (`capturedLockContent`) on `ProjectState`.
> - **Visitor's deps-file branch** edits the live cursor's tree, regenerates the lock from the captured content, caches the result on `ProjectState`, and **publishes** the modified deps tree to the ctx side channel via `PyProjectHelper.putLiveDepsTree(ctx, depsPath, modified)`.
> - **Visitor's lock-file branch** lazily computes if not yet done: it reads the live deps tree from the ctx side channel via `PyProjectHelper.getLiveDepsTree(ctx, depsPath)` (or falls back to the scanner-captured `capturedDepsFile`), wraps it in a synthetic cursor rooted at `getCursor().getRoot()`, re-applies the trait edit, and caches. Then it emits the regenerated lock content via `reparseToml` / `reparseJson`.
>
> The ctx side channel survives across composite recipe boundaries within the same `RecipeRun`, so recipe B reads recipe A's modified deps tree directly. Within a single recipe pass, lazy-compute makes the design tolerant of any visit order: whichever branch is visited first computes; the other reads the cache.
>
> Task 2 (`AddDependency`) is now the **canonical reference implementation**. Tasks 3–6 mirror its shape with three localised changes only: the recipe option fields, the recipe-specific match predicate, and the trait edit function passed to `PyProjectHelper.editAndRegenerate`.

**Tech Stack:** Java 17+, OpenRewrite (`rewrite-core`, `rewrite-python`, `rewrite-toml`, `rewrite-json`), JUnit 5, Lombok, JSpecify nullability annotations.

---

## Background: Current State and Why It Is Broken

All 5 recipes today share the same shape:
- `Accumulator { Set<Path> projectsToUpdate }` — only the deps-file paths to edit.
- Cross-recipe state on `ExecutionContext` via `PythonDependencyExecutionContextView`: two `Map<Path, String>` (`existingLockContents` keyed by deps-file path, `updatedLockFiles` keyed by deps-file path).
- Scanner calls `PyProjectHelper.captureExistingLockContent(sourceFile, tree, ctx)` to stash on-disk lock content. If a deps-file matches, scanner adds its source path to `acc.projectsToUpdate`.
- Visitor: when visiting a deps-file in `projectsToUpdate`, calls `trait.with…(…).afterModification(ctx)`, where `afterModification` runs the package manager and refreshes the marker. When visiting a lock-file, calls `PyProjectHelper.maybeReplayLockContent(tree, ctx)` which reparses the lock document from `updatedLockFiles`.

Problems:
1. **Per-project state lives on `ExecutionContext`** (a flat global bag). Wrong scope for per-project data.
2. **Trait owns lock regeneration**, which forced `PipfileFile.afterModification` to take a `Function<Toml.Document, PythonResolutionResult> markerFactory` parameter to dodge a coupling cycle with `PipfileParser`.
3. **Visit-order dependence.** `PyProjectHelper.maybeReplayLockContent` only finds something if the deps-file was visited *before* the lock-file in the same pass. Path-sort order is not guaranteed (e.g. `package-lock.json` < `package.json` ASCII-wise; the JS analogue has the same hazard and copes by relying on multi-cycle execution).
4. **Recipe visitors do slow process invocations** (`uv lock` ~120s timeout, `pipenv lock` ~300s timeout) inline. No batching, no clear single-shot semantics.

## Design

### `ProjectState` (one per deps-file)

```java
static class ProjectState {
    @Nullable SourceFile capturedDepsFile;
    @Nullable String capturedLockContent;
    boolean depsFileMatches;
    @Nullable SourceFile modifiedDepsFile;
    LockFileRegeneration.@Nullable Result regenResult;
}
```

The map key in the accumulator is the deps-file source path, so it does not need to live on the value object. The single `regenResult` collapses what would otherwise be a `regeneratedLockContent` + `regenerationError` pair (mutually exclusive) into a single nullable; `null` means "regen wasn't attempted (no captured lock)", `regenResult.isSuccess()` distinguishes success/failure.

### `Accumulator` shape (same shape in every recipe)

```java
static class Accumulator {
    final Map<Path, ProjectState> projects = new HashMap<>();
    final Map<Path, Path> lockToDeps = new HashMap<>();
}
```

### Phase responsibilities

- **`getScanner(Accumulator)`**: For each `SourceFile`, dispatch on filename.
  - If lock-file (`uv.lock` → `Toml.Document`; `Pipfile.lock` → `Json.Document`): compute the corresponding deps-file path via `PyProjectHelper.correspondingPyprojectPath` / `correspondingPipfilePath`, write `capturedLockContent` into the matching `ProjectState`, and add an entry to `lockToDeps`.
  - Otherwise, attempt to match a deps-file via `PythonDependencyFile.Matcher`. If matched: create or update `ProjectState` for its source path, write `capturedDepsFile`, run the recipe-specific match predicate to set `depsFileMatches`.
- **`generate(Accumulator, ExecutionContext)`**: Not overridden — `ScanningRecipe`'s default returns an empty list. Lock regeneration cannot run here; see Plan revision 2 above.
- **`getVisitor(Accumulator)`**: Lazy-compute on first visit, with two branches sharing one `ensureComputed` helper.
  - **Deps-file branch.** When visiting a deps-file path that is in `projects` and has `depsFileMatches == true`: obtain the trait from the live cursor (so any prior recipe's edits are reflected), call `ensureComputed(ps, trait)`. If `ps.modifiedDepsFile != null`, publish it to the ctx side channel via `PyProjectHelper.putLiveDepsTree(ctx, sourcePath, out)`, apply `Markup.warn` when `ps.regenResult` is non-null and not successful, and return.
  - **Lock-file branch.** When visiting a lock-file path that is in `lockToDeps`: look up the `ProjectState`. If `depsFileMatches && modifiedDepsFile == null` (deps branch hasn't run yet for this path), pull the live deps tree from `PyProjectHelper.getLiveDepsTree(ctx, depsPath)` (covering prior-recipe edits) or fall back to `lockPs.capturedDepsFile` (scanner snapshot when no prior recipe touched it). Wrap that tree in a synthetic `Cursor(new Cursor(null, Cursor.ROOT_VALUE), depsTree)`, re-derive the trait via the visitor's `matcher.get(synth)`, call `ensureComputed`, and re-publish the result to the ctx side channel. Then if `lockPs.regenResult` is non-null and successful, emit via `reparseToml` / `reparseJson` using `regenResult.getLockFileContent()`.
  - **`ensureComputed(ProjectState, PythonDependencyFile)`**: idempotent. If `modifiedDepsFile != null`, return. Otherwise build the recipe-specific edit `Function<PythonDependencyFile, PythonDependencyFile>` from the recipe's option fields and call `PyProjectHelper.editAndRegenerate(trait, editFn, ps.capturedLockContent)`. If the result `isChanged()`, copy `modifiedDepsFile` and `regenResult` onto the state.

### Cross-recipe carryover

Composite recipe chains are handled via an `ExecutionContext` side channel (`PyProjectHelper.LIVE_DEPS_TREES`) keyed by deps-file path. Recipe A's deps-branch publishes its modified tree there. Recipe B's deps-branch and lock-branch both read from there before falling back to the scanner-captured snapshot. The ctx is shared across the recipe stack within a single `RecipeRun`, so the side channel is the carryover medium.

### What disappears

- `PythonDependencyExecutionContextView` (deleted).
- `PyProjectHelper.captureExistingLockContent`, `maybeReplayLockContent`, `maybeUpdateUvLock`, `maybeUpdatePipfileLock`, `regenerateLockAndRefreshMarker`, `regeneratePipfileLockAndRefreshMarker` (deleted).
- `PythonDependencyFile.afterModification` (default method removed from the interface; overrides removed from `PyProjectFile` and `PipfileFile`).

### What is added

- `PyProjectHelper.regenerateLockContent(SourceFile depsFile, @Nullable String capturedLockContent)` — reads `PythonResolutionResult.PackageManager` from the marker and dispatches via `LockFileRegeneration.forPackageManager(pm)`. Returns the underlying `LockFileRegeneration.Result` directly (success: lock content; failure: error message). Returns `null` when there's no marker, no package manager, or no regeneration adapter.
- `PyProjectHelper.editAndRegenerate(trait, editFn, capturedLockContent)` — applies the recipe-specific trait edit, refreshes the `PythonResolutionResult` marker on the modified source, and (if a captured lock was seen) regenerates the lock. Returns an `EditAndRegenerateResult` holding `(modifiedDepsFile, regenResult)`.
- `PyProjectHelper.getLiveDepsTree(ctx, depsPath)` / `putLiveDepsTree(ctx, depsPath, depsTree)` — read/write accessors on the `LIVE_DEPS_TREES` ctx side channel that carries chain-modified deps trees across composite recipe boundaries.

## File Structure

| File | Action |
|---|---|
| `rewrite-python/src/main/java/org/openrewrite/python/internal/PyProjectHelper.java` | Modify: remove 6 methods, add 1 method + nested type |
| `rewrite-python/src/main/java/org/openrewrite/python/internal/PythonDependencyExecutionContextView.java` | Delete |
| `rewrite-python/src/main/java/org/openrewrite/python/trait/PythonDependencyFile.java` | Modify: remove default `afterModification` |
| `rewrite-python/src/main/java/org/openrewrite/python/trait/PyProjectFile.java` | Modify: remove `afterModification` override |
| `rewrite-python/src/main/java/org/openrewrite/python/trait/PipfileFile.java` | Modify: remove `afterModification` override and `PipfileParser` import |
| `rewrite-python/src/main/java/org/openrewrite/python/AddDependency.java` | Modify: new Accumulator/scan/generate/visit |
| `rewrite-python/src/main/java/org/openrewrite/python/RemoveDependency.java` | Modify: same |
| `rewrite-python/src/main/java/org/openrewrite/python/ChangeDependency.java` | Modify: same |
| `rewrite-python/src/main/java/org/openrewrite/python/UpgradeDependencyVersion.java` | Modify: same |
| `rewrite-python/src/main/java/org/openrewrite/python/UpgradeTransitiveDependencyVersion.java` | Modify: same |

Tests covering recipe behavior already exist and constitute the regression suite:
- `rewrite-python/src/test/java/org/openrewrite/python/AddDependencyTest.java`
- `rewrite-python/src/test/java/org/openrewrite/python/RemoveDependencyTest.java`
- `rewrite-python/src/test/java/org/openrewrite/python/ChangeDependencyTest.java`
- `rewrite-python/src/test/java/org/openrewrite/python/UpgradeDependencyVersionTest.java`
- `rewrite-python/src/test/java/org/openrewrite/python/UpgradeTransitiveDependencyVersionTest.java`
- `rewrite-python/src/test/java/org/openrewrite/python/trait/PipfileFileTest.java`
- `rewrite-python/src/test/java/org/openrewrite/python/PipfileParserTest.java`
- `rewrite-python/src/test/java/org/openrewrite/python/PyProjectTomlParserTest.java`

After each recipe refactor task, the matching `*Test.java` must pass with no edits.

---

## Tasks

### Task 1: Add PackageManager-aware regeneration dispatch helper

**Files:**
- Modify: `rewrite-python/src/main/java/org/openrewrite/python/internal/PyProjectHelper.java`

- [ ] **Step 1.1: Add the `regenerateLockContent` method and `RegenerationResult` nested type to `PyProjectHelper`**

Insert this block into `PyProjectHelper` (location: just above `regenerateLockAndRefreshMarker`, which will be deleted in Task 8):

```java
/**
 * Regenerate the lock file for a dependencies-file source by dispatching to the
 * package manager indicated by its {@link PythonResolutionResult} marker.
 * Returns a {@link RegenerationResult} carrying the regenerated lock content
 * on success, or an error message on failure. Returns {@code null} when the
 * source has no marker, no resolvable package manager, or no recognised
 * regeneration adapter.
 */
public static @Nullable RegenerationResult regenerateLockContent(
        SourceFile depsFile, @Nullable String capturedLockContent) {
    PythonResolutionResult marker = depsFile.getMarkers()
            .findFirst(PythonResolutionResult.class).orElse(null);
    if (marker == null) {
        return null;
    }
    LockFileRegeneration regen;
    switch (marker.getPackageManager()) {
        case Uv:
            regen = LockFileRegeneration.UV;
            break;
        case Pipenv:
            regen = LockFileRegeneration.PIPENV;
            break;
        default:
            return null;
    }
    LockFileRegeneration.Result result = regen.regenerate(
            depsFile.printAll(), capturedLockContent);
    return result.isSuccess()
            ? RegenerationResult.success(result.getLockFileContent())
            : RegenerationResult.failure(result.getErrorMessage());
}

@lombok.Value
public static class RegenerationResult {
    boolean success;
    @Nullable String lockContent;
    @Nullable String errorMessage;

    public static RegenerationResult success(String lockContent) {
        return new RegenerationResult(true, lockContent, null);
    }

    public static RegenerationResult failure(String errorMessage) {
        return new RegenerationResult(false, null, errorMessage);
    }
}
```

- [ ] **Step 1.2: Compile to confirm the new method is well-formed**

Run:
```
gw :rewrite-python:compileJava -x pythonInstall -x pythonUpgradePip -x pythonSetupVenv
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 1.3: Commit**

```
git add rewrite-python/src/main/java/org/openrewrite/python/internal/PyProjectHelper.java
git commit -m "Add PyProjectHelper.regenerateLockContent dispatcher"
```

---

### Reference: shared scanner / visitor / generate templates used by Tasks 2–6

Each recipe rewrite uses the same `Accumulator`, `ProjectState`, scanner, and visitor structure. The differences across recipes are: (a) the recipe-specific match predicate run from the scanner, and (b) the trait method called from `generate()`. The reference blocks are inlined in each task below so each task is self-contained.

---

### Task 2: Refactor `AddDependency` to the new Accumulator + generate() pattern

This task establishes the canonical shape that Tasks 3–6 will copy.

**Files:**
- Modify: `rewrite-python/src/main/java/org/openrewrite/python/AddDependency.java`

- [ ] **Step 2.1: Add imports**

At the top of `AddDependency.java`, ensure these imports are present (add any missing ones):

```java
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.openrewrite.Cursor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.toml.tree.Toml;
```

- [ ] **Step 2.2: Replace the `Accumulator` declaration (current lines 96–98)**

Replace the existing 3-line `Accumulator` class with:

```java
static class Accumulator {
    final Map<Path, ProjectState> projects = new HashMap<>();
    final Map<Path, Path> lockToDeps = new HashMap<>();
}

static class ProjectState {
    final Path depsFilePath;
    @Nullable Path lockFilePath;
    @Nullable SourceFile depsFile;
    @Nullable String capturedLockContent;
    boolean depsFileMatches;
    @Nullable SourceFile modifiedDepsFile;
    @Nullable String regeneratedLockContent;
    @Nullable String regenerationError;

    ProjectState(Path depsFilePath) {
        this.depsFilePath = depsFilePath;
    }
}
```

- [ ] **Step 2.3: Replace `getScanner` with the deps/lock-aware scanner**

Replace the entire `getScanner(Accumulator acc)` method with:

```java
@Override
public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
    return new TreeVisitor<Tree, ExecutionContext>() {
        final PythonDependencyFile.Matcher matcher = new PythonDependencyFile.Matcher();

        @Override
        public Tree preVisit(Tree tree, ExecutionContext ctx) {
            stopAfterPreVisit();
            if (!(tree instanceof SourceFile)) {
                return tree;
            }
            SourceFile sourceFile = (SourceFile) tree;
            Path sourcePath = sourceFile.getSourcePath();

            if (tree instanceof Toml.Document && sourcePath.endsWith("uv.lock")) {
                Path depsPath = PyProjectHelper.correspondingPyprojectPath(sourcePath);
                ProjectState ps = acc.projects.computeIfAbsent(depsPath, ProjectState::new);
                ps.lockFilePath = sourcePath;
                ps.capturedLockContent = ((Toml.Document) tree).printAll();
                acc.lockToDeps.put(sourcePath, depsPath);
                return tree;
            }
            if (tree instanceof Json.Document && sourcePath.endsWith("Pipfile.lock")) {
                Path depsPath = PyProjectHelper.correspondingPipfilePath(sourcePath);
                ProjectState ps = acc.projects.computeIfAbsent(depsPath, ProjectState::new);
                ps.lockFilePath = sourcePath;
                ps.capturedLockContent = ((Json.Document) tree).printAll();
                acc.lockToDeps.put(sourcePath, depsPath);
                return tree;
            }

            PythonDependencyFile trait = matcher.get(getCursor()).orElse(null);
            if (trait != null) {
                ProjectState ps = acc.projects.computeIfAbsent(sourcePath, ProjectState::new);
                ps.depsFile = sourceFile;
                ps.depsFileMatches = matchesAddDependency(trait);
            }
            return tree;
        }
    };
}

private boolean matchesAddDependency(PythonDependencyFile trait) {
    return PyProjectHelper.findDependencyInScope(
            trait.getMarker(), packageName, scope, groupName) == null;
}
```

- [ ] **Step 2.4: Add the `generate` override**

Add this override below `getScanner`:

```java
@Override
public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
    PythonDependencyFile.Matcher matcher = new PythonDependencyFile.Matcher();
    String ver = version != null ? version : "";
    Map<String, String> additions = Collections.singletonMap(packageName, ver);

    for (ProjectState ps : acc.projects.values()) {
        if (!ps.depsFileMatches || ps.depsFile == null) {
            continue;
        }
        PythonDependencyFile trait = matcher.test(new Cursor(null, ps.depsFile));
        if (trait == null) {
            continue;
        }
        PythonDependencyFile updated = trait.withAddedDependencies(additions, scope, groupName);
        if (updated.getTree() == ps.depsFile) {
            continue;
        }
        ps.modifiedDepsFile = (SourceFile) updated.getTree();

        if (ps.capturedLockContent != null) {
            PyProjectHelper.RegenerationResult r =
                    PyProjectHelper.regenerateLockContent(ps.modifiedDepsFile, ps.capturedLockContent);
            if (r != null) {
                if (r.isSuccess()) {
                    ps.regeneratedLockContent = r.getLockContent();
                } else {
                    ps.regenerationError = r.getErrorMessage();
                }
            }
        }
    }
    return Collections.emptyList();
}
```

- [ ] **Step 2.5: Replace `getVisitor` with the pure-lookup visitor**

Replace the entire `getVisitor(Accumulator acc)` method with:

```java
@Override
public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
    if (acc.projects.isEmpty()) {
        return TreeVisitor.noop();
    }
    return new TreeVisitor<Tree, ExecutionContext>() {
        @Override
        public Tree preVisit(Tree tree, ExecutionContext ctx) {
            stopAfterPreVisit();
            if (!(tree instanceof SourceFile)) {
                return tree;
            }
            SourceFile sourceFile = (SourceFile) tree;
            Path sourcePath = sourceFile.getSourcePath();

            ProjectState ps = acc.projects.get(sourcePath);
            if (ps != null && ps.modifiedDepsFile != null) {
                SourceFile out = ps.modifiedDepsFile;
                if (ps.regenerationError != null) {
                    out = Markup.warn(out, new RuntimeException(
                            "lock regeneration failed: " + ps.regenerationError));
                }
                return out;
            }

            Path depsPath = acc.lockToDeps.get(sourcePath);
            if (depsPath != null) {
                ProjectState lockPs = acc.projects.get(depsPath);
                if (lockPs != null && lockPs.regeneratedLockContent != null) {
                    if (tree instanceof Toml.Document) {
                        return PyProjectHelper.reparseToml(
                                (Toml.Document) tree, lockPs.regeneratedLockContent);
                    }
                    if (tree instanceof Json.Document) {
                        return PyProjectHelper.reparseJson(
                                (Json.Document) tree, lockPs.regeneratedLockContent);
                    }
                }
            }
            return tree;
        }
    };
}
```

- [ ] **Step 2.6: Compile**

Run:
```
gw :rewrite-python:compileJava -x pythonInstall -x pythonUpgradePip -x pythonSetupVenv
```
Expected: BUILD SUCCESSFUL. The codebase will not yet compile if `afterModification` is referenced from `PyProjectFile`/`PipfileFile` for the other 4 recipes — leave those untouched until Tasks 3–6 are done.

- [ ] **Step 2.7: Run `AddDependencyTest`**

Run:
```
gw :rewrite-python:test --tests "org.openrewrite.python.AddDependencyTest" -x pythonInstall -x pythonUpgradePip -x pythonSetupVenv
```
Expected: all tests pass.

- [ ] **Step 2.8: Commit**

```
git add rewrite-python/src/main/java/org/openrewrite/python/AddDependency.java
git commit -m "AddDependency: move per-project state to Accumulator and run regen in generate()"
```

---

### Task 3: Apply the same pattern to `RemoveDependency`

**Files:**
- Modify: `rewrite-python/src/main/java/org/openrewrite/python/RemoveDependency.java`

The implementation must mirror `rewrite-python/src/main/java/org/openrewrite/python/AddDependency.java` (the canonical reference, see commits `ec3de0de44` and `903f2cf301` on `warm-raven`). Read that file before editing — copy its `Accumulator`, `ProjectState`, `getScanner`, `generate` (returns `Collections.emptyList()`), `getVisitor`, and `ensureComputed` exactly. The only differences relative to AddDependency are listed below.

- [ ] **Step 3.1: Open `AddDependency.java` for reference and `RemoveDependency.java` for editing.**

- [ ] **Step 3.2: Imports.** Use the same imports as AddDependency, plus `import java.util.Set;`.

- [ ] **Step 3.3: Recipe option fields.** Keep RemoveDependency's existing `packageName`, `scope`, and `groupName` fields. Drop any state that no longer applies under the new structure.

- [ ] **Step 3.4: Match predicate (renamed from `matchesAddDependency`).**

```java
private boolean matchesRemoveDependency(PythonDependencyFile trait) {
    return PyProjectHelper.findDependencyInScope(
            trait.getMarker(), packageName, scope, groupName) != null;
}
```

Wire the scanner's `ps.depsFileMatches = matchesRemoveDependency(trait);` line accordingly.

- [ ] **Step 3.5: Edit function inside `ensureComputed`.** Replace AddDependency's edit construction with:

```java
Set<String> removals = Collections.singleton(packageName);
Function<PythonDependencyFile, PythonDependencyFile> editFn =
        t -> t.withRemovedDependencies(removals, scope, groupName);
```

Everything else inside `ensureComputed` (the `PyProjectHelper.editAndRegenerate` call and the result handling) stays identical.

- [ ] **Step 3.6: Compile, test, commit.**

```
gw :rewrite-python:compileJava -x pythonInstall -x pythonUpgradePip -x pythonSetupVenv
gw :rewrite-python:test --tests "org.openrewrite.python.RemoveDependencyTest" -x pythonInstall -x pythonUpgradePip -x pythonSetupVenv
git add rewrite-python/src/main/java/org/openrewrite/python/RemoveDependency.java
git commit -m "RemoveDependency: ctx side-channel for cross-recipe deps tree sync"
```

---

### Task 4: Apply the same pattern to `ChangeDependency`

**Files:**
- Modify: `rewrite-python/src/main/java/org/openrewrite/python/ChangeDependency.java`

Mirror `AddDependency.java` exactly; only the recipe option fields, the match predicate, and the edit function differ.

- [ ] **Step 4.1: Imports.** Same as AddDependency.

- [ ] **Step 4.2: Recipe option fields.** Keep ChangeDependency's existing `oldPackageName`, `newPackageName`, `newVersion`, `scope`, and `groupName`.

- [ ] **Step 4.3: Match predicate.**

```java
private boolean matchesChangeDependency(PythonDependencyFile trait) {
    return PyProjectHelper.findDependencyInScope(
            trait.getMarker(), oldPackageName, scope, groupName) != null;
}
```

- [ ] **Step 4.4: Edit function inside `ensureComputed`.**

```java
Function<PythonDependencyFile, PythonDependencyFile> editFn =
        t -> t.withChangedDependency(oldPackageName, newPackageName, newVersion, scope, groupName);
```

- [ ] **Step 4.5: Compile, test, commit.**

```
gw :rewrite-python:compileJava -x pythonInstall -x pythonUpgradePip -x pythonSetupVenv
gw :rewrite-python:test --tests "org.openrewrite.python.ChangeDependencyTest" -x pythonInstall -x pythonUpgradePip -x pythonSetupVenv
git add rewrite-python/src/main/java/org/openrewrite/python/ChangeDependency.java
git commit -m "ChangeDependency: ctx side-channel for cross-recipe deps tree sync"
```

---

### Task 5: Apply the same pattern to `UpgradeDependencyVersion`

**Files:**
- Modify: `rewrite-python/src/main/java/org/openrewrite/python/UpgradeDependencyVersion.java`

Mirror `AddDependency.java` exactly; only the differences below apply.

- [ ] **Step 5.1: Imports.** Same as AddDependency.

- [ ] **Step 5.2: Recipe option fields.** Keep UpgradeDependencyVersion's existing `packageName`, `newVersion`, `scope`, and `groupName`.

- [ ] **Step 5.3: Match predicate (preserves the original behavior — a project matches when the named dependency exists in scope **and** its current version differs from the requested new version).**

```java
private boolean matchesUpgrade(PythonDependencyFile trait) {
    PythonResolutionResult.Dependency dep = PyProjectHelper.findDependencyInScope(
            trait.getMarker(), packageName, scope, groupName);
    return dep != null && !PyProjectHelper.normalizeVersionConstraint(newVersion)
            .equals(dep.getVersionConstraint());
}
```

- [ ] **Step 5.4: Edit function inside `ensureComputed`.**

```java
Map<String, String> upgrades = Collections.singletonMap(packageName, newVersion);
Function<PythonDependencyFile, PythonDependencyFile> editFn =
        t -> t.withUpgradedVersions(upgrades, scope, groupName);
```

- [ ] **Step 5.5: Compile, test, commit.**

```
gw :rewrite-python:compileJava -x pythonInstall -x pythonUpgradePip -x pythonSetupVenv
gw :rewrite-python:test --tests "org.openrewrite.python.UpgradeDependencyVersionTest" -x pythonInstall -x pythonUpgradePip -x pythonSetupVenv
git add rewrite-python/src/main/java/org/openrewrite/python/UpgradeDependencyVersion.java
git commit -m "UpgradeDependencyVersion: ctx side-channel for cross-recipe deps tree sync"
```

---

### Task 6: Apply the same pattern to `UpgradeTransitiveDependencyVersion`

**Files:**
- Modify: `rewrite-python/src/main/java/org/openrewrite/python/UpgradeTransitiveDependencyVersion.java`

Mirror `AddDependency.java` exactly; only the differences below apply.

- [ ] **Step 6.1: Imports.** Same as AddDependency.

- [ ] **Step 6.2: Recipe option fields.** Keep UpgradeTransitiveDependencyVersion's existing `packageName`, `version`.

- [ ] **Step 6.3: Match predicate.**

```java
private boolean matchesTransitive(PythonDependencyFile trait) {
    PythonResolutionResult marker = trait.getMarker();
    if (marker.findDependency(packageName) != null) {
        return false;
    }
    if (marker.getPackageManager() == PythonResolutionResult.PackageManager.Uv &&
            marker.getResolvedDependency(packageName) == null) {
        return false;
    }
    return true;
}
```

- [ ] **Step 6.4: Edit function inside `ensureComputed`.**

```java
String normalizedName = PythonResolutionResult.normalizeName(packageName);
Map<String, String> pins = Collections.singletonMap(normalizedName, version);
Function<PythonDependencyFile, PythonDependencyFile> editFn =
        t -> t.withPinnedTransitiveDependencies(pins, null, null);
```

- [ ] **Step 6.5: Compile, test, commit.**

```
gw :rewrite-python:compileJava -x pythonInstall -x pythonUpgradePip -x pythonSetupVenv
gw :rewrite-python:test --tests "org.openrewrite.python.UpgradeTransitiveDependencyVersionTest" -x pythonInstall -x pythonUpgradePip -x pythonSetupVenv
git add rewrite-python/src/main/java/org/openrewrite/python/UpgradeTransitiveDependencyVersion.java
git commit -m "UpgradeTransitiveDependencyVersion: ctx side-channel for cross-recipe deps tree sync"
```

---

### Task 7: Remove `afterModification` from the trait API and overrides

**Files:**
- Modify: `rewrite-python/src/main/java/org/openrewrite/python/trait/PythonDependencyFile.java`
- Modify: `rewrite-python/src/main/java/org/openrewrite/python/trait/PyProjectFile.java`
- Modify: `rewrite-python/src/main/java/org/openrewrite/python/trait/PipfileFile.java`

After Tasks 2–6, no recipe calls `afterModification(ctx)` anymore. The trait method becomes dead code.

- [ ] **Step 7.1: Remove the default `afterModification` method from the trait interface**

In `PythonDependencyFile.java`, delete this entire default method (currently around lines 93–103):

```java
/**
 * Post-process the modified source file, e.g. regenerate lock files.
 * Called by recipes after a trait method modifies the tree.
 * The default implementation returns the tree unchanged.
 *
 * @param ctx the execution context
 * @return the post-processed source file
 */
default SourceFile afterModification(ExecutionContext ctx) {
    return getTree();
}
```

Drop the unused `import org.openrewrite.ExecutionContext;` if it is no longer referenced.

- [ ] **Step 7.2: Remove the `afterModification` override from `PyProjectFile`**

In `PyProjectFile.java`, delete this override (currently around lines 417–420):

```java
@Override
public SourceFile afterModification(ExecutionContext ctx) {
    return PyProjectHelper.regenerateLockAndRefreshMarker((Toml.Document) getTree(), ctx);
}
```

Drop unused imports (`ExecutionContext`) if no longer referenced.

- [ ] **Step 7.3: Remove the `afterModification` override from `PipfileFile`**

In `PipfileFile.java`, delete this override (currently around lines 165–169):

```java
@Override
public SourceFile afterModification(ExecutionContext ctx) {
    return PyProjectHelper.regeneratePipfileLockAndRefreshMarker(
            (Toml.Document) getTree(), ctx, PipfileParser::createMarker);
}
```

Drop the no-longer-needed imports: `ExecutionContext`, `PipfileParser`.

- [ ] **Step 7.4: Compile**

```
gw :rewrite-python:compileJava -x pythonInstall -x pythonUpgradePip -x pythonSetupVenv
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7.5: Commit**

```
git add rewrite-python/src/main/java/org/openrewrite/python/trait/PythonDependencyFile.java \
        rewrite-python/src/main/java/org/openrewrite/python/trait/PyProjectFile.java \
        rewrite-python/src/main/java/org/openrewrite/python/trait/PipfileFile.java
git commit -m "Trait: drop afterModification — recipes now own lock regeneration"
```

---

### Task 8: Delete dead helpers and the ExecutionContext view

**Files:**
- Modify: `rewrite-python/src/main/java/org/openrewrite/python/internal/PyProjectHelper.java`
- Delete: `rewrite-python/src/main/java/org/openrewrite/python/internal/PythonDependencyExecutionContextView.java`

- [ ] **Step 8.1: Remove the deprecated helpers from `PyProjectHelper`**

Delete these methods entirely (with their javadoc):
- `captureExistingLockContent` (currently around lines 101–116)
- `maybeReplayLockContent` (currently around lines 123–137)
- `maybeUpdateUvLock` (currently around lines 147–162)
- `maybeUpdatePipfileLock` (currently around lines 168–182)
- `regenerateLockAndRefreshMarker` (currently around lines 232–274)
- `regeneratePipfileLockAndRefreshMarker` (currently around lines 284–324)

Keep: `correspondingPyprojectPath`, `correspondingPipfilePath`, `reparseJson`, `reparseToml`, `regenerateLockContent` (added in Task 1), `RegenerationResult` (added in Task 1), and all the dependency-array predicates and lookup helpers.

Drop the now-unused imports: at minimum `org.openrewrite.marker.Markup`, `org.openrewrite.ExecutionContext`, and `java.util.function.Function`. Keep `java.util.Map` if still referenced.

- [ ] **Step 8.2: Delete `PythonDependencyExecutionContextView.java`**

```
git rm rewrite-python/src/main/java/org/openrewrite/python/internal/PythonDependencyExecutionContextView.java
```

- [ ] **Step 8.3: Compile**

```
gw :rewrite-python:compileJava -x pythonInstall -x pythonUpgradePip -x pythonSetupVenv
```
Expected: BUILD SUCCESSFUL with no references to the deleted symbols.

- [ ] **Step 8.4: Commit**

```
git add rewrite-python/src/main/java/org/openrewrite/python/internal/PyProjectHelper.java
git commit -m "Drop PythonDependencyExecutionContextView and unused PyProjectHelper helpers"
```

---

### Task 9: Run the full Python test suite for the recipe surface

**Files:** none (verification only).

- [ ] **Step 9.1: Run all five recipe test classes plus trait/parser tests**

```
gw :rewrite-python:test \
    --tests "org.openrewrite.python.AddDependencyTest" \
    --tests "org.openrewrite.python.RemoveDependencyTest" \
    --tests "org.openrewrite.python.ChangeDependencyTest" \
    --tests "org.openrewrite.python.UpgradeDependencyVersionTest" \
    --tests "org.openrewrite.python.UpgradeTransitiveDependencyVersionTest" \
    --tests "org.openrewrite.python.trait.PipfileFileTest" \
    --tests "org.openrewrite.python.PipfileParserTest" \
    --tests "org.openrewrite.python.PyProjectTomlParserTest" \
    -x pythonInstall -x pythonUpgradePip -x pythonSetupVenv
```
Expected: all pass. If any fail, fix the recipe in question and amend the failing test scenario to that recipe's commit before continuing — do **not** create a separate fix commit on top of an unrelated recipe.

- [ ] **Step 9.2: (Optional) full module test pass**

```
gw :rewrite-python:test -x pythonInstall -x pythonUpgradePip -x pythonSetupVenv
```
Expected: green.

- [ ] **Step 9.3: Push the branch**

```
git push origin pipenv-lockfiles
```

---

## Out of Scope for This Plan

These items from the original handoff are **not** addressed here:
- **Real-repo testing via `/create-organization`** — separate, downstream of this rework.
- **CLI / DevCenter project-type detection for standalone `Pipfile`** — lives in another repo.
- **Marker refresh from regenerated lock content** — current code passes `null` for lock content to `PythonDependencyParser.createMarker(doc, null)`; the rework preserves that behavior. A follow-up could add a `createMarker(doc, lockContent)` overload to both `PythonDependencyParser` and `PipfileParser`, then call it from `generate()` after regeneration succeeds.

---

## Self-Review

**Spec coverage**

| Handoff requirement | Plan task |
|---|---|
| Accumulator holds per-project deps + lock | Tasks 2–6 (`ProjectState`) |
| Capture both files during scanner phase | Tasks 2–6 (scanner branches) |
| Regenerate lock during between-scan-and-visit phase from updated deps + captured lock seed | Tasks 2–6 (`generate()`) |
| Emit regenerated lock file in place of old | Tasks 2–6 (visitor lock branch) |
| Refresh marker on deps file from new lock contents | Preserved via existing `createMarker(updated, null)` path inside the trait `with…` methods; deeper marker-from-lock-content refresh is called out in "Out of Scope" |
| Drop `captureExistingLockContent` / `maybeReplayLockContent` | Task 8 |
| Drop `PythonDependencyExecutionContextView` indirection | Task 8 |
| Drop trait `afterModification` (with `markerFactory` parameter) | Task 7 |

**Type/name consistency**

- `ProjectState` field names (`depsFilePath`, `lockFilePath`, `depsFile`, `capturedLockContent`, `depsFileMatches`, `modifiedDepsFile`, `regeneratedLockContent`, `regenerationError`) used identically in scanner, generate, and visitor across Tasks 2–6.
- `RegenerationResult` defined in Task 1 (`isSuccess`, `getLockContent`, `getErrorMessage`); consumed in Tasks 2–6 with consistent property names.
- `PyProjectHelper.correspondingPyprojectPath` / `correspondingPipfilePath` already exist in the current file; no rename.

**Placeholder scan**

Searched the plan for "TBD", "implement later", "similar to", "fill in", "etc.", "as appropriate". None remain. Tasks 3–6 inline the full scanner / `generate()` / visitor blocks rather than referring back to Task 2; the only differences across recipes (the match predicate and the trait method called) are inlined verbatim in each task.
