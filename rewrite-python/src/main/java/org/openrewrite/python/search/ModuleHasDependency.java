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
package org.openrewrite.python.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class ModuleHasDependency extends ScanningRecipe<ModuleHasDependency.Accumulator> {

    private static final Path ROOT = Paths.get("");

    String displayName = "Module has dependency";

    String description = "Searches for Python projects that have a dependency matching the specified package name. " +
                         "Places a `SearchResult` marker on all sources within a project with a matching dependency. " +
                         "A source belongs to the dependency manifest (`pyproject.toml`, `Pipfile`, `setup.cfg`, `setup.py` or `requirements.txt`) " +
                         "in its nearest enclosing directory. " +
                         "This recipe is intended to be used as a precondition for other recipes. " +
                         "For example this could be used to limit the application of a migration to only projects " +
                         "that use a specific version of a library. " +
                         "If the search result you want is instead just the dependency entry in the manifest, use the `DependencyInsight` recipe instead.";

    @Option(displayName = "Package name pattern",
            description = "A glob pattern to match Python package names (PEP 503 normalized). Use `*` as a wildcard.",
            example = "django*")
    String packageNamePattern;

    @Option(displayName = "Version",
            description = "Match only dependencies with the specified resolved version. " +
                          "Node-style [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors) may be used. " +
                          "When a dependency is not resolved, only an exact `==` pin in its constraint is considered. " +
                          "All versions are searched by default.",
            example = "4.2.x",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Only direct",
            description = "If enabled, transitive dependencies will not be considered. All dependencies are searched by default.",
            required = false,
            example = "true")
    @Nullable
    Boolean onlyDirect;

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s%s`", packageNamePattern, version == null ? "" : ":" + version);
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> v = super.validate();
        if (version != null) {
            v = v.and(Semver.validate(version, null));
        }
        return v;
    }

    @Value
    public static class Accumulator {
        Map<Path, Boolean> projectsByDirectory;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator(new HashMap<>());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    sourceFile.getMarkers().findFirst(PythonResolutionResult.class).ifPresent(resolution ->
                            acc.getProjectsByDirectory().merge(directoryOf(sourceFile.getSourcePath()),
                                    hasDependency(resolution), Boolean::logicalOr));
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile && belongsToMatchingProject(((SourceFile) tree).getSourcePath(), acc)) {
                    return SearchResult.found(tree, "Module has dependency: " + packageNamePattern + (version == null ? "" : ":" + version));
                }
                return tree;
            }
        };
    }

    private boolean hasDependency(PythonResolutionResult resolution) {
        String normalizedPattern = PythonResolutionResult.normalizeName(packageNamePattern);
        VersionComparator versionComparator = version == null ? null : Semver.validate(version, null).getValue();
        for (Dependency dependency : resolution.getAllDeclaredDependencies()) {
            ResolvedDependency resolved = dependency.getResolved();
            String candidate = resolved != null ? resolved.getVersion() : pinnedVersion(dependency.getVersionConstraint());
            if (matches(dependency.getName(), candidate, normalizedPattern, versionComparator)) {
                return true;
            }
            if (!Boolean.TRUE.equals(onlyDirect) && resolved != null &&
                hasTransitiveMatch(resolved, normalizedPattern, versionComparator, new HashSet<>())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasTransitiveMatch(ResolvedDependency resolved, String normalizedPattern,
                                       @Nullable VersionComparator versionComparator, Set<ResolvedDependency> visited) {
        if (!visited.add(resolved) || resolved.getDependencies() == null) {
            return false;
        }
        for (ResolvedDependency dependency : resolved.getDependencies()) {
            if (matches(dependency.getName(), dependency.getVersion(), normalizedPattern, versionComparator) ||
                hasTransitiveMatch(dependency, normalizedPattern, versionComparator, visited)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String name, @Nullable String candidateVersion, String normalizedPattern,
                                   @Nullable VersionComparator versionComparator) {
        if (!StringUtils.matchesGlob(PythonResolutionResult.normalizeName(name), normalizedPattern)) {
            return false;
        }
        return versionComparator == null || (candidateVersion != null && versionComparator.isValid(null, candidateVersion));
    }

    private static @Nullable String pinnedVersion(@Nullable String versionConstraint) {
        if (versionConstraint == null) {
            return null;
        }
        String constraint = versionConstraint.trim();
        if (!constraint.startsWith("==") || constraint.startsWith("===") || constraint.indexOf(',') >= 0) {
            return null;
        }
        String pinned = constraint.substring(2).trim();
        return pinned.isEmpty() || pinned.contains("*") ? null : pinned;
    }

    private static boolean belongsToMatchingProject(Path sourcePath, Accumulator acc) {
        if (acc.getProjectsByDirectory().isEmpty()) {
            return false;
        }
        Path directory = directoryOf(sourcePath);
        while (true) {
            Boolean matches = acc.getProjectsByDirectory().get(directory);
            if (matches != null) {
                return matches;
            }
            if (directory.equals(ROOT)) {
                return false;
            }
            directory = directoryOf(directory);
        }
    }

    private static Path directoryOf(Path path) {
        Path parent = path.getParent();
        return parent == null ? ROOT : parent;
    }
}
