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
package org.openrewrite.python;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.marker.Markup;
import org.openrewrite.python.internal.PythonDependencyParser;
import org.openrewrite.python.internal.UvLockRegeneration;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlRightPadded;

import java.util.*;

/**
 * Remove a dependency from the {@code [project].dependencies} array in pyproject.toml.
 * When uv is available, the uv.lock file is regenerated to reflect the change.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveDependency extends ScanningRecipe<RemoveDependency.Accumulator> {

    @Option(displayName = "Package name",
            description = "The PyPI package name to remove.",
            example = "requests")
    String packageName;

    @Override
    public String getDisplayName() {
        return "Remove Python dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s`", packageName);
    }

    @Override
    public String getDescription() {
        return "Remove a dependency from the [project].dependencies array in pyproject.toml. " +
                "When uv is available, the uv.lock file is regenerated.";
    }

    static class Accumulator {
        final Set<String> projectsToUpdate = new HashSet<>();
        final Map<String, String> updatedLockFiles = new HashMap<>();
        final Set<String> failedProjects = new HashSet<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
                if (!document.getSourcePath().toString().endsWith("pyproject.toml")) {
                    return document;
                }
                Optional<PythonResolutionResult> resolution = document.getMarkers()
                        .findFirst(PythonResolutionResult.class);
                if (!resolution.isPresent()) {
                    return document;
                }

                PythonResolutionResult marker = resolution.get();

                // Check if the dependency exists
                if (marker.findDependency(packageName) == null) {
                    return document;
                }

                acc.projectsToUpdate.add(document.getSourcePath().toString());
                return document;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
                String sourcePath = document.getSourcePath().toString();

                if (sourcePath.endsWith("pyproject.toml") && acc.projectsToUpdate.contains(sourcePath)) {
                    return removeDependencyFromPyproject(document, ctx, acc);
                }

                if (sourcePath.endsWith("uv.lock")) {
                    String pyprojectPath = correspondingPyprojectPath(sourcePath);
                    String newContent = acc.updatedLockFiles.get(pyprojectPath);
                    if (newContent != null) {
                        return reparseToml(document, newContent);
                    }
                }

                return document;
            }
        };
    }

    private Toml.Document removeDependencyFromPyproject(Toml.Document document, ExecutionContext ctx, Accumulator acc) {
        String normalizedName = PythonResolutionResult.normalizeName(packageName);
        String sourcePath = document.getSourcePath().toString();

        Toml.Document updated = (Toml.Document) new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.Array visitArray(Toml.Array array, ExecutionContext ctx) {
                Toml.Array a = super.visitArray(array, ctx);

                // Check if this array is the value of a "dependencies" key inside [project]
                Cursor parent = getCursor().getParentTreeCursor();
                if (!(parent.getValue() instanceof Toml.KeyValue)) {
                    return a;
                }
                Toml.KeyValue kv = parent.getValue();
                if (!(kv.getKey() instanceof Toml.Identifier) ||
                        !"dependencies".equals(((Toml.Identifier) kv.getKey()).getName())) {
                    return a;
                }

                // Verify we're inside [project] table
                Cursor tableParent = parent.getParentTreeCursor();
                if (!(tableParent.getValue() instanceof Toml.Table)) {
                    return a;
                }
                Toml.Table table = tableParent.getValue();
                if (table.getName() == null || !"project".equals(table.getName().getName())) {
                    return a;
                }

                // Find and remove the matching dependency
                List<TomlRightPadded<Toml>> existingPadded = a.getPadding().getValues();
                List<TomlRightPadded<Toml>> newPadded = new ArrayList<>();
                boolean found = false;
                int removedIdx = -1;

                for (int i = 0; i < existingPadded.size(); i++) {
                    TomlRightPadded<Toml> padded = existingPadded.get(i);
                    Toml element = padded.getElement();

                    if (!found && element instanceof Toml.Literal) {
                        Object val = ((Toml.Literal) element).getValue();
                        if (val instanceof String) {
                            String depName = extractPackageName((String) val);
                            if (PythonResolutionResult.normalizeName(depName).equals(normalizedName)) {
                                found = true;
                                removedIdx = i;
                                continue;
                            }
                        }
                    }

                    newPadded.add(padded);
                }

                if (!found) {
                    return a;
                }

                // If the removed element was the first one, the next element
                // may have a space prefix from comma formatting. Transfer the
                // removed element's prefix to the first remaining real element.
                if (removedIdx == 0 && !newPadded.isEmpty()) {
                    TomlRightPadded<Toml> first = newPadded.get(0);
                    if (!(first.getElement() instanceof Toml.Empty)) {
                        Space originalPrefix = existingPadded.get(removedIdx).getElement().getPrefix();
                        newPadded.set(0, first.map(el -> el.withPrefix(originalPrefix)));
                    }
                }

                return a.getPadding().withValues(newPadded);
            }
        }.visitNonNull(document, ctx);

        if (updated != document) {
            // Regenerate lock file
            String pyprojectContent = updated.printAll();
            UvLockRegeneration.Result lockResult = UvLockRegeneration.regenerate(pyprojectContent);
            if (lockResult.isSuccess()) {
                acc.updatedLockFiles.put(sourcePath, lockResult.getLockFileContent());
            } else {
                acc.failedProjects.add(sourcePath);
                updated = Markup.warn(updated, new RuntimeException(
                        "uv lock regeneration failed: " + lockResult.getErrorMessage()));
            }

            // Update the marker
            PythonResolutionResult marker = updated.getMarkers()
                    .findFirst(PythonResolutionResult.class).orElse(null);
            if (marker != null) {
                PythonResolutionResult newMarker = PythonDependencyParser.createMarker(updated, null);
                if (newMarker != null) {
                    updated = updated.withMarkers(updated.getMarkers()
                            .removeByType(PythonResolutionResult.class)
                            .addIfAbsent(newMarker.withId(marker.getId())));
                }
            }
        }

        return updated;
    }

    private static String extractPackageName(String pep508Spec) {
        String trimmed = pep508Spec.trim();
        int end = 0;
        while (end < trimmed.length()) {
            char c = trimmed.charAt(end);
            if (c == '[' || c == '>' || c == '<' || c == '=' || c == '!' || c == '~' || c == ';' || c == ' ') {
                break;
            }
            end++;
        }
        return trimmed.substring(0, end).trim();
    }

    private static String correspondingPyprojectPath(String uvLockPath) {
        if (uvLockPath.contains("/")) {
            return uvLockPath.substring(0, uvLockPath.lastIndexOf('/') + 1) + "pyproject.toml";
        }
        return "pyproject.toml";
    }

    private static Toml.Document reparseToml(Toml.Document original, String newContent) {
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
}
