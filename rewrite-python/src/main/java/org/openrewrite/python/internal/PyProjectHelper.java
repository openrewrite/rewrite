/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.python.PipfileParser;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.trait.PythonDependencyFile;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Shared utilities for Python dependency recipes operating on pyproject.toml files.
 */
@UtilityClass
public class PyProjectHelper {

    /**
     * Normalize a version constraint so it is valid PEP 508. When the value
     * does not start with a comparison operator ({@code >=}, {@code <=}, etc.)
     * we default to {@code >=}.
     */
    public static String normalizeVersionConstraint(String version) {
        String trimmed = version.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        char first = trimmed.charAt(0);
        if (first == '>' || first == '<' || first == '=' || first == '!' || first == '~') {
            return trimmed;
        }
        return ">=" + trimmed;
    }

    /**
     * Extract the package name from a PEP 508 dependency spec string.
     * The name is the first token before any version specifier, extras, or marker.
     */
    public static @Nullable String extractPackageName(String pep508Spec) {
        String trimmed = pep508Spec.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int end = 0;
        while (end < trimmed.length()) {
            char c = trimmed.charAt(end);
            if (c == '[' || c == '>' || c == '<' || c == '=' || c == '!' || c == '~' || c == ';' || c == ' ' || c == '@') {
                break;
            }
            end++;
        }
        String name = trimmed.substring(0, end).trim();
        return name.isEmpty() ? null : name;
    }

    /**
     * Derive the pyproject.toml path that corresponds to a uv.lock path.
     */
    public static Path correspondingPyprojectPath(Path uvLockPath) {
        return uvLockPath.resolveSibling("pyproject.toml");
    }

    /**
     * Derive the Pipfile path that corresponds to a Pipfile.lock path.
     */
    public static Path correspondingPipfilePath(Path pipfileLockPath) {
        return pipfileLockPath.resolveSibling("Pipfile");
    }

    /**
     * If {@code tree} is a Python lock file (uv.lock or Pipfile.lock), capture
     * its current content into the shared {@link PythonDependencyExecutionContextView}
     * so downstream recipes can seed regeneration from it.
     *
     * @return {@code true} when the tree was recognized as a lock file
     */
    public static boolean captureExistingLockContent(SourceFile sourceFile, Tree tree, ExecutionContext ctx) {
        Path sourcePath = sourceFile.getSourcePath();
        if (tree instanceof Toml.Document && sourcePath.endsWith("uv.lock")) {
            PythonDependencyExecutionContextView.view(ctx).getExistingLockContents().put(
                    correspondingPyprojectPath(sourcePath),
                    ((Toml.Document) tree).printAll());
            return true;
        }
        if (tree instanceof Json.Document && sourcePath.endsWith("Pipfile.lock")) {
            PythonDependencyExecutionContextView.view(ctx).getExistingLockContents().put(
                    correspondingPipfilePath(sourcePath),
                    ((Json.Document) tree).printAll());
            return true;
        }
        return false;
    }

    /**
     * If a previous visitor regenerated lock content for this file, replay it here.
     * Returns the updated tree, or {@code null} if the tree is not a lock file or
     * there is no pending update.
     */
    public static @Nullable Tree maybeReplayLockContent(Tree tree, ExecutionContext ctx) {
        if (tree instanceof Toml.Document) {
            Toml.Document doc = (Toml.Document) tree;
            if (doc.getSourcePath().endsWith("uv.lock")) {
                return maybeUpdateUvLock(doc, ctx);
            }
        }
        if (tree instanceof Json.Document) {
            Json.Document doc = (Json.Document) tree;
            if (doc.getSourcePath().endsWith("Pipfile.lock")) {
                return maybeUpdatePipfileLock(doc, ctx);
            }
        }
        return null;
    }

    /**
     * If there is regenerated uv.lock content for this document, reparse the document
     * from that content. Returns {@code null} when no update is needed (either no
     * regenerated content exists, or the current document already matches).
     * <p>
     * After reparsing, the stored content is normalized to the printer output so that
     * subsequent recipe cycles see identical content and do not trigger spurious changes.
     */
    public static Toml.@Nullable Document maybeUpdateUvLock(Toml.Document document, ExecutionContext ctx) {
        PythonDependencyExecutionContextView view = PythonDependencyExecutionContextView.view(ctx);
        Path pyprojectPath = correspondingPyprojectPath(document.getSourcePath());
        String newContent = view.getUpdatedLockFiles().get(pyprojectPath);
        if (newContent == null) {
            return null;
        }
        Toml.Document reparsed = reparseToml(document, newContent);
        String reparsedContent = reparsed.printAll();
        if (reparsedContent.equals(document.printAll())) {
            return null;
        }
        // Normalize stored content to printer output for round-trip stability
        view.getUpdatedLockFiles().put(pyprojectPath, reparsedContent);
        return reparsed;
    }

