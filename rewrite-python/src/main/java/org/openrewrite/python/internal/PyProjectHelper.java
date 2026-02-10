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
        Cursor parent = cursor.getParentTreeCursor();
        if (!(parent.getValue() instanceof Toml.KeyValue)) {
            return false;
        }
        Toml.KeyValue kv = parent.getValue();
        if (!(kv.getKey() instanceof Toml.Identifier) ||
                !"dependencies".equals(((Toml.Identifier) kv.getKey()).getName())) {
            return false;
        }

        Cursor tableParent = parent.getParentTreeCursor();
        if (!(tableParent.getValue() instanceof Toml.Table)) {
            return false;
        }
        Toml.Table table = tableParent.getValue();
        return table.getName() != null && "project".equals(table.getName().getName());
    }
}
