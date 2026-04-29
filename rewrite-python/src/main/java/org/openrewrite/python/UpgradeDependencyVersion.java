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
import org.openrewrite.python.trait.PythonDependencyFile;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Upgrade the version constraint for a dependency. Supports {@code pyproject.toml}
 * (with scope and group targeting), {@code requirements.txt}, and {@code Pipfile}.
 * When the matching package manager is available on {@code PATH}, the lock file
 * (uv.lock for pyproject, Pipfile.lock for Pipfile) is regenerated to reflect the change.
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
            description = "The dependency scope to update in. All scopes are searched by default.",
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
        return "Upgrade the version constraint for a dependency. Supports `pyproject.toml` " +
                "(with scope/group targeting), `requirements.txt`, and `Pipfile`. " +
                "When the matching package manager (`uv` or `pipenv`) is available, " +
                "the corresponding lock file (`uv.lock` or `Pipfile.lock`) is regenerated.";
    }

    static class Accumulator {
        final Set<Path> projectsToUpdate = new HashSet<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            final PythonDependencyFile.Matcher matcher = new PythonDependencyFile.Matcher();

            @Override
            public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                if (PyProjectHelper.captureExistingLockContent(sourceFile, tree, ctx)) {
                    return tree;
                }
                PythonDependencyFile trait = matcher.get(getCursor()).orElse(null);
                if (trait == null) {
                    return tree;
                }
                PythonResolutionResult.Dependency dep = PyProjectHelper.findDependencyInScope(
                        trait.getMarker(), packageName, scope, groupName);
                if (dep != null && !PyProjectHelper.normalizeVersionConstraint(newVersion).equals(dep.getVersionConstraint())) {
                    acc.projectsToUpdate.add(sourceFile.getSourcePath());
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if (acc.projectsToUpdate.isEmpty()) {
            return TreeVisitor.noop();
        }
        return new TreeVisitor<Tree, ExecutionContext>() {
            final PythonDependencyFile.Matcher matcher = new PythonDependencyFile.Matcher();

            @Override
            public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                Path sourcePath = sourceFile.getSourcePath();

                if (acc.projectsToUpdate.contains(sourcePath)) {
                    PythonDependencyFile trait = matcher.get(getCursor()).orElse(null);
                    if (trait != null) {
                        Map<String, String> upgrades = Collections.singletonMap(
                                PythonResolutionResult.normalizeName(packageName), newVersion);
                        PythonDependencyFile updated = trait.withUpgradedVersions(upgrades, scope, groupName);
                        if (updated.getTree() != tree) {
                            return updated.afterModification(ctx);
                        }
                    }
                }

                Tree updatedLock = PyProjectHelper.maybeReplayLockContent(tree, ctx);
                if (updatedLock != null) {
                    return updatedLock;
                }

                return tree;
            }
        };
    }
}
