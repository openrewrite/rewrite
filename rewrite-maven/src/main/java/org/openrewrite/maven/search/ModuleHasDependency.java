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
package org.openrewrite.maven.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class ModuleHasDependency extends ScanningRecipe<ModuleHasDependency.Accumulator> {

    @Override
    public String getDisplayName() {
        return "Module has dependency";
    }

    @Override
    public String getDescription() {
        return "Searches for Maven modules that have a dependency matching the specified groupId and artifactId. " +
               "Places a `SearchResult` marker on all sources within a module with a matching dependency. " +
               "This recipe is intended to be used as a precondition for other recipes. " +
               "For example this could be used to limit the application of a spring boot migration to only projects " +
               "that use spring-boot-starter, limiting unnecessary upgrading. " +
               "If the search result you want is instead just the build.gradle(.kts) file applying the plugin, use the `FindDependency` recipe instead.";
    }

    @Option(displayName = "Group pattern",
            description = "Group glob pattern used to match dependencies.",
            example = "com.fasterxml.jackson.module")
    String groupIdPattern;

    @Option(displayName = "Artifact pattern",
            description = "Artifact glob pattern used to match dependencies.",
            example = "jackson-module-*")
    String artifactIdPattern;

    @Option(displayName = "Scope",
            description = "Match dependencies with the specified scope. All scopes are searched by default.",
            valid = {"compile", "test", "runtime", "provided", "system"},
            example = "compile",
            required = false)
    @Nullable
    String scope;

    @Option(displayName = "Version",
            description = "Match only dependencies with the specified version. " +
                          "Node-style [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors) may be used." +
                          "All versions are searched by default.",
            example = "1.x",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Only direct",
            description = "If enabled, transitive dependencies will not be considered. All dependencies are searched by default.",
            required = false,
            example = "true")
    @Nullable
    Boolean onlyDirect;

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
                            Tree t = new DependencyInsight(groupIdPattern, artifactIdPattern, scope, version, onlyDirect).getVisitor().visit(tree, ctx);
                            if (t != tree) {
                                acc.getProjectsWithDependency().add(jp);
                            }
                        });
                return tree;
            }
        };
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
                    return SearchResult.found(tree, "Module has dependency: " + groupIdPattern + ":" + artifactIdPattern + (version == null ? "" : ":" + version));
                }
                return tree;
            }
        };
    }
}
