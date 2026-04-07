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
import org.openrewrite.python.trait.PythonDependencyFile;
import org.openrewrite.toml.tree.Toml;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Pin a transitive dependency version using the strategy appropriate for the file type
 * and package manager. For {@code pyproject.toml}: uv uses
 * {@code [tool.uv].constraint-dependencies}, PDM uses {@code [tool.pdm.overrides]},
 * and other managers add a direct dependency. For {@code requirements.txt} and
 * {@code Pipfile}: appends the dependency. When uv is available, the uv.lock file
 * is regenerated.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeTransitiveDependencyVersion extends ScanningRecipe<UpgradeTransitiveDependencyVersion.Accumulator> {

    @Option(displayName = "Package name",
            description = "The PyPI package name of the transitive dependency to pin.",
            example = "certifi")
    String packageName;

    @Option(displayName = "Version",
            description = "The PEP 508 version constraint (e.g., `>=2023.7.22`).",
            example = ">=2023.7.22")
    String version;

    @Override
    public String getDisplayName() {
        return "Upgrade transitive Python dependency version";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", packageName, version);
    }

    @Override
    public String getDescription() {
        return "Pin a transitive dependency version using the strategy appropriate for the file type " +
                "and package manager. For `pyproject.toml`: uv uses `[tool.uv].constraint-dependencies`, " +
                "PDM uses `[tool.pdm.overrides]`, and other managers add a direct dependency. " +
                "For `requirements.txt` and `Pipfile`: appends the dependency.";
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
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                if (tree instanceof Toml.Document && sourceFile.getSourcePath().toString().endsWith("uv.lock")) {
                    PythonDependencyExecutionContextView.view(ctx).getExistingLockContents().put(
                            PyProjectHelper.correspondingPyprojectPath(sourceFile.getSourcePath().toString()),
                            ((Toml.Document) tree).printAll());
                    return tree;
                }
                PythonDependencyFile trait = new PythonDependencyFile.Matcher().get(getCursor()).orElse(null);
                if (trait == null) {
                    return tree;
                }
                PythonResolutionResult marker = trait.getMarker();

                // Skip if this is a direct dependency
                if (marker.findDependency(packageName) != null) {
                    return tree;
                }

                // For uv: skip if not in the resolved dependency tree
                if (marker.getPackageManager() == PythonResolutionResult.PackageManager.Uv &&
                        marker.getResolvedDependency(packageName) == null) {
                    return tree;
                }

                acc.projectsToUpdate.add(sourceFile.getSourcePath().toString());
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                String sourcePath = sourceFile.getSourcePath().toString();

                if (acc.projectsToUpdate.contains(sourcePath)) {
                    PythonDependencyFile trait = new PythonDependencyFile.Matcher().get(getCursor()).orElse(null);
                    if (trait != null) {
                        String normalizedName = PythonResolutionResult.normalizeName(packageName);
                        Map<String, String> pins = Collections.singletonMap(normalizedName, version);
                        PythonDependencyFile updated = trait.withPinnedTransitiveDependencies(pins);
                        if (updated.getTree() != tree) {
                            return updated.afterModification(ctx);
                        }
                    }
                }

                if (tree instanceof Toml.Document && sourcePath.endsWith("uv.lock")) {
                    Toml.Document updatedLock = PyProjectHelper.maybeUpdateUvLock((Toml.Document) tree, ctx);
                    if (updatedLock != null) {
                        return updatedLock;
                    }
                }

                return tree;
            }
        };
    }
}
