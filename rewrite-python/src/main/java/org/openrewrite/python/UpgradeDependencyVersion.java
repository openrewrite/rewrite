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
import org.openrewrite.python.internal.PythonDependencyExecutionContextView;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlType;

import java.util.*;

/**
 * Upgrade the version constraint for a dependency in {@code [project].dependencies} in pyproject.toml.
 * When uv is available, the uv.lock file is regenerated to reflect the change.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeDependencyVersion extends ScanningRecipe<UpgradeDependencyVersion.Accumulator> {

    @Option(displayName = "Package name",
            description = "The PyPI package name to update.",
            example = "requests")
    String packageName;

    @Option(displayName = "New version",
            description = "The new PEP 508 version constraint (e.g., `>=2.31.0`).",
            example = ">=2.31.0")
    String newVersion;

    @Option(displayName = "Scope",
            description = "The dependency scope to update in. Defaults to `project.dependencies`.",
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
        return "Upgrade Python dependency version";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", packageName, newVersion);
    }

    @Override
    public String getDescription() {
        return "Upgrade the version constraint for a dependency in `[project].dependencies` in `pyproject.toml`. " +
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

                // Check if the dependency exists in the target scope and has a different version
                PythonResolutionResult.Dependency dep = PyProjectHelper.findDependencyInScope(
                        marker, packageName, scope, groupName);
                if (dep == null) {
                    return document;
                }

                // Skip if the version constraint already matches
                if (PyProjectHelper.normalizeVersionConstraint(newVersion).equals(dep.getVersionConstraint())) {
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
                    return changeVersionInPyproject(document, ctx, acc);
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

    private Toml.Document changeVersionInPyproject(Toml.Document document, ExecutionContext ctx, Accumulator acc) {
        String normalizedName = PythonResolutionResult.normalizeName(packageName);

        Toml.Document updated = (Toml.Document) new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.Literal visitLiteral(Toml.Literal literal, ExecutionContext ctx) {
                Toml.Literal l = super.visitLiteral(literal, ctx);
                if (l.getType() != TomlType.Primitive.String) {
                    return l;
                }

                Object val = l.getValue();
                if (!(val instanceof String)) {
                    return l;
                }

                // Check if we're inside the target dependency array
                if (!isInsideTargetDependencies()) {
                    return l;
                }

                // Check if this literal matches the package we're looking for
                String spec = (String) val;
                String depName = PyProjectHelper.extractPackageName(spec);
                if (depName == null || !PythonResolutionResult.normalizeName(depName).equals(normalizedName)) {
                    return l;
                }

                // Build new PEP 508 string preserving extras and markers
                String newSpec = buildNewSpec(spec, depName);
                return l.withSource("\"" + newSpec + "\"").withValue(newSpec);
            }

            private boolean isInsideTargetDependencies() {
                // Walk up the cursor to find the enclosing array, then check scope
                Cursor c = getCursor();
                while (c != null) {
                    if (c.getValue() instanceof Toml.Array) {
                        return PyProjectHelper.isInsideDependencyArray(c, scope, groupName);
                    }
                    c = c.getParent();
                }
                return false;
            }

            private String buildNewSpec(String oldSpec, String depName) {
                // Parse extras and markers from old spec
                String extras = extractExtras(oldSpec);
                String marker = extractMarker(oldSpec);

                StringBuilder sb = new StringBuilder(depName);
                if (extras != null) {
                    sb.append('[').append(extras).append(']');
                }
                sb.append(PyProjectHelper.normalizeVersionConstraint(newVersion));
                if (marker != null) {
                    sb.append("; ").append(marker);
                }
                return sb.toString();
            }
        }.visitNonNull(document, ctx);

        if (updated != document) {
            updated = PyProjectHelper.regenerateLockAndRefreshMarker(updated, ctx);
        }

        return updated;
    }

    static @Nullable String extractExtras(String pep508Spec) {
        int start = pep508Spec.indexOf('[');
        int end = pep508Spec.indexOf(']');
        if (start >= 0 && end > start) {
            return pep508Spec.substring(start + 1, end);
        }
        return null;
    }

    static @Nullable String extractMarker(String pep508Spec) {
        int idx = pep508Spec.indexOf(';');
        if (idx >= 0) {
            String marker = pep508Spec.substring(idx + 1).trim();
            return marker.isEmpty() ? null : marker;
        }
        return null;
    }

}
