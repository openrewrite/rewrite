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
                if (PyProjectHelper.findDependencyInScope(trait.getMarker(), packageName, scope, groupName) == null) {
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
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                stopAfterPreVisit();
                SourceFile sourceFile = (SourceFile) tree;
                String sourcePath = sourceFile.getSourcePath().toString();

                if (acc.projectsToUpdate.contains(sourcePath)) {
                    PythonDependencyFile trait = new PythonDependencyFile.Matcher().get(getCursor()).orElse(null);
                    if (trait != null) {
                        PythonDependencyFile updated = trait.withRemovedDependencies(
                                Collections.singleton(packageName), scope, groupName);
                        SourceFile result = (SourceFile) updated.getTree();
                        if (result != tree) {
                            if (result instanceof Toml.Document) {
                                return PyProjectHelper.regenerateLockAndRefreshMarker((Toml.Document) result, ctx);
                            }
                            return result;
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
