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
package org.openrewrite.javascript.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.javascript.marker.NodeResolutionResult.ResolvedDependency;
import org.openrewrite.javascript.semver.NodeSemver;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

@Value
@EqualsAndHashCode(callSuper = false)
public class ModuleHasDependency extends ScanningRecipe<ModuleHasDependency.Accumulator> {

    private static final Path ROOT = Paths.get("");

    private static final List<String> SCOPES = asList("dependencies", "devDependencies", "peerDependencies",
            "optionalDependencies", "bundledDependencies");

    String displayName = "Module has dependency";

    String description = "Searches for Node.js packages that have an npm dependency matching the specified package name. " +
                         "Places a `SearchResult` marker on all sources within a package with a matching dependency. " +
                         "A source belongs to the `package.json` in its nearest enclosing directory, so each member of a workspace is its own module. " +
                         "This recipe is intended to be used as a precondition for other recipes. " +
                         "For example this could be used to limit the application of a migration to only packages " +
                         "that use a specific version of a library. " +
                         "If the search result you want is instead just the dependency entry in `package.json`, use the `DependencyInsight` recipe instead.";

    @Option(displayName = "Package name pattern",
            description = "A glob pattern to match npm package names. Use `*` as a wildcard.",
            example = "@angular/*")
    String packageNamePattern;

    @Option(displayName = "Scope",
            description = "Match dependencies declared in the specified scope. All scopes are searched by default.",
            valid = {"dependencies", "devDependencies", "peerDependencies", "optionalDependencies", "bundledDependencies"},
            example = "dependencies",
            required = false)
    @Nullable
    String scope;

    @Option(displayName = "Version",
            description = "Match only dependencies whose installed version satisfies this npm semver range. " +
                          "When a dependency is not resolved from a lock file, the lowest version its constraint admits is used. " +
                          "All versions are searched by default.",
            example = "^17.0.0",
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
        return String.format("`%s%s`", packageNamePattern, version == null ? "" : "@" + version);
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> v = super.validate();
        if (scope != null) {
            v = v.and(Validated.test("scope", "scope is a valid npm dependency scope", scope, SCOPES::contains));
        }
        if (version != null) {
            v = v.and(Validated.test("version", "version is a valid npm semver range", version,
                    range -> NodeSemver.validRange(range, NodeSemver.Options.DEFAULT) != null));
        }
        return v;
    }

    @Value
    public static class Accumulator {
        Map<Path, Boolean> packagesByDirectory;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator(new HashMap<>());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        Pattern packageNameMatcher = PackageJsonHelper.compileGlobPattern(packageNamePattern);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    sourceFile.getMarkers().findFirst(NodeResolutionResult.class).ifPresent(resolution ->
                            acc.getPackagesByDirectory().merge(directoryOf(sourceFile.getSourcePath()),
                                    hasDependency(resolution, packageNameMatcher), Boolean::logicalOr));
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
                if (tree instanceof SourceFile && belongsToMatchingPackage(((SourceFile) tree).getSourcePath(), acc)) {
                    return SearchResult.found(tree, "Module has dependency: " + packageNamePattern + (version == null ? "" : "@" + version));
                }
                return tree;
            }
        };
    }

    private boolean hasDependency(NodeResolutionResult resolution, Pattern packageNameMatcher) {
        for (String declaredScope : SCOPES) {
            if (scope != null && !scope.equals(declaredScope)) {
                continue;
            }
            for (Dependency dependency : declared(resolution, declaredScope)) {
                if (matches(dependency, packageNameMatcher)) {
                    return true;
                }
                if (!Boolean.TRUE.equals(onlyDirect) && dependency.getResolved() != null &&
                    hasTransitiveMatch(dependency.getResolved(), packageNameMatcher, new HashSet<>())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasTransitiveMatch(ResolvedDependency resolved, Pattern packageNameMatcher, Set<ResolvedDependency> visited) {
        if (!visited.add(resolved)) {
            return false;
        }
        for (List<Dependency> dependencies : asList(resolved.getDependencies(), resolved.getPeerDependencies(), resolved.getOptionalDependencies())) {
            if (dependencies == null) {
                continue;
            }
            for (Dependency dependency : dependencies) {
                if (matches(dependency, packageNameMatcher) ||
                    (dependency.getResolved() != null && hasTransitiveMatch(dependency.getResolved(), packageNameMatcher, visited))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matches(Dependency dependency, Pattern packageNameMatcher) {
        if (!packageNameMatcher.matcher(dependency.getName()).matches()) {
            return false;
        }
        if (version == null) {
            return true;
        }
        String candidate = dependency.getResolved() != null ?
                dependency.getResolved().getVersion() :
                lowestAdmitted(dependency.getVersionConstraint());
        return candidate != null && NodeSemver.satisfies(candidate, version, NodeSemver.Options.DEFAULT);
    }

    private static @Nullable String lowestAdmitted(@Nullable String versionConstraint) {
        if (versionConstraint == null || NodeSemver.validRange(versionConstraint, NodeSemver.Options.DEFAULT) == null) {
            return null;
        }
        return NodeSemver.minVersion(versionConstraint, NodeSemver.Options.DEFAULT);
    }

    private static List<Dependency> declared(NodeResolutionResult resolution, String declaredScope) {
        List<Dependency> dependencies;
        switch (declaredScope) {
            case "dependencies":
                dependencies = resolution.getDependencies();
                break;
            case "devDependencies":
                dependencies = resolution.getDevDependencies();
                break;
            case "peerDependencies":
                dependencies = resolution.getPeerDependencies();
                break;
            case "optionalDependencies":
                dependencies = resolution.getOptionalDependencies();
                break;
            default:
                dependencies = resolution.getBundledDependencies();
        }
        return dependencies == null ? Collections.emptyList() : dependencies;
    }

    private static boolean belongsToMatchingPackage(Path sourcePath, Accumulator acc) {
        if (acc.getPackagesByDirectory().isEmpty()) {
            return false;
        }
        Path directory = directoryOf(sourcePath);
        while (true) {
            Boolean matches = acc.getPackagesByDirectory().get(directory);
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
