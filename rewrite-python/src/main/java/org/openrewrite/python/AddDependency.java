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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.Markup;
import org.openrewrite.python.internal.PythonDependencyParser;
import org.openrewrite.python.internal.UvLockRegeneration;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlRightPadded;
import org.openrewrite.toml.tree.TomlType;
import org.openrewrite.toml.tree.Space;

import java.util.*;

import static org.openrewrite.Tree.randomId;

/**
 * Add a dependency to the {@code [project].dependencies} array in pyproject.toml.
 * When uv is available, the uv.lock file is regenerated to reflect the change.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class AddDependency extends ScanningRecipe<AddDependency.Accumulator> {

    @Option(displayName = "Package name",
            description = "The PyPI package name to add.",
            example = "requests")
    String packageName;

    @Option(displayName = "Version",
            description = "The PEP 508 version constraint (e.g., `>=2.28.0`).",
            example = ">=2.28.0",
            required = false)
    @Nullable
    String version;

    @Override
    public String getDisplayName() {
        return "Add Python dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s`", packageName);
    }

    @Override
    public String getDescription() {
        return "Add a dependency to the [project].dependencies array in pyproject.toml. " +
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

                // Check if the dependency already exists
                if (marker.findDependency(packageName) != null) {
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
                    return addDependencyToPyproject(document, ctx, acc);
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

    private Toml.Document addDependencyToPyproject(Toml.Document document, ExecutionContext ctx, Accumulator acc) {
        String pep508 = version != null ? packageName + version : packageName;
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

                Toml.Literal newLiteral = new Toml.Literal(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        TomlType.Primitive.String,
                        "\"" + pep508 + "\"",
                        pep508
                );

                List<TomlRightPadded<Toml>> existingPadded = a.getPadding().getValues();
                List<TomlRightPadded<Toml>> newPadded = new ArrayList<>();

                // An empty TOML array [] is represented as a single Toml.Empty element
                boolean isEmpty = existingPadded.size() == 1 &&
                        existingPadded.get(0).getElement() instanceof Toml.Empty;
                if (existingPadded.isEmpty() || isEmpty) {
                    newPadded.add(new TomlRightPadded<>(newLiteral, Space.EMPTY, Markers.EMPTY));
                } else {
                    // Check if the last element is Toml.Empty (trailing comma marker)
                    TomlRightPadded<Toml> lastPadded = existingPadded.get(existingPadded.size() - 1);
                    boolean hasTrailingComma = lastPadded.getElement() instanceof Toml.Empty;

                    if (hasTrailingComma) {
                        // Insert before the Empty element. The Empty's position
                        // stores the whitespace before ']'.
                        // Find the last real element to copy its prefix formatting
                        int lastRealIdx = existingPadded.size() - 2;
                        Toml lastRealElement = existingPadded.get(lastRealIdx).getElement();
                        Toml.Literal formattedLiteral = newLiteral.withPrefix(lastRealElement.getPrefix());

                        // Copy all existing elements up to (not including) the Empty
                        for (int i = 0; i <= lastRealIdx; i++) {
                            newPadded.add(existingPadded.get(i));
                        }
                        // Add new literal with empty after (comma added by printer)
                        newPadded.add(new TomlRightPadded<>(formattedLiteral, Space.EMPTY, Markers.EMPTY));
                        // Keep the Empty element for trailing comma + closing bracket whitespace
                        newPadded.add(lastPadded);
                    } else {
                        // No trailing comma — the last real element's after has the space before ']'
                        Toml lastElement = lastPadded.getElement();
                        // For multi-line arrays, use same prefix; for inline, use single space
                        Space newPrefix = lastElement.getPrefix().getWhitespace().contains("\n")
                                ? lastElement.getPrefix()
                                : Space.SINGLE_SPACE;
                        Toml.Literal formattedLiteral = newLiteral.withPrefix(newPrefix);

                        // Copy all existing elements but set last one's after to empty
                        for (int i = 0; i < existingPadded.size() - 1; i++) {
                            newPadded.add(existingPadded.get(i));
                        }
                        newPadded.add(lastPadded.withAfter(Space.EMPTY));
                        // New element gets the after from the old last element
                        newPadded.add(new TomlRightPadded<>(formattedLiteral, lastPadded.getAfter(), Markers.EMPTY));
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