    /**
     * If there is regenerated Pipfile.lock content for this document, reparse the
     * document from that content. Returns {@code null} when no update is needed.
     */
    public static Json.@Nullable Document maybeUpdatePipfileLock(Json.Document document, ExecutionContext ctx) {
        PythonDependencyExecutionContextView view = PythonDependencyExecutionContextView.view(ctx);
        Path pipfilePath = correspondingPipfilePath(document.getSourcePath());
        String newContent = view.getUpdatedLockFiles().get(pipfilePath);
        if (newContent == null) {
            return null;
        }
        Json.Document reparsed = reparseJson(document, newContent);
        String reparsedContent = reparsed.printAll();
        if (reparsedContent.equals(document.printAll())) {
            return null;
        }
        view.getUpdatedLockFiles().put(pipfilePath, reparsedContent);
        return reparsed;
    }

    /**
     * Reparse a JSON document from new content while preserving the original document's
     * identity (id) and markers.
     */
    public static Json.Document reparseJson(Json.Document original, String newContent) {
        JsonParser parser = new JsonParser();
        Parser.Input input = Parser.Input.fromString(original.getSourcePath(), newContent);
        List<SourceFile> parsed = new ArrayList<>();
        parser.parseInputs(Collections.singletonList(input), null,
                new InMemoryExecutionContext(Throwable::printStackTrace)).forEach(parsed::add);
        if (!parsed.isEmpty() && parsed.get(0) instanceof Json.Document) {
            Json.Document newDoc = (Json.Document) parsed.get(0);
            return newDoc.withId(original.getId())
                    .withMarkers(original.getMarkers());
        }
        return original;
    }

    /**
     * Reparse a TOML document from new content while preserving the original document's
     * identity (id) and markers.
     */
    public static Toml.Document reparseToml(Toml.Document original, String newContent) {
        TomlParser parser = new TomlParser();
        Parser.Input input = Parser.Input.fromString(original.getSourcePath(), newContent);
        List<SourceFile> parsed = new ArrayList<>();
        parser.parseInputs(Collections.singletonList(input), null,
                new InMemoryExecutionContext(Throwable::printStackTrace)).forEach(parsed::add);
        if (!parsed.isEmpty() && parsed.get(0) instanceof Toml.Document) {
            Toml.Document newDoc = (Toml.Document) parsed.get(0);
            return newDoc.withId(original.getId())
                    .withMarkers(original.getMarkers());
        }
        return original;
    }

    /**
     * Re-derive the {@link PythonResolutionResult} marker on a modified dependency
     * source file by re-parsing its current document content. This is needed after
     * structural edits (adding/removing/changing dependencies) so that the marker's
     * declared-dependency list reflects the new content; otherwise idempotency
     * checks in subsequent recipe cycles see stale data and re-apply the edit.
     * Returns the source file unchanged if no marker is present or the file shape
     * is not recognised.
     */
    public static SourceFile refreshMarker(SourceFile depsFile) {
        PythonResolutionResult existing = depsFile.getMarkers()
                .findFirst(PythonResolutionResult.class).orElse(null);
        if (existing == null) {
            return depsFile;
        }
        if (depsFile instanceof Toml.Document) {
            Toml.Document doc = (Toml.Document) depsFile;
            Path sourcePath = doc.getSourcePath();
            if (sourcePath.endsWith("pyproject.toml")) {
                PythonResolutionResult newMarker = PythonDependencyParser.createMarker(doc, null);
                if (newMarker != null) {
                    return doc.withMarkers(doc.getMarkers().setByType(newMarker.withId(existing.getId())));
                }
            } else if (sourcePath.endsWith("Pipfile")) {
                PythonResolutionResult newMarker = PipfileParser.createMarker(doc);
                if (newMarker != null) {
                    return doc.withMarkers(doc.getMarkers().setByType(newMarker.withId(existing.getId())));
                }
            }
        }
        return depsFile;
    }

    /**
     * ExecutionContext key for the {@code Map<Path, SourceFile>} that holds the
     * latest chain-modified deps tree for each project path. Recipes write to this
     * after applying their edit so that subsequent recipes (or the same recipe's
     * lock-file visit when lock is visited before deps) can read the up-to-date
     * deps tree.
     */
    private static final String LIVE_DEPS_TREES = "org.openrewrite.python.liveDepsTrees";

