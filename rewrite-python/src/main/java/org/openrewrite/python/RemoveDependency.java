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
import org.openrewrite.python.internal.PyProjectHelper;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.toml.TomlIsoVisitor;
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

    @Option(displayName = "Scope",
            description = "The dependency scope to remove from. Defaults to `project.dependencies`.",
            valid = {"project.dependencies", "project.optional-dependencies", "dependency-groups"},
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
        return "Remove Python dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s`", packageName);
    }

    @Override
    public String getDescription() {
        return "Remove a dependency from the `[project].dependencies` array in `pyproject.toml`. " +
                "When `uv` is available, the `uv.lock` file is regenerated.";
    }

    static class Accumulator {
        final Set<String> projectsToUpdate = new HashSet<>();
        final Map<String, String> updatedLockFiles = new HashMap<>();
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

                // Check if the dependency exists in the target scope
                if (PyProjectHelper.findDependencyInScope(marker, packageName, scope, groupName) == null) {
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
                    String pyprojectPath = PyProjectHelper.correspondingPyprojectPath(sourcePath);
                    String newContent = acc.updatedLockFiles.get(pyprojectPath);
                    if (newContent != null) {
                        return PyProjectHelper.reparseToml(document, newContent);
                    }
                }

                return document;
            }
        };
    }

    private Toml.Document removeDependencyFromPyproject(Toml.Document document, ExecutionContext ctx, Accumulator acc) {
        String normalizedName = PythonResolutionResult.normalizeName(packageName);

        Toml.Document updated = (Toml.Document) new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.Array visitArray(Toml.Array array, ExecutionContext ctx) {
                Toml.Array a = super.visitArray(array, ctx);

                if (!PyProjectHelper.isInsideDependencyArray(getCursor(), scope, groupName)) {
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
                            String depName = PyProjectHelper.extractPackageName((String) val);
                            if (depName != null && PythonResolutionResult.normalizeName(depName).equals(normalizedName)) {
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
            updated = PyProjectHelper.regenerateLockAndRefreshMarker(updated, acc.updatedLockFiles);
        }

        return updated;
    }

}
