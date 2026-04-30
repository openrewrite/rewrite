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
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markup;
import org.openrewrite.python.internal.LockFileRegeneration;
import org.openrewrite.python.internal.PyProjectHelper;
import org.openrewrite.python.trait.PythonDependencyFile;
import org.openrewrite.toml.tree.Toml;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Remove a dependency from a Python project. Supports {@code pyproject.toml}
 * (with scope and group targeting), {@code requirements.txt}, and {@code Pipfile}.
 * When the matching package manager is available on {@code PATH}, the lock file
 * (uv.lock for pyproject, Pipfile.lock for Pipfile) is regenerated to reflect the change.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveDependency extends ScanningRecipe<RemoveDependency.Accumulator> {

    @Option(displayName = "Package name",
            description = "The PyPI package name to remove.",
            example = "requests")
    String packageName;

    @Option(displayName = "Scope",
            description = "The dependency scope to remove from. All scopes are searched by default.",
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
        return "Remove a dependency from a Python project. Supports `pyproject.toml` " +
                "(with scope/group targeting), `requirements.txt`, and `Pipfile`. " +
                "When the matching package manager (`uv` or `pipenv`) is available, " +
                "the corresponding lock file (`uv.lock` or `Pipfile.lock`) is regenerated. " +
                "Not safe to use as a precondition: invokes the package manager and " +
                "publishes per-project state shared with other dependency recipes.";
    }

    static class Accumulator {
        final Map<Path, ProjectState> projects = new HashMap<>();
        final Map<Path, Path> lockToDeps = new HashMap<>();
    }

    static class ProjectState {
        @Nullable SourceFile capturedDepsFile;
        @Nullable String capturedLockContent;
        boolean depsFileMatches;
        @Nullable SourceFile modifiedDepsFile;
        LockFileRegeneration.@Nullable Result regenResult;
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
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                Path sourcePath = sourceFile.getSourcePath();

                if (tree instanceof Toml.Document && sourcePath.endsWith("uv.lock")) {
                    Path depsPath = PyProjectHelper.correspondingPyprojectPath(sourcePath);
                    ProjectState ps = acc.projects.computeIfAbsent(depsPath, k -> new ProjectState());
                    ps.capturedLockContent = ((Toml.Document) tree).printAll();
                    acc.lockToDeps.put(sourcePath, depsPath);
                    return tree;
                }
                if (tree instanceof Json.Document && sourcePath.endsWith("Pipfile.lock")) {
                    Path depsPath = PyProjectHelper.correspondingPipfilePath(sourcePath);
                    ProjectState ps = acc.projects.computeIfAbsent(depsPath, k -> new ProjectState());
                    ps.capturedLockContent = ((Json.Document) tree).printAll();
                    acc.lockToDeps.put(sourcePath, depsPath);
                    return tree;
                }

                PythonDependencyFile trait = matcher.get(getCursor()).orElse(null);
                if (trait != null) {
                    ProjectState ps = acc.projects.computeIfAbsent(sourcePath, k -> new ProjectState());
                    ps.capturedDepsFile = sourceFile;
                    ps.depsFileMatches = matchesRemoveDependency(trait);
                }
                return tree;
            }
        };
    }

    private boolean matchesRemoveDependency(PythonDependencyFile trait) {
        return PyProjectHelper.findDependencyInScope(
                trait.getMarker(), packageName, scope, groupName) != null;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if (acc.projects.values().stream().noneMatch(ps -> ps.depsFileMatches)) {
            return TreeVisitor.noop();
        }
        return new TreeVisitor<Tree, ExecutionContext>() {
            final PythonDependencyFile.Matcher matcher = new PythonDependencyFile.Matcher();

            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                Path sourcePath = sourceFile.getSourcePath();

                ProjectState ps = acc.projects.get(sourcePath);
                if (ps != null && ps.depsFileMatches) {
                    PythonDependencyFile trait = matcher.get(getCursor()).orElse(null);
                    if (trait != null) {
                        ensureComputed(ps, trait);
                    }
                    if (ps.modifiedDepsFile != null) {
                        SourceFile out = ps.modifiedDepsFile;
                        if (ps.regenResult != null && !ps.regenResult.isSuccess()) {
                            out = Markup.warn(out, new RuntimeException(
                                    "lock regeneration failed: " + ps.regenResult.getErrorMessage()));
                        }
                        PyProjectHelper.putLiveDepsTree(ctx, sourcePath, out);
                        return out;
                    }
                }

                Path depsPath = acc.lockToDeps.get(sourcePath);
                if (depsPath == null) {
                    return tree;
                }
                ProjectState lockPs = acc.projects.get(depsPath);
                if (lockPs == null) {
                    return tree;
                }
                if (lockPs.depsFileMatches && lockPs.modifiedDepsFile == null) {
                    SourceFile depsTree = PyProjectHelper.getLiveDepsTree(ctx, depsPath);
                    if (depsTree == null) {
                        depsTree = lockPs.capturedDepsFile;
                    }
                    if (depsTree != null) {
                        Cursor synth = new Cursor(new Cursor(null, Cursor.ROOT_VALUE), depsTree);
                        PythonDependencyFile trait = matcher.get(synth).orElse(null);
                        if (trait != null) {
                            ensureComputed(lockPs, trait);
                            if (lockPs.modifiedDepsFile != null) {
                                PyProjectHelper.putLiveDepsTree(ctx, depsPath, lockPs.modifiedDepsFile);
                            }
                        }
                    }
                }
                if (lockPs.regenResult != null && lockPs.regenResult.isSuccess()) {
                    String lockContent = lockPs.regenResult.getLockFileContent();
                    if (tree instanceof Toml.Document) {
                        return PyProjectHelper.reparseToml((Toml.Document) tree, lockContent);
                    }
                    if (tree instanceof Json.Document) {
                        return PyProjectHelper.reparseJson((Json.Document) tree, lockContent);
                    }
                }
                return tree;
            }

            private void ensureComputed(ProjectState ps, PythonDependencyFile trait) {
                if (ps.modifiedDepsFile != null) {
                    return;
                }
                Set<String> removals = Collections.singleton(packageName);
                Function<PythonDependencyFile, PythonDependencyFile> editFn =
                        t -> t.withRemovedDependencies(removals, scope, groupName);
                PyProjectHelper.EditAndRegenerateResult r =
                        PyProjectHelper.editAndRegenerate(trait, editFn, ps.capturedLockContent);
                if (r.isChanged()) {
                    ps.modifiedDepsFile = r.getModifiedDepsFile();
                    ps.regenResult = r.getRegenResult();
                }
            }
        };
    }

}