    /**
     * Read the latest known chain-modified deps tree for a given path from the
     * shared {@link ExecutionContext} side channel, or {@code null} if no recipe
     * has written one yet.
     */
    @SuppressWarnings("unchecked")
    public static @Nullable SourceFile getLiveDepsTree(ExecutionContext ctx, Path depsPath) {
        Map<Path, SourceFile> map = (Map<Path, SourceFile>) ctx.getMessage(LIVE_DEPS_TREES);
        return map == null ? null : map.get(depsPath);
    }

    /**
     * Publish the current chain-modified deps tree for a given path to the shared
     * {@link ExecutionContext} side channel. Subsequent recipes — and lock-file
     * visits within the same recipe pass that arrive after this one — read this
     * to apply their edit on top of prior recipes' modifications.
     */
    public static void putLiveDepsTree(ExecutionContext ctx, Path depsPath, SourceFile depsTree) {
        Map<Path, SourceFile> map = ctx.computeMessageIfAbsent(LIVE_DEPS_TREES, k -> new java.util.HashMap<>());
        map.put(depsPath, depsTree);
    }

    /**
     * Apply a recipe-specific trait edit to a deps tree, refresh its marker, and
     * regenerate the lock file. Used both by deps-file visits (which obtain the
     * trait from the framework's live cursor) and by lock-file lazy-compute visits
     * (which obtain the trait via a synthetic cursor over a tree pulled from the
     * accumulator or the {@link #getLiveDepsTree(ExecutionContext, Path) ctx side
     * channel}).
     *
     * @param trait               the trait wrapping the deps tree to edit
     * @param editFn              applies the recipe-specific edit (e.g.
     *                            {@code t -> t.withAddedDependencies(...)})
     * @param capturedLockContent the lock content captured during scanning, or
     *                            {@code null} when no lock file was seen
     * @return a result describing what changed
     */
    public static EditAndRegenerateResult editAndRegenerate(
            PythonDependencyFile trait,
            java.util.function.Function<PythonDependencyFile, PythonDependencyFile> editFn,
            @Nullable String capturedLockContent) {
        PythonDependencyFile updated = editFn.apply(trait);
        if (updated.getTree() == trait.getTree()) {
            return EditAndRegenerateResult.unchanged();
        }
        SourceFile modified = refreshMarker((SourceFile) updated.getTree());
        String regen = null;
        String error = null;
        if (capturedLockContent != null) {
            RegenerationResult r = regenerateLockContent(modified, capturedLockContent);
            if (r != null) {
                if (r.isSuccess()) {
                    regen = r.getLockContent();
                } else {
                    error = r.getErrorMessage();
                }
            }
        }
        return EditAndRegenerateResult.changed(modified, regen, error);
    }

    @lombok.Value
    public static class EditAndRegenerateResult {
        @Nullable SourceFile modifiedDepsFile;
        @Nullable String regeneratedLockContent;
        @Nullable String regenerationError;

        public boolean isChanged() {
            return modifiedDepsFile != null;
        }

        public static EditAndRegenerateResult unchanged() {
            return new EditAndRegenerateResult(null, null, null);
        }

