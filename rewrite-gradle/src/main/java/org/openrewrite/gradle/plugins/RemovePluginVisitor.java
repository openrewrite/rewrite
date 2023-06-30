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
import org.openrewrite.ExecutionContext;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemovePluginVisitor extends GroovyIsoVisitor<ExecutionContext> {
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

        J.MethodInvocation m = getCursor().firstEnclosing(J.MethodInvocation.class);
        if (m != null && buildPluginsContainerMatcher.matches(m) || settingsPluginsContainerMatcher.matches(m)) {
            b = b.withStatements(ListUtils.map(b.getStatements(), statement -> {
                if (!(statement instanceof J.MethodInvocation
                        || (statement instanceof J.Return && ((J.Return) statement).getExpression() instanceof J.MethodInvocation))) {
                    return statement;
                }

                J.MethodInvocation m2 = (J.MethodInvocation) (statement instanceof J.Return ? ((J.Return) statement).getExpression() : statement);
                if (buildPluginMatcher.matches(m2) || settingsPluginMatcher.matches(m2)) {
                    if (m2.getArguments().get(0) instanceof J.Literal && pluginId.equals(((J.Literal) m2.getArguments().get(0)).getValue())) {
                        return null;
                    }
                } else if (buildPluginWithVersionMatcher.matches(m2) || settingsPluginWithVersionMatcher.matches(m2)) {
                    if (m2.getSelect() instanceof J.MethodInvocation
                            && ((J.MethodInvocation) m2.getSelect()).getArguments().get(0) instanceof J.Literal
                            && pluginId.equals(((J.Literal) ((J.MethodInvocation) m2.getSelect()).getArguments().get(0)).getValue())) {
                        return null;
                    }
                } else if (buildPluginWithApplyMatcher.matches(m2) || settingsPluginWithApplyMatcher.matches(m2)) {
                    if (buildPluginMatcher.matches(m2.getSelect()) || settingsPluginMatcher.matches(m2.getSelect())) {
                        if (m2.getSelect() instanceof J.MethodInvocation
                                && ((J.MethodInvocation) m2.getSelect()).getArguments().get(0) instanceof J.Literal
                                && pluginId.equals(((J.Literal) ((J.MethodInvocation) m2.getSelect()).getArguments().get(0)).getValue())) {
                            return null;
                        }
                    } else if (buildPluginWithVersionMatcher.matches(m2.getSelect()) || settingsPluginWithVersionMatcher.matches(m2.getSelect())) {
                        if (m2.getSelect() instanceof J.MethodInvocation
                                && (buildPluginMatcher.matches(((J.MethodInvocation) m2.getSelect()).getSelect()) || settingsPluginMatcher.matches(((J.MethodInvocation) m2.getSelect()).getSelect()))) {
                            if (((J.MethodInvocation) m2.getSelect()).getSelect() instanceof J.MethodInvocation
                                    && ((J.MethodInvocation) ((J.MethodInvocation) m2.getSelect()).getSelect()).getArguments().get(0) instanceof J.Literal
                                    && pluginId.equals(((J.Literal) ((J.MethodInvocation) ((J.MethodInvocation) m2.getSelect()).getSelect()).getArguments().get(0)).getValue())) {
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

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
        J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);

        if (buildPluginsContainerMatcher.matches(m) || settingsPluginsContainerMatcher.matches(m)) {
            if (m.getArguments().get(0) instanceof J.Lambda
                    && ((J.Lambda) m.getArguments().get(0)).getBody() instanceof J.Block
                    && ((J.Block) ((J.Lambda) m.getArguments().get(0)).getBody()).getStatements().isEmpty()) {
                //noinspection DataFlowIssue
                return null;
            }
        } else if(applyPluginMatcher.matches(m)) {
            for (Expression arg : m.getArguments()) {
                if(arg instanceof G.MapEntry) {
                    G.MapEntry me = (G.MapEntry) arg;
                    if(me.getKey() instanceof J.Literal && me.getValue() instanceof J.Literal) {
                        J.Literal pluginLiteral = (J.Literal) me.getKey();
                        J.Literal pluginIdLiteral = (J.Literal) me.getValue();
                        if("plugin".equals(pluginLiteral.getValue()) && pluginId.equals(pluginIdLiteral.getValue())) {
                            //noinspection DataFlowIssue
                            return null;
                        }
                    }
                }
            }
        }

        return m;
    }
}
