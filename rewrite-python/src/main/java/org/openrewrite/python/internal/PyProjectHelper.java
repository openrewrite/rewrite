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
import org.openrewrite.python.PipfileParser;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.trait.PythonDependencyFile;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.singletonList;

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
     * Read the unquoted key name from a TOML key-value, or {@code null} if the
     * key isn't a plain {@link Toml.Identifier}. Quote stripping is handled by
     * the TOML parser itself ({@code "urllib3"} and {@code urllib3} both expose
     * {@code "urllib3"} via {@code getName()}).
     */
    public static @Nullable String extractKeyName(Toml.KeyValue kv) {
        return kv.getKey() instanceof Toml.Identifier
                ? ((Toml.Identifier) kv.getKey()).getName()
                : null;
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
     * Reparse a JSON document from new content while preserving the original document's
     * identity (id) and markers.
     */
    public static Json.Document reparseJson(Json.Document original, String newContent) {
        JsonParser parser = new JsonParser();
        Parser.Input input = Parser.Input.fromString(original.getSourcePath(), newContent);
        List<SourceFile> parsed = new ArrayList<>();
        parser.parseInputs(singletonList(input), null,
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
        Toml.Document newDoc = parseToml(original.getSourcePath(), newContent);
        if (newDoc != null) {
            return newDoc.withId(original.getId())
                    .withMarkers(original.getMarkers());
        }
        return original;
    }

    private static Toml.@Nullable Document parseToml(Path sourcePath, String content) {
        TomlParser parser = new TomlParser();
        Parser.Input input = Parser.Input.fromString(sourcePath, content);
        List<SourceFile> parsed = new ArrayList<>();
        parser.parseInputs(singletonList(input), null,
                new InMemoryExecutionContext(Throwable::printStackTrace)).forEach(parsed::add);
        return !parsed.isEmpty() && parsed.get(0) instanceof Toml.Document ? (Toml.Document) parsed.get(0) : null;
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
        PythonResolutionResult newMarker = createMarker(depsFile);
        if (newMarker != null) {
            return depsFile.withMarkers(depsFile.getMarkers().setByType(newMarker.withId(existing.getId())));
        }
        return depsFile;
    }

    /**
     * Derive a pyproject.toml or Pipfile marker from the document's content alone, or {@code null} for other files.
     */
    private static @Nullable PythonResolutionResult createMarker(SourceFile depsFile) {
        if (depsFile instanceof Toml.Document) {
            Toml.Document doc = (Toml.Document) depsFile;
            Path sourcePath = doc.getSourcePath();
            if (sourcePath.endsWith("pyproject.toml")) {
                return PythonDependencyParser.createMarker(doc, null);
            } else if (sourcePath.endsWith("Pipfile")) {
                return PipfileParser.createMarker(doc);
            }
        }
        return null;
    }

    /**
     * Overlay resolved-dependency information from regenerated lock content onto
     * the source file's existing {@link PythonResolutionResult} marker. Dispatches
     * on the marker's package manager (uv → {@link UvLockParser}; pipenv →
     * {@link PipfileLockParser}). Returns the source file unchanged if there is no
     * marker, no recognised package manager, or the lock content has no resolved
     * dependencies.
     */
    public static SourceFile applyResolvedDependencies(SourceFile depsFile, String regeneratedLockContent) {
        PythonResolutionResult existing = depsFile.getMarkers()
                .findFirst(PythonResolutionResult.class).orElse(null);
        if (existing == null || existing.getPackageManager() == null) {
            return depsFile;
        }
        List<PythonResolutionResult.ResolvedDependency> resolved;
        PythonResolutionResult overlaid;
        switch (existing.getPackageManager()) {
            case Uv:
                resolved = UvLockParser.parse(regeneratedLockContent);
                if (resolved.isEmpty()) {
                    return depsFile;
                }
                overlaid = PythonResolutionLinker.applyPyproject(existing, resolved,
                        PythonResolutionResult.PackageManager.Uv);
                break;
            case Poetry:
                resolved = PoetryLockParser.parse(regeneratedLockContent);
                if (resolved.isEmpty()) {
                    return depsFile;
                }
                overlaid = PythonResolutionLinker.applyPyproject(existing, resolved,
                        PythonResolutionResult.PackageManager.Poetry);
                break;
            case Pdm:
                resolved = PdmLockParser.parse(regeneratedLockContent);
                if (resolved.isEmpty()) {
                    return depsFile;
                }
                overlaid = PythonResolutionLinker.applyPyproject(existing, resolved,
                        PythonResolutionResult.PackageManager.Pdm);
                break;
            case Pipenv:
                resolved = PipfileLockParser.parse(regeneratedLockContent);
                if (resolved.isEmpty()) {
                    return depsFile;
                }
                overlaid = PythonResolutionLinker.applyPipfile(existing, resolved);
                break;
            default:
                return depsFile;
        }
        return depsFile.withMarkers(depsFile.getMarkers().setByType(overlaid.withId(existing.getId())));
    }

    /**
     * ExecutionContext key for the {@code Map<Path, LiveDeps>} that holds the
     * latest chain-modified deps file for each project path. Recipes write to this
     * after applying their edit so that subsequent recipes (or the same recipe's
     * lock-file visit when lock is visited before deps) can read the up-to-date
     * deps file.
     */
    private static final String LIVE_DEPS = "org.openrewrite.python.liveDeps";

    /**
     * A chain-modified deps file kept as its text and the lock regenerated with it, from which its tree is rebuilt.
     */
    @lombok.Value
    private static class LiveDeps {
        String content;
        @Nullable String lockContent;
    }

    /**
     * Rebuild the latest known chain-modified deps tree for a given pyproject.toml
     * or Pipfile from the shared {@link ExecutionContext} side channel, or return
     * {@code null} if no recipe has written one yet.
     */
    @SuppressWarnings("unchecked")
    public static @Nullable SourceFile getLiveDepsTree(ExecutionContext ctx, Path depsPath) {
        Map<Path, LiveDeps> map = (Map<Path, LiveDeps>) ctx.getMessage(LIVE_DEPS);
        LiveDeps live = map == null ? null : map.get(depsPath);
        if (live == null) {
            return null;
        }
        Toml.Document doc = parseToml(depsPath, live.getContent());
        if (doc == null) {
            return null;
        }
        PythonResolutionResult marker = createMarker(doc);
        if (marker == null) {
            return null;
        }
        SourceFile depsTree = doc.withMarkers(doc.getMarkers().add(marker));
        return live.getLockContent() == null ? depsTree : applyResolvedDependencies(depsTree, live.getLockContent());
    }

    /**
     * Publish the current chain-modified deps file for a given path to the shared
     * {@link ExecutionContext} side channel. Subsequent recipes — and lock-file
     * visits within the same recipe pass that arrive after this one — read this
     * to apply their edit on top of prior recipes' modifications.
     *
     * @param depsContent the edited deps file's text, without any markup added to report a failure
     * @param regen       the lock regeneration that went with the edit, or {@code null}
     */
    public static void putLiveDeps(ExecutionContext ctx, Path depsPath, String depsContent,
                                   LockFileRegeneration.@Nullable Result regen) {
        Map<Path, LiveDeps> map = ctx.computeMessageIfAbsent(LIVE_DEPS, k -> new HashMap<>());
        map.put(depsPath, new LiveDeps(depsContent,
                regen != null && regen.isSuccess() ? regen.getLockFileContent() : null));
    }

    /**
     * Apply a recipe-specific trait edit to a deps tree, refresh its marker, and
     * regenerate the lock file. Used by deps-file visits, which obtain the trait
     * from the framework's live cursor, when the lock file has not been visited yet.
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
            Function<PythonDependencyFile, PythonDependencyFile> editFn,
            @Nullable String capturedLockContent) {
        return editAndRegenerate(trait, editFn, capturedLockContent, new InMemoryExecutionContext());
    }

    public static EditAndRegenerateResult editAndRegenerate(
            PythonDependencyFile trait,
            Function<PythonDependencyFile, PythonDependencyFile> editFn,
            @Nullable String capturedLockContent,
            ExecutionContext ctx) {
        SourceFile modified = applyEdit(trait, editFn);
        if (modified == null) {
            return EditAndRegenerateResult.unchanged();
        }
        String originalDepsContent = ((SourceFile) trait.getTree()).printAll();
        LockFileRegeneration.Result regen = capturedLockContent == null ? null
                : regenerateLockContent(modified, originalDepsContent, capturedLockContent, ctx);
        return EditAndRegenerateResult.changed(applyRegeneratedLock(modified, regen), regen);
    }

    /**
     * Apply a recipe-specific trait edit to a deps tree whose lock file was visited
     * and regenerated first, refresh its marker and overlay that regeneration.
     *
     * @return the edited deps tree, or {@code null} when the edit changes nothing
     */
    public static @Nullable SourceFile edit(PythonDependencyFile trait,
                                            Function<PythonDependencyFile, PythonDependencyFile> editFn,
                                            LockFileRegeneration.@Nullable Result regen) {
        SourceFile modified = applyEdit(trait, editFn);
        return modified == null ? null : applyRegeneratedLock(modified, regen);
    }

    private static @Nullable SourceFile applyEdit(PythonDependencyFile trait,
                                                  Function<PythonDependencyFile, PythonDependencyFile> editFn) {
        PythonDependencyFile updated = editFn.apply(trait);
        return updated.getTree() == trait.getTree() ? null : refreshMarker((SourceFile) updated.getTree());
    }

    private static SourceFile applyRegeneratedLock(SourceFile modified, LockFileRegeneration.@Nullable Result regen) {
        if (regen != null && regen.isSuccess() && regen.getLockFileContent() != null) {
            return applyResolvedDependencies(modified, regen.getLockFileContent());
        }
        return modified;
    }

    /**
     * A recipe's edit of a deps file as text, which is what regenerating its lock file
     * needs when the lock file is visited before the deps file.
     */
    @lombok.Value
    public static class DepsEdit {
        String originalContent;
        String modifiedContent;
        PythonResolutionResult.@Nullable PackageManager packageManager;

        /**
         * Apply a recipe-specific trait edit to a deps tree, or return {@code null} when it changes nothing.
         */
        public static @Nullable DepsEdit of(PythonDependencyFile trait,
                                            Function<PythonDependencyFile, PythonDependencyFile> editFn) {
            SourceFile modified = applyEdit(trait, editFn);
            if (modified == null) {
                return null;
            }
            PythonResolutionResult marker = modified.getMarkers()
                    .findFirst(PythonResolutionResult.class).orElse(null);
            return new DepsEdit(((SourceFile) trait.getTree()).printAll(), modified.printAll(),
                    marker == null ? null : marker.getPackageManager());
        }

        /**
         * Regenerate the lock file for this edit, or return {@code null} when there is no
         * lock content or no regeneration adapter for the package manager.
         */
        public LockFileRegeneration.@Nullable Result regenerate(@Nullable String capturedLockContent,
                                                                ExecutionContext ctx) {
            LockFileRegeneration regen = LockFileRegeneration.forPackageManager(packageManager);
            if (regen == null || capturedLockContent == null) {
                return null;
            }
            return regen.regenerate(modifiedContent, originalContent, capturedLockContent, ctx);
        }
    }

    @lombok.Value
    public static class EditAndRegenerateResult {
        @Nullable SourceFile modifiedDepsFile;
        LockFileRegeneration.@Nullable Result regenResult;

        public boolean isChanged() {
            return modifiedDepsFile != null;
        }

        public static EditAndRegenerateResult unchanged() {
            return new EditAndRegenerateResult(null, null);
        }

        public static EditAndRegenerateResult changed(
                SourceFile modified, LockFileRegeneration.@Nullable Result regen) {
            return new EditAndRegenerateResult(modified, regen);
        }
    }

    /**
     * Regenerate the lock file for a dependencies-file source by dispatching to the
     * package manager indicated by its {@link PythonResolutionResult} marker.
     * Returns {@code null} when the source has no marker, no package manager, or
     * no regeneration adapter for that package manager.
     */
    public static LockFileRegeneration.@Nullable Result regenerateLockContent(
            SourceFile depsFile, @Nullable String capturedLockContent) {
        return regenerateLockContent(depsFile, capturedLockContent, new InMemoryExecutionContext());
    }

    public static LockFileRegeneration.@Nullable Result regenerateLockContent(
            SourceFile depsFile, @Nullable String capturedLockContent, ExecutionContext ctx) {
        return regenerateLockContent(depsFile, null, capturedLockContent, ctx);
    }

    /**
     * @param originalDepsContent the pre-edit dependencies-file content, or {@code null}; lets the
     *                            engine reconcile only the packages the edit changed (see
     *                            {@link LockFileRegeneration#regenerate}).
     */
    public static LockFileRegeneration.@Nullable Result regenerateLockContent(
            SourceFile depsFile, @Nullable String originalDepsContent,
            @Nullable String capturedLockContent, ExecutionContext ctx) {
        PythonResolutionResult marker = depsFile.getMarkers()
                .findFirst(PythonResolutionResult.class).orElse(null);
        if (marker == null) {
            return null;
        }
        LockFileRegeneration regen = LockFileRegeneration.forPackageManager(marker.getPackageManager());
        if (regen == null) {
            return null;
        }
        return regen.regenerate(depsFile.printAll(), originalDepsContent, capturedLockContent, ctx);
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
        if (scope == null) {
            // default: all scopes, matching the null-scope edit paths (Pipfile dev-packages, pyproject groups)
            return marker.findDependencyInAnyScope(packageName);
        } else if ("project.dependencies".equals(scope)) {
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
