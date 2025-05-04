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
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class ModuleHasPlugin extends ScanningRecipe<ModuleHasPlugin.Accumulator> {

    @Override
    public String getDisplayName() {
        return "Module has plugin";
    }

    @Override
    public String getDescription() {
        return "Searches for Gradle Projects (modules) that have a plugin matching the specified id or implementing class. " +
               "Places a `SearchResult` marker on all sources within a project with a matching plugin. " +
               "This recipe is intended to be used as a precondition for other recipes. " +
               "For example this could be used to limit the application of a spring boot migration to only projects " +
               "that apply the spring dependency management plugin, limiting unnecessary upgrading. " +
               "If the search result you want is instead just the build.gradle(.kts) file applying the plugin, use the `FindPlugins` recipe instead.";
    }

    @Option(displayName = "Plugin id",
            description = "The unique identifier used to apply a plugin in the `plugins` block. " +
                          "Note that this alone is insufficient to search for plugins applied by fully qualified class name and the `buildscript` block.",
            example = "`com.jfrog.bintray`")
    String pluginId;

    @Option(displayName = "Plugin class",
            description = "The fully qualified name of a class implementing a Gradle plugin. ",
            required = false,
            example = "com.jfrog.bintray.gradle.BintrayPlugin")
    @Nullable
    String pluginClass;

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
                            Tree t = new FindPlugins(pluginId, pluginClass).getVisitor().visit(tree, ctx);
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
                    return SearchResult.found(tree, "Module has plugin: " + pluginId);
                }
                return tree;
            }
        };
    }
}
