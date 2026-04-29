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
import org.openrewrite.python.internal.PyProjectHelper;
import org.openrewrite.python.trait.PythonDependencyFile;
import org.openrewrite.toml.tree.Toml;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Add a dependency to a Python project. Supports {@code pyproject.toml}
 * (with scope and group targeting), {@code requirements.txt}, and {@code Pipfile}.
 * When the matching package manager is available on {@code PATH}, the lock file
 * (uv.lock for pyproject, Pipfile.lock for Pipfile) is regenerated to reflect the change.
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
            description = "The dependency scope to add to. For pyproject.toml this targets a specific TOML section. " +
                    "For requirements files, `null` matches all files, empty string matches only `requirements.txt`, " +
                    "and a value like `dev` matches `requirements-dev.txt`. Defaults to `project.dependencies`.",
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
        return "Add a dependency to a Python project. Supports `pyproject.toml` " +
                "(with scope/group targeting), `requirements.txt`, and `Pipfile`. " +
                "When the matching package manager (`uv` or `pipenv`) is available, " +
                "the corresponding lock file (`uv.lock` or `Pipfile.lock`) is regenerated.";
    }

    static class Accumulator {
        final Map<Path, ProjectState> projects = new HashMap<>();
        final Map<Path, Path> lockToDeps = new HashMap<>();
    }

    static class ProjectState {
        final Path depsFilePath;
        @Nullable Path lockFilePath;
        @Nullable String capturedLockContent;
        boolean depsFileMatches;
        @Nullable String regeneratedLockContent;
        @Nullable String regenerationError;

        ProjectState(Path depsFilePath) {
            this.depsFilePath = depsFilePath;
        }
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
                    ProjectState ps = acc.projects.computeIfAbsent(depsPath, ProjectState::new);
                    ps.lockFilePath = sourcePath;
                    ps.capturedLockContent = ((Toml.Document) tree).printAll();
                    acc.lockToDeps.put(sourcePath, depsPath);
                    return tree;
                }
                if (tree instanceof Json.Document && sourcePath.endsWith("Pipfile.lock")) {
                    Path depsPath = PyProjectHelper.correspondingPipfilePath(sourcePath);
                    ProjectState ps = acc.projects.computeIfAbsent(depsPath, ProjectState::new);
                    ps.lockFilePath = sourcePath;
                    ps.capturedLockContent = ((Json.Document) tree).printAll();
                    acc.lockToDeps.put(sourcePath, depsPath);
                    return tree;
                }

                PythonDependencyFile trait = matcher.get(getCursor()).orElse(null);
                if (trait != null) {
                    ProjectState ps = acc.projects.computeIfAbsent(sourcePath, ProjectState::new);
                    ps.depsFileMatches = matchesAddDependency(trait);
                }
                return tree;
            }
        };
    }

    private boolean matchesAddDependency(PythonDependencyFile trait) {
        return PyProjectHelper.findDependencyInScope(
                trait.getMarker(), packageName, scope, groupName) == null;
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        return Collections.emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if (acc.projects.isEmpty()) {
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
                        String ver = version != null ? version : "";
                        Map<String, String> additions = Collections.singletonMap(packageName, ver);
                        PythonDependencyFile updated = trait.withAddedDependencies(additions, scope, groupName);
                        if (updated.getTree() != tree) {
                            SourceFile modified = PyProjectHelper.refreshMarker((SourceFile) updated.getTree());
                            if (ps.capturedLockContent != null) {
                                PyProjectHelper.RegenerationResult r =
                                        PyProjectHelper.regenerateLockContent(modified, ps.capturedLockContent);
                                if (r != null) {
                                    if (r.isSuccess()) {
                                        ps.regeneratedLockContent = r.getLockContent();
                                    } else {
                                        ps.regenerationError = r.getErrorMessage();
                                    }
                                }
                            }
                            if (ps.regenerationError != null) {
                                modified = Markup.warn(modified, new RuntimeException(
                                        "lock regeneration failed: " + ps.regenerationError));
                            }
                            return modified;
                        }
                    }
                }

                Path depsPath = acc.lockToDeps.get(sourcePath);
                if (depsPath != null) {
                    ProjectState lockPs = acc.projects.get(depsPath);
                    if (lockPs != null && lockPs.regeneratedLockContent != null) {
                        if (tree instanceof Toml.Document) {
                            return PyProjectHelper.reparseToml(
                                    (Toml.Document) tree, lockPs.regeneratedLockContent);
                        }
                        if (tree instanceof Json.Document) {
                            return PyProjectHelper.reparseJson(
                                    (Json.Document) tree, lockPs.regeneratedLockContent);
                        }
                    }
                }
                return tree;
            }
        };
    }

}
