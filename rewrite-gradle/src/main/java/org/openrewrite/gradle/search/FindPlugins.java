/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.tree.GradlePlugin;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindPlugins extends Recipe {
    @Option(displayName = "Plugin id",
            description = "The `ID` part of `plugin { ID }`.",
            example = "`com.jfrog.bintray`")
    String pluginId;

    @Override
    public String getDisplayName() {
        return "Find Gradle plugin";
    }

    @Override
    public String getDescription() {
        return "Find a Gradle plugin by id.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new IsBuildGradle<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher pluginMatcher = new MethodMatcher("PluginSpec id(..)", false);
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (pluginMatcher.matches(method)) {
                    if (method.getArguments().get(0) instanceof J.Literal &&
                        pluginId.equals(((J.Literal) method.getArguments().get(0)).getValue())) {
                        return SearchResult.found(method);
                    }
                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }

    /**
     * @param j               The subtree to search.
     * @param pluginIdPattern A method pattern. See {@link MethodMatcher} for details about this syntax.
     * @return A set of {@link J.MethodInvocation} and {@link J.MemberReference} representing plugin
     * definitions in the plugins block.
     */
    public static List<GradlePlugin> find(J j, String pluginIdPattern) {
        List<J.MethodInvocation> plugins = TreeVisitor.collect(
                new FindPlugins(pluginIdPattern).getVisitor(),
                j,
                new ArrayList<>(),
                J.MethodInvocation.class,
                Function.identity()
        );

        MethodMatcher idMatcher = new MethodMatcher("PluginSpec id(..)", false);
        MethodMatcher versionMatcher = new MethodMatcher("Plugin version(..)", false);
        return plugins.stream().flatMap(plugin -> {
            if (versionMatcher.matches(plugin) && idMatcher.matches(plugin.getSelect())) {
                return Stream.of(new GradlePlugin(
                        plugin,
                        requireNonNull(((J.Literal) requireNonNull(((J.MethodInvocation) plugin.getSelect()))
                                .getArguments().get(0)).getValue()).toString(),
                        requireNonNull(((J.Literal) plugin.getArguments().get(0)).getValue()).toString()
                ));
            } else if (idMatcher.matches(plugin)) {
                return Stream.of(new GradlePlugin(
                        plugin,
                        requireNonNull(((J.Literal) requireNonNull(plugin)
                                .getArguments().get(0)).getValue()).toString(),
                        null
                ));
            }
            return Stream.<GradlePlugin>empty();
        }).collect(Collectors.toList());
    }
}
