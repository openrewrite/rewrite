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
import org.openrewrite.python.internal.PyProjectHelper;
import org.openrewrite.python.internal.PythonDependencyExecutionContextView;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlRightPadded;
import org.openrewrite.toml.tree.TomlType;

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

    @Option(displayName = "Scope",
            description = "The dependency scope to add to. Defaults to `project.dependencies`.",
            valid = {"project.dependencies", "project.optional-dependencies", "dependency-groups",
                    "tool.uv.constraint-dependencies", "tool.uv.override-dependencies"},
            example = "project.dependencies",
            required = false)
    @Nullable
    String scope;

    @Option(displayName = "Group name",
            description = "The group name, required when scope is `project.optional-dependencies` or `dependency-groups`.",
            example = "dev",
            required = false)
    @Nullable
    String groupName;

    @Override
    public Validated<Object> validate() {
        Validated<Object> v = super.validate();
        if ("project.optional-dependencies".equals(scope) || "dependency-groups".equals(scope)) {
            v = v.and(Validated.required("groupName", groupName));
        }
        return v;
    }

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
        return "Add a dependency to the `[project].dependencies` array in `pyproject.toml`. " +
                "When `uv` is available, the `uv.lock` file is regenerated.";
    }

    static class Accumulator {
        final Set<String> projectsToUpdate = new HashSet<>();
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
                String sourcePath = document.getSourcePath().toString();

                if (sourcePath.endsWith("uv.lock")) {
                    PythonDependencyExecutionContextView.view(ctx).getExistingLockContents().put(
                            PyProjectHelper.correspondingPyprojectPath(sourcePath),
                            document.printAll());
                    return document;
                }

                if (!sourcePath.endsWith("pyproject.toml")) {
                    return document;
                }
                Optional<PythonResolutionResult> resolution = document.getMarkers()
                        .findFirst(PythonResolutionResult.class);
                if (!resolution.isPresent()) {
                    return document;
                }

                PythonResolutionResult marker = resolution.get();

                // Check if the dependency already exists in the target scope
                if (PyProjectHelper.findDependencyInScope(marker, packageName, scope, groupName) != null) {
                    return document;
                }

                acc.projectsToUpdate.add(sourcePath);
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
                    Toml.Document updatedLock = PyProjectHelper.maybeUpdateUvLock(document, ctx);
                    if (updatedLock != null) {
                        return updatedLock;
                    }
                }

                return document;
            }
        };
    }

    private Toml.Document addDependencyToPyproject(Toml.Document document, ExecutionContext ctx, Accumulator acc) {
        String pep508 = version != null ? packageName + PyProjectHelper.normalizeVersionConstraint(version) : packageName;

        Toml.Document updated = (Toml.Document) new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.Array visitArray(Toml.Array array, ExecutionContext ctx) {
                Toml.Array a = super.visitArray(array, ctx);

                if (!PyProjectHelper.isInsideDependencyArray(getCursor(), scope, groupName)) {
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
            updated = PyProjectHelper.regenerateLockAndRefreshMarker(updated, ctx);
        }

        return updated;
    }

}
