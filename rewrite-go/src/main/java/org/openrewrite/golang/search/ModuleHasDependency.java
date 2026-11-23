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
package org.openrewrite.golang.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.golang.marker.GoProject;
import org.openrewrite.golang.marker.GoResolutionResult;
import org.openrewrite.golang.marker.GoResolutionResult.ModuleRef;
import org.openrewrite.golang.marker.GoResolutionResult.Require;
import org.openrewrite.golang.marker.GoResolutionResult.ResolvedDependency;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class ModuleHasDependency extends ScanningRecipe<ModuleHasDependency.Accumulator> {

    String displayName = "Module has dependency";

    String description = "Searches for Go modules that have a dependency matching the specified module path. " +
                         "Places a `SearchResult` marker on all sources within a module with a matching dependency. " +
                         "This recipe is intended to be used as a precondition for other recipes. " +
                         "For example this could be used to limit the application of a migration to only modules " +
                         "that use a specific version of a library. " +
                         "If the search result you want is instead just the `require` directive in `go.mod`, use the `DependencyInsight` recipe instead.";

    @Option(displayName = "Module pattern",
            description = "A glob pattern to match Go module paths. Use `*` as a wildcard.",
            example = "github.com/aws/aws-sdk-go-v2*")
    String modulePattern;

    @Option(displayName = "Version",
            description = "Match only dependencies with the specified version. " +
                          "An exact version or a version range (see the [version selector syntax]" +
                          "(https://docs.openrewrite.org/reference/dependency-version-selectors)) may be used. " +
                          "Direct dependencies are compared by the version in their `require` directive. " +
                          "All versions are searched by default.",
            required = false,
            example = "1.x")
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
        return String.format("`%s%s`", modulePattern, version == null ? "" : "@" + version);
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
        Set<GoProject> projectsWithDependency;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator(new HashSet<>());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree != null) {
                    Optional<GoProject> project = tree.getMarkers().findFirst(GoProject.class);
                    Optional<GoResolutionResult> resolution = tree.getMarkers().findFirst(GoResolutionResult.class);
                    if (project.isPresent() && resolution.isPresent() && hasDependency(resolution.get())) {
                        acc.getProjectsWithDependency().add(project.get());
                    }
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
                if (tree != null && tree.getMarkers().findFirst(GoProject.class)
                        .filter(acc.getProjectsWithDependency()::contains).isPresent()) {
                    return SearchResult.found(tree, "Module has dependency: " + modulePattern + (version == null ? "" : "@" + version));
                }
                return tree;
            }
        };
    }

    private boolean hasDependency(GoResolutionResult resolution) {
        VersionComparator versionComparator = version == null ? null : Semver.validate(version, null).getValue();
        for (Require require : resolution.getRequires()) {
            if (matches(require.getModulePath(), require.getVersion(), versionComparator)) {
                return true;
            }
            if (!Boolean.TRUE.equals(onlyDirect) &&
                hasTransitiveMatch(resolution, require.getModulePath(), versionComparator, new HashSet<>())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasTransitiveMatch(GoResolutionResult resolution, String modulePath,
                                       @Nullable VersionComparator versionComparator, Set<String> visited) {
        ResolvedDependency resolved = resolution.findResolved(modulePath);
        if (resolved == null || resolved.getDeps() == null ||
            !visited.add(resolved.getModulePath() + "@" + resolved.getVersion())) {
            return false;
        }
        for (ModuleRef ref : resolved.getDeps()) {
            if (matches(ref.getModulePath(), ref.getVersion(), versionComparator) ||
                hasTransitiveMatch(resolution, ref.getModulePath(), versionComparator, visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String modulePath, @Nullable String moduleVersion, @Nullable VersionComparator versionComparator) {
        if (!StringUtils.matchesGlob(modulePath, modulePattern)) {
            return false;
        }
        if (versionComparator == null) {
            return true;
        }
        if (moduleVersion == null) {
            return false;
        }
        String normalized = moduleVersion.startsWith("v") ? moduleVersion.substring(1) : moduleVersion;
        int plus = normalized.indexOf('+');
        if (plus > 0) {
            normalized = normalized.substring(0, plus);
        }
        return versionComparator.isValid(null, normalized);
    }
}
