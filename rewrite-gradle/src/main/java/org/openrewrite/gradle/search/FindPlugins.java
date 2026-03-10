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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.tree.GradlePlugin;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindPlugins extends Recipe {
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

    @Override
    public Validated<Object> validate(ExecutionContext ctx) {
        return Validated.none().and(new Validated.Either<>(
                Validated.notBlank("pluginId", pluginId),
                Validated.notBlank("pluginClass", pluginClass)));
    }

    String displayName = "Find Gradle plugin";

    String description = "Find a Gradle plugin by id and/or class name. " +
               "For best results both should be specified, as one cannot automatically be used to infer the other.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher pluginMatcher = new MethodMatcher("org.gradle.plugin.use.PluginDependenciesSpec id(..)", true);

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile s = (SourceFile) tree;

                AtomicBoolean found = new AtomicBoolean(false);
                TreeVisitor<?, ExecutionContext> jv = Preconditions.check(
                        Preconditions.or(new IsBuildGradle<>(), new IsSettingsGradle<>()),
                        new JavaVisitor<ExecutionContext>() {

                            @Override
                            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                                if (pluginMatcher.matches(method, true)) {
                                    if (method.getArguments().get(0) instanceof J.Literal &&
                                        pluginId.equals(((J.Literal) method.getArguments().get(0)).getValue())) {
                                        found.set(true);
                                        return SearchResult.found(method);
                                    }
                                }
                                return super.visitMethodInvocation(method, ctx);
                            }
                        });
                if (jv.isAcceptable(s, ctx)) {
                    s = (SourceFile) jv.visitNonNull(s, ctx);
                }

                // Even if we couldn't find a declaration the metadata might show the plugin is in use
                GradleProject gp = s.getMarkers().findFirst(GradleProject.class).orElse(null);
                if (!found.get() && gp != null && gp.getPlugins().stream().anyMatch(plugin ->
                        matchesGlob(plugin.getId(), pluginId) || matchesGlob(plugin.getFullyQualifiedClassName(), pluginClass))) {
                    s = SearchResult.found(s);
                }

                return s;
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
                new FindPlugins(pluginIdPattern, null).getVisitor(),
                j,
                new ArrayList<>(),
                J.MethodInvocation.class,
                Function.identity()
        );

        MethodMatcher idMatcher = new MethodMatcher("org.gradle.plugin.use.PluginDependenciesSpec id(..)", true);
        MethodMatcher versionMatcher = new MethodMatcher("org.gradle.plugin.use.PluginDependencySpec version(..)", true);
        List<GradlePlugin> pluginsWithVersion = plugins.stream()
                .flatMap(plugin -> {
                    if (versionMatcher.matches(plugin, true) &&
                            plugin.getSelect() instanceof J.MethodInvocation && idMatcher.matches((J.MethodInvocation) plugin.getSelect(), true) &&
                            plugin.getArguments().get(0) instanceof J.Literal) {
                        return Stream.of(new GradlePlugin(
                                plugin,
                                requireNonNull(((J.Literal) requireNonNull(((J.MethodInvocation) plugin.getSelect()))
                                        .getArguments().get(0)).getValue()).toString(),
                                requireNonNull(((J.Literal) plugin.getArguments().get(0)).getValue()).toString()
                        ));
                    }
                    return Stream.empty();
                }).collect(toList());
        List<GradlePlugin> pluginsWithoutVersion = plugins.stream().flatMap(plugin -> {
            if (idMatcher.matches(plugin, true) && pluginsWithVersion.stream()
                    .noneMatch(it -> it.getPluginId().equals(plugin.getSimpleName()))) {
                return Stream.of(new GradlePlugin(
                        plugin,
                        requireNonNull(((J.Literal) requireNonNull(plugin)
                                .getArguments().get(0)).getValue()).toString(),
                        null
                ));
            }
            return Stream.empty();
        }).collect(toList());

        List<GradlePlugin> result = new ArrayList<>(pluginsWithVersion.size() + pluginsWithoutVersion.size());
        result.addAll(pluginsWithVersion);
        result.addAll(pluginsWithoutVersion);
        return result;
    }
}
