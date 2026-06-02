/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class ModuleHasDependency extends ScanningRecipe<ModuleHasDependency.Accumulator> {

    String displayName = "Module has dependency";

    String description = "Searches for Gradle Projects (modules) that have a dependency matching the specified id or implementing class. " +
               "Places a `SearchResult` marker on all sources within a project with a matching dependency. " +
               "This recipe is intended to be used as a precondition for other recipes. " +
               "For example this could be used to limit the application of a spring boot migration to only projects " +
               "that use spring-boot-starter, limiting unnecessary upgrading. " +
               "If the search result you want is instead just the build.gradle(.kts) file that use the dependency, use the `FindDependency` recipe instead.";

    @Option(displayName = "Group pattern",
            description = "Group glob pattern used to match dependencies.",
            example = "com.fasterxml.jackson.module")
    String groupIdPattern;

    @Option(displayName = "Artifact pattern",
            description = "Artifact glob pattern used to match dependencies.",
            example = "jackson-module-*")
    String artifactIdPattern;

    @Option(displayName = "Version",
            description = "Match only dependencies with the specified version. " +
                          "Node-style [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors) may be used." +
                          "All versions are searched by default.",
            example = "1.x",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Scope",
            description = "Match dependencies with the specified scope. If not specified, all configurations will be searched.",
            example = "compileClasspath",
            required = false)
    @Nullable
    String configuration;

    @Value
    public static class Accumulator {
        Set<JavaProject> projectsWithDependency;
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
                assert tree != null;
                tree.getMarkers()
                        .findFirst(JavaProject.class)
                        .ifPresent(jp -> {
                            if (hasDependency(tree)) {
                                acc.getProjectsWithDependency().add(jp);
                            }
                        });
                return tree;
            }
        };
    }

    private boolean hasDependency(Tree tree) {
        Optional<GradleProject> maybeGradleProject = tree.getMarkers().findFirst(GradleProject.class);
        if (!maybeGradleProject.isPresent()) {
            return false;
        }

        GradleProject gp = maybeGradleProject.get();
        VersionComparator versionComparator = version != null ? Semver.validate(version, null).getValue() : null;
        for (GradleDependencyConfiguration c : gp.getConfigurations()) {
            if (configuration != null && !configuration.isEmpty() && !c.getName().equals(configuration)) {
                continue;
            }
            for (ResolvedDependency resolvedDependency : c.getDirectResolved()) {
                ResolvedDependency found = resolvedDependency.findDependency(groupIdPattern, artifactIdPattern);
                if (found != null && (versionComparator == null || versionComparator.isValid(null, found.getVersion()))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                assert tree != null;
                Optional<JavaProject> maybeJp = tree.getMarkers().findFirst(JavaProject.class);
                if (!maybeJp.isPresent()) {
                    return tree;
                }
                JavaProject jp = maybeJp.get();
                if (acc.getProjectsWithDependency().contains(jp)) {
                    return SearchResult.found(tree, "Module has dependency: " + groupIdPattern + ":" + artifactIdPattern + (StringUtils.isNullOrEmpty(version) ? "" : ":" + version) + (StringUtils.isNullOrEmpty(configuration) ? "" : " in configuration "));
                }
                return tree;
            }
        };
    }
}
