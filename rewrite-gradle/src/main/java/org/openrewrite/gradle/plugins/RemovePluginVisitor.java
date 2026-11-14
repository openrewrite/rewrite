/*
 * Copyright 2023 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.gradle.internal.RemoveStatementsVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.tree.K;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemovePluginVisitor extends RemoveStatementsVisitor<ExecutionContext> {
    String pluginId;

    // Wildcard type because KTS extension functions have a file-level declaring type, not Project/Settings
    MethodMatcher pluginsMatcher = new MethodMatcher("* plugins(..)", false);
    MethodMatcher applyPluginMatcher = new MethodMatcher("org.gradle.api.Project apply(..)", true);
    MethodMatcher buildPluginsContainerMatcher = new MethodMatcher("org.gradle.api.Project plugins(..)", true);
    MethodMatcher settingsPluginsContainerMatcher = new MethodMatcher("org.gradle.api.initialization.Settings plugins(..)", true);

    MethodMatcher pluginIdMatcher = new MethodMatcher("org.gradle.plugin.use.PluginDependenciesSpec id(..)", true);
    MethodMatcher pluginVersionMatcher = new MethodMatcher("org.gradle.plugin.use.PluginDependencySpec version(..)", true);
    MethodMatcher pluginApplyMatcher = new MethodMatcher("org.gradle.plugin.use.PluginDependencySpec apply(..)", true);

    @Override
    public J.Block visitBlock(J.Block block, ExecutionContext executionContext) {
        J.Block b = super.visitBlock(block, executionContext);

        J.MethodInvocation enclosingMethod = getCursor().firstEnclosing(J.MethodInvocation.class);
        if (enclosingMethod == null || !isPluginsMethod(enclosingMethod)) {
            return b;
        }

        for (Statement statement : b.getStatements()) {
            J expression = statement instanceof J.Return ? ((J.Return) statement).getExpression() : statement;
            if (expression instanceof J.MethodInvocation && isPluginDeclaration((J.MethodInvocation) expression)) {
                remove((J.MethodInvocation) expression);
            }
        }
        return b;
    }

    private boolean isPluginDeclaration(J.MethodInvocation m) {
        Expression spec = m;
        if (isApplyMethodInvocation(spec)) {
            spec = ((J.MethodInvocation) spec).getSelect();
        }
        if (isVersionMethodInvocation(spec)) {
            spec = ((J.MethodInvocation) spec).getSelect();
        }
        return isIdMethodInvocation(spec) && isPluginLiteral(((J.MethodInvocation) spec).getArguments().get(0));
    }

    private boolean isPluginsMethod(J.MethodInvocation m) {
        // Specifically for Kotlin type information is still missing; match strongly where possible for Groovy
        return getCursor().firstEnclosing(K.CompilationUnit.class) != null ?
                pluginsMatcher.matches(m) :
                buildPluginsContainerMatcher.matches(m, true) || settingsPluginsContainerMatcher.matches(m);
    }

    private boolean isPluginLiteral(Expression expression) {
        return expression instanceof J.Literal &&
                pluginId.equals(((J.Literal) expression).getValue());
    }

    private boolean isIdMethodInvocation(@Nullable Expression expr) {
        if (!(expr instanceof J.MethodInvocation)) {
            return false;
        }
        return pluginIdMatcher.matches((J.MethodInvocation) expr, true);
    }

    private boolean isVersionMethodInvocation(@Nullable Expression expr) {
        if (!(expr instanceof J.MethodInvocation)) {
            return false;
        }
        return pluginVersionMatcher.matches((J.MethodInvocation) expr, true);
    }

    private boolean isApplyMethodInvocation(@Nullable Expression expr) {
        if (!(expr instanceof J.MethodInvocation)) {
            return false;
        }
        return pluginApplyMatcher.matches((J.MethodInvocation) expr, true);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
        J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);

        // Check for empty plugins{} block
        if (isPluginsMethod(m)) {
            if (m.getArguments().get(0) instanceof J.Lambda &&
                    ((J.Lambda) m.getArguments().get(0)).getBody() instanceof J.Block &&
                    ((J.Block) ((J.Lambda) m.getArguments().get(0)).getBody()).getStatements().isEmpty()) {
                remove(m);
            }
        }
        // Check for TOP-LEVEL apply plugin: "..." or apply(plugin = "...")
        else if (applyPluginMatcher.matches(m, true)) {
            for (Expression arg : m.getArguments()) {
                if (arg instanceof G.MapEntry) {
                    G.MapEntry me = (G.MapEntry) arg;
                    if (me.getKey() instanceof J.Literal && "plugin".equals(((J.Literal) me.getKey()).getValue()) &&
                            me.getValue() instanceof J.Literal && pluginId.equals(((J.Literal) me.getValue()).getValue())) {
                        remove(m);
                    }
                } else if (arg instanceof J.Assignment) {
                    J.Assignment as = (J.Assignment) arg;
                    if (as.getVariable() instanceof J.Identifier && "plugin".equals(((J.Identifier) as.getVariable()).getSimpleName()) &&
                            as.getAssignment() instanceof J.Literal && pluginId.equals(((J.Literal) as.getAssignment()).getValue())) {
                        remove(m);
                    }
                }
            }
        }

        return m;
    }
}
