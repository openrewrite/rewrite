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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Change a dependency to a different package. Supports {@code pyproject.toml},
 * {@code requirements.txt}, and {@code Pipfile}. Searches all dependency scopes.
 * When the matching package manager is available on {@code PATH}, the lock file
 * (uv.lock for pyproject, Pipfile.lock for Pipfile) is regenerated to reflect the change.
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
                    ps.depsFileMatches = matchesChangeDependency(trait);
                }
                return tree;
            }
        };
    }

    private boolean matchesChangeDependency(PythonDependencyFile trait) {
        return trait.getMarker().findDependencyInAnyScope(oldPackageName) != null;
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
                Function<PythonDependencyFile, PythonDependencyFile> editFn =
                        t -> t.withChangedDependency(oldPackageName, newPackageName, newVersion, null, null);
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