        public static EditAndRegenerateResult changed(
                SourceFile modified, @Nullable String regen, @Nullable String error) {
            return new EditAndRegenerateResult(modified, regen, error);
        }
    }

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
        if (marker.getPackageManager() == null) {
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

    /**
     * After modifying a pyproject.toml document, regenerate the uv.lock file and
     * refresh the {@link PythonResolutionResult} marker. Returns the updated document.
     * <p>
     * Lock file state is shared across all Python dependency recipes via the
     * {@link ExecutionContext}, so sequential recipes in a composite correctly
     * build on each other's lock regeneration results.
     *
     * @param updated the modified pyproject.toml document
     * @param ctx     the execution context holding shared lock file state
     * @return the document with refreshed marker (and possibly a warning markup)
     */
    public static Toml.Document regenerateLockAndRefreshMarker(
            Toml.Document updated,
            ExecutionContext ctx) {
        PythonDependencyExecutionContextView view = PythonDependencyExecutionContextView.view(ctx);
        Map<Path, String> updatedLockFiles = view.getUpdatedLockFiles();
        Map<Path, String> existingLockContents = view.getExistingLockContents();

        PythonResolutionResult marker = updated.getMarkers()
                .findFirst(PythonResolutionResult.class).orElse(null);

        Path sourcePath = updated.getSourcePath();

        // Attempt lock regeneration when we know a uv.lock exists — either
        // captured during scanning or already regenerated by a prior recipe
        boolean lockKnown = existingLockContents.containsKey(sourcePath) ||
                updatedLockFiles.containsKey(sourcePath) ||
                (marker != null && !marker.getResolvedDependencies().isEmpty());

        if (marker != null && lockKnown) {
            String pyprojectContent = updated.printAll();
            // Prefer the most recently regenerated lock; fall back to original
            String seedLock = updatedLockFiles.getOrDefault(sourcePath,
                    existingLockContents.get(sourcePath));

            LockFileRegeneration.Result lockResult = LockFileRegeneration.UV.regenerate(pyprojectContent, seedLock);
            if (lockResult.isSuccess()) {
                updatedLockFiles.put(sourcePath, lockResult.getLockFileContent());
                existingLockContents.put(sourcePath, lockResult.getLockFileContent());
            } else {
                updated = Markup.warn(updated, new RuntimeException(
                        "uv lock regeneration failed: " + lockResult.getErrorMessage()));
            }
        }

        if (marker != null) {
            PythonResolutionResult newMarker = PythonDependencyParser.createMarker(updated, null);
            if (newMarker != null) {
                updated = updated.withMarkers(updated.getMarkers().setByType(newMarker.withId(marker.getId())));
            }
        }

        return updated;
    }

    /**
     * After modifying a Pipfile document, regenerate the Pipfile.lock file and
     * refresh the {@link PythonResolutionResult} marker. Returns the updated document.
     * <p>
     * The {@code markerFactory} callback rebuilds the marker from the updated
     * Pipfile contents — passed in to avoid coupling this internal class to the
     * public {@code PipfileParser}.
     */
    public static Toml.Document regeneratePipfileLockAndRefreshMarker(
            Toml.Document updated,
            ExecutionContext ctx,
            Function<Toml.Document, PythonResolutionResult> markerFactory) {
        PythonDependencyExecutionContextView view = PythonDependencyExecutionContextView.view(ctx);
        Map<Path, String> updatedLockFiles = view.getUpdatedLockFiles();
        Map<Path, String> existingLockContents = view.getExistingLockContents();

        PythonResolutionResult marker = updated.getMarkers()
                .findFirst(PythonResolutionResult.class).orElse(null);

        Path sourcePath = updated.getSourcePath();

        boolean lockKnown = existingLockContents.containsKey(sourcePath) ||
                updatedLockFiles.containsKey(sourcePath) ||
                (marker != null && !marker.getResolvedDependencies().isEmpty());

        if (marker != null && lockKnown) {
            String pipfileContent = updated.printAll();
            String seedLock = updatedLockFiles.getOrDefault(sourcePath,
                    existingLockContents.get(sourcePath));

            LockFileRegeneration.Result lockResult = LockFileRegeneration.PIPENV.regenerate(pipfileContent, seedLock);
            if (lockResult.isSuccess()) {
                updatedLockFiles.put(sourcePath, lockResult.getLockFileContent());
                existingLockContents.put(sourcePath, lockResult.getLockFileContent());
            } else {
                updated = Markup.warn(updated, new RuntimeException(
                        "pipenv lock regeneration failed: " + lockResult.getErrorMessage()));
            }
        }

        if (marker != null) {
            PythonResolutionResult newMarker = markerFactory.apply(updated);
            if (newMarker != null) {
                updated = updated.withMarkers(updated.getMarkers().setByType(newMarker.withId(marker.getId())));
            }
        }

        return updated;
    }

    /**
     * Check whether a cursor path represents a position inside
     * the {@code [project].dependencies} array in a pyproject.toml.
     */
    public static boolean isInsideProjectDependencies(Cursor cursor) {
        return isInsideDependencyArray(cursor, null, null);
    }

    /**
     * Check whether a cursor path represents a position inside a dependency array
     * for the given scope and optional group name.
     * <p>
     * Scope values use TOML dotted-key path syntax:
     * <ul>
     *   <li>{@code null} or {@code "project.dependencies"} → {@code [project].dependencies}</li>
     *   <li>{@code "build-system.requires"} → {@code [build-system].requires}</li>
     *   <li>{@code "project.optional-dependencies"} → {@code [project.optional-dependencies].<groupName>}</li>
     *   <li>{@code "dependency-groups"} → {@code [dependency-groups].<groupName>}</li>
     *   <li>{@code "tool.uv.constraint-dependencies"} → {@code [tool.uv].constraint-dependencies}</li>
     *   <li>{@code "tool.uv.override-dependencies"} → {@code [tool.uv].override-dependencies}</li>
     * </ul>
     */
    public static boolean isInsideDependencyArray(Cursor cursor, @Nullable String scope, @Nullable String groupName) {
        String tableName;
        String keyName;
        if (scope == null || "project.dependencies".equals(scope)) {
            tableName = "project";
            keyName = "dependencies";
        } else if ("build-system.requires".equals(scope)) {
            tableName = "build-system";
            keyName = "requires";
        } else if ("project.optional-dependencies".equals(scope)) {
            tableName = "project.optional-dependencies";
            keyName = groupName;
        } else if ("dependency-groups".equals(scope)) {
            tableName = "dependency-groups";
            keyName = groupName;
        } else if ("tool.uv.constraint-dependencies".equals(scope)) {
            tableName = "tool.uv";
            keyName = "constraint-dependencies";
        } else if ("tool.uv.override-dependencies".equals(scope)) {
            tableName = "tool.uv";
            keyName = "override-dependencies";
        } else {
            return false;
        }

        if (keyName == null) {
            return false;
        }

        Cursor parent = cursor.getParentTreeCursor();
        if (!(parent.getValue() instanceof Toml.KeyValue)) {
            return false;
        }
        Toml.KeyValue kv = parent.getValue();
        if (!(kv.getKey() instanceof Toml.Identifier) ||
                !keyName.equals(((Toml.Identifier) kv.getKey()).getName())) {
            return false;
        }

        Cursor tableParent = parent.getParentTreeCursor();
        if (!(tableParent.getValue() instanceof Toml.Table)) {
            return false;
        }
        Toml.Table table = tableParent.getValue();
        if (table.getName() == null) {
            return false;
        }

        if (tableName.equals(table.getName().getName())) {
            return true;
        }

        // For project.optional-dependencies, also handle the nested form where
        // [project] contains optional-dependencies = { groupName = [...] }
        if ("project.optional-dependencies".equals(scope) && "project".equals(table.getName().getName())) {
            // Check if parent KV is inside another KV with key "optional-dependencies"
            Cursor kvCursor = parent;
            while (kvCursor != null) {
                Object val = kvCursor.getValue();
                if (val instanceof Toml.KeyValue) {
                    Toml.KeyValue outerKv = (Toml.KeyValue) val;
                    if (outerKv.getKey() instanceof Toml.Identifier &&
                            "optional-dependencies".equals(((Toml.Identifier) outerKv.getKey()).getName())) {
                        return true;
                    }
                }
                kvCursor = kvCursor.getParent();
            }
        }

        return false;
    }

    /**
     * Find a dependency in the specified scope of the marker.
     *
     * @param marker      the resolution result marker
     * @param packageName the package name to find
     * @param scope       the scope to search (null means project.dependencies)
     * @param groupName   the group name (required for optional-dependencies and dependency-groups)
     * @return the dependency, or null if not found
     */
    public static @Nullable Dependency findDependencyInScope(
            PythonResolutionResult marker,
            String packageName,
            @Nullable String scope,
            @Nullable String groupName) {
        if (scope == null || "project.dependencies".equals(scope)) {
            return marker.findDependency(packageName);
        } else if ("build-system.requires".equals(scope)) {
            return findInList(marker.getBuildRequires(), packageName);
        } else if ("project.optional-dependencies".equals(scope)) {
            if (groupName == null) {
                return null;
            }
            List<Dependency> deps = marker.getOptionalDependencies().get(groupName);
            return deps != null ? findInList(deps, packageName) : null;
        } else if ("dependency-groups".equals(scope)) {
            if (groupName == null) {
                return null;
            }
            List<Dependency> deps = marker.getDependencyGroups().get(groupName);
            return deps != null ? findInList(deps, packageName) : null;
        } else if ("tool.uv.constraint-dependencies".equals(scope)) {
            return findInList(marker.getConstraintDependencies(), packageName);
        } else if ("tool.uv.override-dependencies".equals(scope) || "tool.pdm.overrides".equals(scope)) {
            return findInList(marker.getOverrideDependencies(), packageName);
        }
        return null;
    }

    /**
     * Check whether a cursor path represents a position inside the
     * {@code [tool.pdm.overrides]} table in a pyproject.toml.
     */
    public static boolean isInsidePdmOverridesTable(Cursor cursor) {
        Toml.Table table = cursor.firstEnclosing(Toml.Table.class);
        return table != null && table.getName() != null &&
                "tool.pdm.overrides".equals(table.getName().getName());
    }

    private static @Nullable Dependency findInList(List<Dependency> deps, String packageName) {
        String normalized = PythonResolutionResult.normalizeName(packageName);
        for (Dependency dep : deps) {
            if (PythonResolutionResult.normalizeName(dep.getName()).equals(normalized)) {
                return dep;
            }
        }
        return null;
    }
}
