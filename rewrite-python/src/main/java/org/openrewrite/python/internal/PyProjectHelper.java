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
import org.openrewrite.marker.Markup;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            if (c == '[' || c == '>' || c == '<' || c == '=' || c == '!' || c == '~' || c == ';' || c == ' ') {
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
    public static String correspondingPyprojectPath(String uvLockPath) {
        if (uvLockPath.contains("/")) {
            return uvLockPath.substring(0, uvLockPath.lastIndexOf('/') + 1) + "pyproject.toml";
        }
        return "pyproject.toml";
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
     * After modifying a pyproject.toml document, regenerate the uv.lock file and
     * refresh the {@link PythonResolutionResult} marker. Returns the updated document.
     *
     * @param updated          the modified pyproject.toml document
     * @param updatedLockFiles map to store regenerated lock content keyed by pyproject path
     * @return the document with refreshed marker (and possibly a warning markup)
     */
    public static Toml.Document regenerateLockAndRefreshMarker(
            Toml.Document updated,
            java.util.Map<String, String> updatedLockFiles) {
        PythonResolutionResult marker = updated.getMarkers()
                .findFirst(PythonResolutionResult.class).orElse(null);

        // Only attempt lock regeneration when resolved dependencies exist,
        // indicating a uv.lock was originally present
        if (marker != null && !marker.getResolvedDependencies().isEmpty()) {
            String sourcePath = updated.getSourcePath().toString();
            String pyprojectContent = updated.printAll();

            UvLockRegeneration.Result lockResult = UvLockRegeneration.regenerate(pyprojectContent);
            if (lockResult.isSuccess()) {
                updatedLockFiles.put(sourcePath, lockResult.getLockFileContent());
            } else {
                updated = Markup.warn(updated, new RuntimeException(
                        "uv lock regeneration failed: " + lockResult.getErrorMessage()));
            }
        }

        if (marker != null) {
            PythonResolutionResult newMarker = PythonDependencyParser.createMarker(updated, null);
            if (newMarker != null) {
                updated = updated.withMarkers(updated.getMarkers()
                        .removeByType(PythonResolutionResult.class)
                        .addIfAbsent(newMarker.withId(marker.getId())));
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
        Cursor c = cursor;
        while (c != null) {
            Object val = c.getValue();
            if (val instanceof Toml.Table) {
                Toml.Table table = (Toml.Table) val;
                if (table.getName() != null && "tool.pdm.overrides".equals(table.getName().getName())) {
                    return true;
                }
            }
            c = c.getParent();
        }
        return false;
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
