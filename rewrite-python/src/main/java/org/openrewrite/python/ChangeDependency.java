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
import org.openrewrite.python.trait.PythonDependencyFile;
import org.openrewrite.toml.tree.Toml;

import java.util.HashSet;
import java.util.Set;

/**
 * Change a dependency to a different package. Supports {@code pyproject.toml},
 * {@code requirements.txt}, and {@code Pipfile}. Searches all dependency scopes.
 * When uv is available, the uv.lock file is regenerated to reflect the change.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class ChangeDependency extends ScanningRecipe<ChangeDependency.Accumulator> {

    @Option(displayName = "Old package name",
            description = "The current PyPI package name to replace.",
            example = "requests")
    String oldPackageName;

    @Option(displayName = "New package name",
            description = "The new PyPI package name.",
            example = "httpx")
    String newPackageName;

    @Option(displayName = "New version",
            description = "Optional new PEP 508 version constraint. If not specified, the original version constraint is preserved.",
            example = ">=0.24.0",
            required = false)
    @Nullable
    String newVersion;

    @Override
    public String getDisplayName() {
        return "Change Python dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", oldPackageName, newPackageName);
    }

    @Override
    public String getDescription() {
        return "Change a dependency to a different package. Supports `pyproject.toml`, " +
                "`requirements.txt`, and `Pipfile`. Searches all dependency scopes. " +
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
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                stopAfterPreVisit();
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
                if (trait.getMarker().findDependencyInAnyScope(oldPackageName) != null) {
                    acc.projectsToUpdate.add(sourceFile.getSourcePath().toString());
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                stopAfterPreVisit();
                SourceFile sourceFile = (SourceFile) tree;
                String sourcePath = sourceFile.getSourcePath().toString();

                if (acc.projectsToUpdate.contains(sourcePath)) {
                    PythonDependencyFile trait = new PythonDependencyFile.Matcher().get(getCursor()).orElse(null);
                    if (trait != null) {
                        PythonDependencyFile updated = trait.withChangedDependency(oldPackageName, newPackageName, newVersion);
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
