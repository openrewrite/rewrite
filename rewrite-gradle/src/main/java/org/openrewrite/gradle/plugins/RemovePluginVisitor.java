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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
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

        boolean isKotlin = getCursor().firstEnclosing(K.CompilationUnit.class) != null;
        J.MethodInvocation m = getCursor().firstEnclosing(J.MethodInvocation.class);
        
        boolean isPluginsBlock = isKotlin 
            ? (m != null && "plugins".equals(m.getSimpleName()))
            : (m != null && (buildPluginsContainerMatcher.matches(m) || settingsPluginsContainerMatcher.matches(m)));
        
        if (isPluginsBlock) {
            b = b.withStatements(ListUtils.map(b.getStatements(), statement -> {
                if (!(statement instanceof J.MethodInvocation ||
                        (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof J.MethodInvocation))) {
                    return statement;
                }

                J.MethodInvocation m2 = (J.MethodInvocation) (statement instanceof J.Return ? ((J.Return) statement).getExpression() : statement);
                
                // Check for id("pluginId")
                if (matchesIdCall(m2, isKotlin)) {
                    if (m2.getArguments().get(0) instanceof J.Literal && 
                        pluginId.equals(((J.Literal) m2.getArguments().get(0)).getValue())) {
                        return null;
                    }
                } 
                // Check for id("pluginId").version("...")
                else if (matchesVersionCall(m2, isKotlin)) {
                    if (m2.getSelect() instanceof J.MethodInvocation &&
                        ((J.MethodInvocation) m2.getSelect()).getArguments().get(0) instanceof J.Literal &&
                        pluginId.equals(((J.Literal) ((J.MethodInvocation) m2.getSelect()).getArguments().get(0)).getValue())) {
                        return null;
                    }
                } 
                // Check for id("pluginId").apply(...) or id("pluginId").version("...").apply(...)
                else if (matchesApplyCall(m2, isKotlin)) {
                    if (matchesIdCall(m2.getSelect(), isKotlin)) {
                        if (m2.getSelect() instanceof J.MethodInvocation &&
                            ((J.MethodInvocation) m2.getSelect()).getArguments().get(0) instanceof J.Literal &&
                            pluginId.equals(((J.Literal) ((J.MethodInvocation) m2.getSelect()).getArguments().get(0)).getValue())) {
                            return null;
                        }
                    } else if (matchesVersionCall(m2.getSelect(), isKotlin)) {
                        if (m2.getSelect() instanceof J.MethodInvocation &&
                            matchesIdCall(((J.MethodInvocation) m2.getSelect()).getSelect(), isKotlin)) {
                            if (((J.MethodInvocation) m2.getSelect()).getSelect() instanceof J.MethodInvocation &&
                                ((J.MethodInvocation) ((J.MethodInvocation) m2.getSelect()).getSelect()).getArguments().get(0) instanceof J.Literal &&
                                pluginId.equals(((J.Literal) ((J.MethodInvocation) ((J.MethodInvocation) m2.getSelect()).getSelect()).getArguments().get(0)).getValue())) {
                                return null;
                            }
                        }
                    }
                }

                return statement;
            }));
        }

        return b;
    }

    private boolean matchesIdCall(Expression expr, boolean isKotlin) {
        if (!(expr instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation m = (J.MethodInvocation) expr;
        return isKotlin 
            ? "id".equals(m.getSimpleName())
            : (buildPluginMatcher.matches(m) || settingsPluginMatcher.matches(m));
    }

    private boolean matchesVersionCall(Expression expr, boolean isKotlin) {
        if (!(expr instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation m = (J.MethodInvocation) expr;
        return isKotlin 
            ? "version".equals(m.getSimpleName())
            : (buildPluginWithVersionMatcher.matches(m) || settingsPluginWithVersionMatcher.matches(m));
    }

    private boolean matchesApplyCall(Expression expr, boolean isKotlin) {
        if (!(expr instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation m = (J.MethodInvocation) expr;
        return isKotlin 
            ? "apply".equals(m.getSimpleName())
            : (buildPluginWithApplyMatcher.matches(m) || settingsPluginWithApplyMatcher.matches(m));
    }

    @Override
    public J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
        J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);

        boolean isKotlin = getCursor().firstEnclosing(K.CompilationUnit.class) != null;

        // Check for empty plugins{} block
        boolean isPluginsMethod = isKotlin 
            ? "plugins".equals(m.getSimpleName())
            : (buildPluginsContainerMatcher.matches(m) || settingsPluginsContainerMatcher.matches(m));
        
        if (isPluginsMethod) {
            if (m.getArguments().get(0) instanceof J.Lambda &&
                    ((J.Lambda) m.getArguments().get(0)).getBody() instanceof J.Block &&
                    ((J.Block) ((J.Lambda) m.getArguments().get(0)).getBody()).getStatements().isEmpty()) {
                return null;
            }
        } 
        // Check for TOP-LEVEL apply plugin: "..." or apply(plugin = "...")
        else if((isKotlin && "apply".equals(m.getSimpleName())) || (!isKotlin && applyPluginMatcher.matches(m))) {
            for (Expression arg : m.getArguments()) {
                if(arg instanceof G.MapEntry) {
                    G.MapEntry me = (G.MapEntry) arg;
                    if(me.getKey() instanceof J.Literal && me.getValue() instanceof J.Literal) {
                        J.Literal pluginLiteral = (J.Literal) me.getKey();
                        J.Literal pluginIdLiteral = (J.Literal) me.getValue();
                        if("plugin".equals(pluginLiteral.getValue()) && pluginId.equals(pluginIdLiteral.getValue())) {
                            return null;
                        }
                    }
                }
                else if(arg instanceof J.Assignment) {
                    J.Assignment assignment = (J.Assignment) arg;                    
                    if(assignment.getVariable() instanceof J.Identifier && "plugin".equals(((J.Identifier) assignment.getVariable()).getSimpleName()) &&
                        assignment.getAssignment() instanceof J.Literal && pluginId.equals(((J.Literal) assignment.getAssignment()).getValue())) {
                        return null;
                    }
                }
            }
        }

        return m;
    }
}
