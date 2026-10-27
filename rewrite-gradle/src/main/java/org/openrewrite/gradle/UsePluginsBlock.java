/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.gradle;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.plugins.AddBuildPlugin;
import org.openrewrite.gradle.trait.GradlePlugin;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class UsePluginsBlock extends Recipe {

    @Getter
    final String displayName = "Apply plugins using the `plugins` block";

    @Getter
    final String description = "Replace a top-level `apply plugin:` statement with an entry in the `plugins` block, " +
            "which Gradle can resolve before configuring the build and which tooling can read without running it. " +
            "Only plugins Gradle itself ships are converted: a third-party plugin named in a `plugins` block needs a " +
            "version, or a `buildscript` classpath entry the block cannot see, and rewriting one without resolving " +
            "that would stop the build configuring. An `apply` inside `subprojects` or `allprojects` is left alone " +
            "too, since a `plugins` block applies only to the script holding it, as is `apply plugin:` given a class " +
            "rather than an id. See the " +
            "[Gradle best practice](https://docs.gradle.org/current/userguide/best_practices_general.html#use_the_plugins_block).";

    @Getter
    final Duration estimatedEffortPerOccurrence = Duration.ofMinutes(5);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof JavaSourceFile)) {
                    return (J) tree;
                }
                JavaSourceFile cu = (JavaSourceFile) tree;

                List<String> pluginIds = corePluginIds(topLevelStatements(cu));
                if (pluginIds.isEmpty()) {
                    return cu;
                }

                // Added before the `apply` statements are removed so that AddPluginVisitor, which relocates a
                // license header onto the block it inserts, sees the file as it was written.
                JavaSourceFile result = cu;
                for (String pluginId : pluginIds) {
                    result = (JavaSourceFile) new AddBuildPlugin(pluginId, null, null, null, null)
                            .getVisitor().visitNonNull(result, ctx);
                }

                // Removing an `apply` whose plugin did not make it into the block would drop the plugin from the
                // build, so nothing is removed unless every one of them landed.
                for (String pluginId : pluginIds) {
                    if (!declaresPlugin(result, pluginId)) {
                        return cu;
                    }
                }
                return withoutCoreApplies(result);
            }
        });
    }

    private static List<Statement> topLevelStatements(JavaSourceFile cu) {
        if (cu instanceof G.CompilationUnit) {
            return ((G.CompilationUnit) cu).getStatements();
        }
        if (cu instanceof K.CompilationUnit) {
            List<Statement> statements = ((K.CompilationUnit) cu).getStatements();
            // A Kotlin script's statements live in the block that is its first statement
            if (!statements.isEmpty() && statements.get(0) instanceof J.Block) {
                return ((J.Block) statements.get(0)).getStatements();
            }
        }
        return new ArrayList<>();
    }

    private static JavaSourceFile withoutCoreApplies(JavaSourceFile cu) {
        if (cu instanceof G.CompilationUnit) {
            G.CompilationUnit g = (G.CompilationUnit) cu;
            return g.withStatements(ListUtils.filter(g.getStatements(), s -> corePluginId(s) == null));
        }
        if (cu instanceof K.CompilationUnit) {
            K.CompilationUnit k = (K.CompilationUnit) cu;
            if (k.getStatements().isEmpty() || !(k.getStatements().get(0) instanceof J.Block)) {
                return cu;
            }
            return k.withStatements(ListUtils.mapFirst(k.getStatements(), b -> {
                J.Block block = (J.Block) b;
                return block.withStatements(ListUtils.filter(block.getStatements(), s -> corePluginId(s) == null));
            }));
        }
        return cu;
    }

    private static List<String> corePluginIds(List<Statement> statements) {
        List<String> pluginIds = new ArrayList<>();
        for (Statement statement : statements) {
            String pluginId = corePluginId(statement);
            if (pluginId != null && !pluginIds.contains(pluginId)) {
                pluginIds.add(pluginId);
            }
        }
        return pluginIds;
    }

    /**
     * The plugin id of an `apply plugin:` statement naming a plugin Gradle ships, or null for anything else —
     * including an id given as a class reference, which has no id to write into the block.
     */
    private static @Nullable String corePluginId(Statement statement) {
        if (!(statement instanceof J.MethodInvocation)) {
            return null;
        }
        J.MethodInvocation m = (J.MethodInvocation) statement;
        if (!"apply".equals(m.getSimpleName()) || m.getArguments().size() != 1) {
            return null;
        }

        Expression argument = m.getArguments().get(0);
        Expression value = null;
        if (argument instanceof G.MapEntry) {
            G.MapEntry entry = (G.MapEntry) argument;
            if (entry.getKey() instanceof J.Literal && "plugin".equals(((J.Literal) entry.getKey()).getValue())) {
                value = entry.getValue();
            }
        } else if (argument instanceof J.Assignment) {
            J.Assignment assignment = (J.Assignment) argument;
            if (assignment.getVariable() instanceof J.Identifier &&
                "plugin".equals(((J.Identifier) assignment.getVariable()).getSimpleName())) {
                value = assignment.getAssignment();
            }
        }

        if (value instanceof J.Literal && ((J.Literal) value).getValue() instanceof String) {
            String pluginId = (String) ((J.Literal) value).getValue();
            return GradlePlugin.isCorePlugin(pluginId) ? pluginId : null;
        }
        return null;
    }

    private static boolean declaresPlugin(JavaSourceFile cu, String pluginId) {
        return new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                if ("id".equals(method.getSimpleName()) && !method.getArguments().isEmpty() &&
                    method.getArguments().get(0) instanceof J.Literal &&
                    pluginId.equals(((J.Literal) method.getArguments().get(0)).getValue())) {
                    found.set(true);
                }
                return super.visitMethodInvocation(method, found);
            }
        }.reduce(cu, new AtomicBoolean()).get();
    }
}
