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
package org.openrewrite.gradle.plugins;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.trait.GradlePlugin;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.tree.K;

import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.asList;

public class UsePluginsBlockForCorePlugins extends Recipe {

    private static final Set<String> CORE_PLUGINS = new HashSet<>(asList(
            "antlr", "application", "base", "build-dashboard", "checkstyle", "codenarc", "distribution", "ear",
            "eclipse", "eclipse-wtp", "groovy", "groovy-base", "idea", "ivy-publish", "jacoco",
            "jacoco-report-aggregation", "java", "java-base", "java-gradle-plugin", "java-library",
            "java-library-distribution", "java-platform", "java-test-fixtures", "jvm-test-suite", "maven-publish",
            "pmd", "project-report", "scala", "scala-base", "signing", "test-report-aggregation", "version-catalog",
            "war"));

    @Getter
    final String displayName = "Apply core Gradle plugins in the `plugins` block";

    @Getter
    final String description = "Replaces top-level `apply plugin: 'java'` and `apply(plugin = \"java\")` statements for " +
            "Gradle core plugins with an entry in the `plugins { }` block. Core plugins need no version or " +
            "`buildscript` classpath entry, so the change is self-contained. Plugins applied inside `allprojects`, " +
            "`subprojects` or other blocks, and community plugins, are left unchanged. " +
            "See [apply plugins using the plugins block](https://docs.gradle.org/current/userguide/best_practices_general.html#use_the_plugins_block).";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(true), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J preVisit(J tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof JavaSourceFile)) {
                    return tree;
                }
                JavaSourceFile cu = (JavaSourceFile) tree;
                Set<String> declared = declaredPlugins(cu);
                List<String> toDeclare = new ArrayList<>();
                cu = mapTopLevelStatements(cu, statement -> {
                    String pluginId = appliedCorePlugin(statement);
                    if (pluginId == null) {
                        return statement;
                    }
                    if (!declared.contains(pluginId) && !toDeclare.contains(pluginId)) {
                        toDeclare.add(pluginId);
                    }
                    return null;
                });
                for (String pluginId : toDeclare) {
                    cu = (JavaSourceFile) new AddPluginVisitor(pluginId, null, null, null, false)
                            .visitNonNull(cu, ctx, getCursor().getParentOrThrow());
                }
                return cu;
            }
        });
    }

    private static Set<String> declaredPlugins(JavaSourceFile cu) {
        Set<String> declared = new HashSet<>();
        new GradlePlugin.Matcher().asVisitor(plugin -> {
            if (plugin.getPluginId() != null && isInPluginsBlock(plugin)) {
                declared.add(plugin.getPluginId());
            }
            return plugin.getTree();
        }).visit(cu, 0);
        return declared;
    }

    private static boolean isInPluginsBlock(GradlePlugin plugin) {
        return plugin.getCursor().getPathAsStream().anyMatch(p -> p instanceof J.MethodInvocation &&
                                                                  "plugins".equals(((J.MethodInvocation) p).getSimpleName()));
    }

    private static JavaSourceFile mapTopLevelStatements(JavaSourceFile cu, Function<Statement, @Nullable Statement> mapper) {
        if (cu instanceof G.CompilationUnit) {
            G.CompilationUnit g = (G.CompilationUnit) cu;
            return g.withStatements(mapKeepingLeadingPrefix(g.getStatements(), mapper));
        }
        if (cu instanceof K.CompilationUnit) {
            K.CompilationUnit k = (K.CompilationUnit) cu;
            return k.withStatements(ListUtils.mapFirst(k.getStatements(), s -> s instanceof J.Block ?
                    ((J.Block) s).withStatements(mapKeepingLeadingPrefix(((J.Block) s).getStatements(), mapper)) : s));
        }
        return cu;
    }

    private static List<Statement> mapKeepingLeadingPrefix(List<Statement> statements, Function<Statement, @Nullable Statement> mapper) {
        List<Statement> mapped = ListUtils.map(statements, mapper);
        if (mapped.isEmpty() || statements.isEmpty() || mapped.get(0) == statements.get(0)) {
            return mapped;
        }
        return Space.formatFirstPrefix(mapped, statements.get(0).getPrefix());
    }

    private static @Nullable String appliedCorePlugin(Statement statement) {
        if (!(statement instanceof J.MethodInvocation)) {
            return null;
        }
        J.MethodInvocation m = (J.MethodInvocation) statement;
        if (!"apply".equals(m.getSimpleName()) || m.getSelect() != null || m.getArguments().size() != 1) {
            return null;
        }
        Expression arg = m.getArguments().get(0);
        Expression key = null;
        Expression value = null;
        if (arg instanceof G.MapEntry) {
            key = ((G.MapEntry) arg).getKey();
            value = ((G.MapEntry) arg).getValue();
        } else if (arg instanceof J.Assignment) {
            key = ((J.Assignment) arg).getVariable();
            value = ((J.Assignment) arg).getAssignment();
        }
        boolean pluginKey = key instanceof J.Literal && "plugin".equals(((J.Literal) key).getValue()) ||
                            key instanceof J.Identifier && "plugin".equals(((J.Identifier) key).getSimpleName());
        if (pluginKey && value instanceof J.Literal && CORE_PLUGINS.contains(((J.Literal) value).getValue())) {
            return (String) ((J.Literal) value).getValue();
        }
        return null;
    }
}
