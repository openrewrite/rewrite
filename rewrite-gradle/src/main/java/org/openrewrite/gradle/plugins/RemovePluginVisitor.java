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
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.tree.K;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemovePluginVisitor extends JavaIsoVisitor<ExecutionContext> {
    String pluginId;

    MethodMatcher buildPluginsContainerMatcher = new MethodMatcher("RewriteGradleProject plugins(..)");
    MethodMatcher applyPluginMatcher = new MethodMatcher("RewriteGradleProject apply(..)");
    MethodMatcher settingsPluginsContainerMatcher = new MethodMatcher("RewriteSettings plugins(..)");

    MethodMatcher buildPluginMatcher = new MethodMatcher("PluginSpec id(..)");
    MethodMatcher buildPluginWithVersionMatcher = new MethodMatcher("Plugin version(..)");
    MethodMatcher buildPluginWithApplyMatcher = new MethodMatcher("Plugin apply(..)");
    MethodMatcher settingsPluginMatcher = new MethodMatcher("PluginSpec id(..)");
    MethodMatcher settingsPluginWithVersionMatcher = new MethodMatcher("Plugin version(..)");
    MethodMatcher settingsPluginWithApplyMatcher = new MethodMatcher("Plugin apply(..)");

    @Override
    public J.Block visitBlock(J.Block block, ExecutionContext executionContext) {
        J.Block b = super.visitBlock(block, executionContext);

        J.MethodInvocation enclosingMethod = getCursor().firstEnclosing(J.MethodInvocation.class);
        if (enclosingMethod == null) {
            return b;
        }

        boolean isKotlin = getCursor().firstEnclosing(K.CompilationUnit.class) != null;
        boolean isPluginsBlock = isKotlin ?
                "plugins".equals(enclosingMethod.getSimpleName()) :
                buildPluginsContainerMatcher.matches(enclosingMethod) || settingsPluginsContainerMatcher.matches(enclosingMethod);
        if (!isPluginsBlock) {
            return b;
        }

        return b.withStatements(ListUtils.map(b.getStatements(), statement -> {
            J.MethodInvocation m;
            if (statement instanceof J.MethodInvocation) {
                m = (J.MethodInvocation) statement;
            } else if (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof J.MethodInvocation) {
                m = (J.MethodInvocation) ((J.Return) statement).getExpression();
            } else {
                return statement;
            }

            // Check for id("pluginId")
            if (isIdMethodInvocation(m, isKotlin)) {
                if (isPlugin(m.getArguments().get(0))) {
                    return null;
                }
            }
            // Check for id("pluginId").version("...")
            else if (isVersionMethodInvocation(m, isKotlin)) {
                if (m.getSelect() instanceof J.MethodInvocation &&
                        isPlugin(((J.MethodInvocation) m.getSelect()).getArguments().get(0))) {
                    return null;
                }
            }
            // Check for id("pluginId").apply(...) or id("pluginId").version("...").apply(...)
            else if (isApplyMethodInvocation(m, isKotlin)) {
                if (isIdMethodInvocation(m.getSelect(), isKotlin)) {
                    if (m.getSelect() instanceof J.MethodInvocation &&
                            isPlugin(((J.MethodInvocation) m.getSelect()).getArguments().get(0))) {
                        return null;
                    }
                } else if (isVersionMethodInvocation(m.getSelect(), isKotlin)) {
                    if (m.getSelect() instanceof J.MethodInvocation &&
                            isIdMethodInvocation(((J.MethodInvocation) m.getSelect()).getSelect(), isKotlin)) {
                        if (((J.MethodInvocation) m.getSelect()).getSelect() instanceof J.MethodInvocation &&
                                isPlugin(((J.MethodInvocation) ((J.MethodInvocation) m.getSelect()).getSelect()).getArguments().get(0))) {
                            return null;
                        }
                    }
                }
            }

            return statement;
        }));
    }

    private boolean isPlugin(Expression expression) {
        return expression instanceof J.Literal &&
                pluginId.equals(((J.Literal) expression).getValue());
    }

    private boolean isIdMethodInvocation(@Nullable Expression expr, boolean isKotlin) {
        if (!(expr instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation m = (J.MethodInvocation) expr;
        return isKotlin ?
                "id".equals(m.getSimpleName()) :
                (buildPluginMatcher.matches(m) || settingsPluginMatcher.matches(m));
    }

    private boolean isVersionMethodInvocation(@Nullable Expression expr, boolean isKotlin) {
        if (!(expr instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation m = (J.MethodInvocation) expr;
        return isKotlin ?
                "version".equals(m.getSimpleName()) :
                (buildPluginWithVersionMatcher.matches(m) || settingsPluginWithVersionMatcher.matches(m));
    }

    private boolean isApplyMethodInvocation(@Nullable Expression expr, boolean isKotlin) {
        if (!(expr instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation m = (J.MethodInvocation) expr;
        return isKotlin ?
                "apply".equals(m.getSimpleName()) :
                (buildPluginWithApplyMatcher.matches(m) || settingsPluginWithApplyMatcher.matches(m));
    }

    @Override
    public J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
        J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);

        boolean isKotlin = getCursor().firstEnclosing(K.CompilationUnit.class) != null;

        // Check for empty plugins{} block
        boolean isPluginsMethod = isKotlin ?
                "plugins".equals(m.getSimpleName()) :
                (buildPluginsContainerMatcher.matches(m) || settingsPluginsContainerMatcher.matches(m));

        if (isPluginsMethod) {
            if (m.getArguments().get(0) instanceof J.Lambda &&
                    ((J.Lambda) m.getArguments().get(0)).getBody() instanceof J.Block &&
                    ((J.Block) ((J.Lambda) m.getArguments().get(0)).getBody()).getStatements().isEmpty()) {
                return null;
            }
        }
        // Check for TOP-LEVEL apply plugin: "..." or apply(plugin = "...")
        else if ((isKotlin && "apply".equals(m.getSimpleName())) || (!isKotlin && applyPluginMatcher.matches(m))) {
            for (Expression arg : m.getArguments()) {
                if (arg instanceof G.MapEntry) {
                    G.MapEntry me = (G.MapEntry) arg;
                    if (me.getKey() instanceof J.Literal && me.getValue() instanceof J.Literal) {
                        J.Literal pluginLiteral = (J.Literal) me.getKey();
                        J.Literal pluginIdLiteral = (J.Literal) me.getValue();
                        if ("plugin".equals(pluginLiteral.getValue()) && pluginId.equals(pluginIdLiteral.getValue())) {
                            return null;
                        }
                    }
                } else if (arg instanceof J.Assignment) {
                    J.Assignment assignment = (J.Assignment) arg;
                    if (assignment.getVariable() instanceof J.Identifier && "plugin".equals(((J.Identifier) assignment.getVariable()).getSimpleName()) &&
                            assignment.getAssignment() instanceof J.Literal && pluginId.equals(((J.Literal) assignment.getAssignment()).getValue())) {
                        return null;
                    }
                }
            }
        }

        return m;
    }
}
