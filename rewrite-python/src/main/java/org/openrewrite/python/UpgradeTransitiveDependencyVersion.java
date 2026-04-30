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
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.trait.PythonDependencyFile;
import org.openrewrite.toml.tree.Toml;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Pin a transitive dependency version using the strategy appropriate for the file type
 * and package manager. For {@code pyproject.toml}: uv uses
 * {@code [tool.uv].constraint-dependencies}, PDM uses {@code [tool.pdm.overrides]},
 * and other managers add a direct dependency. For {@code requirements.txt} and
 * {@code Pipfile}: appends the dependency. When the matching package manager
 * (uv or pipenv) is available on {@code PATH}, the corresponding lock file
 * (uv.lock or Pipfile.lock) is regenerated.
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
                "For `requirements.txt` and `Pipfile`: appends the dependency. " +
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
                    ps.depsFileMatches = matchesTransitive(trait);
                }
                return tree;
            }
        };
    }

    private boolean matchesTransitive(PythonDependencyFile trait) {
        PythonResolutionResult marker = trait.getMarker();
        if (marker.findDependency(packageName) != null) {
            return false;
        }
        if (marker.getPackageManager() == PythonResolutionResult.PackageManager.Uv &&
                marker.getResolvedDependency(packageName) == null) {
            return false;
        }
        return true;
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
                String normalizedName = PythonResolutionResult.normalizeName(packageName);
                Map<String, String> pins = Collections.singletonMap(normalizedName, version);
                Function<PythonDependencyFile, PythonDependencyFile> editFn =
                        t -> t.withPinnedTransitiveDependencies(pins, null, null);
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
