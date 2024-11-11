/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveSettingsPluginManagement extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove Gradle settings pluginManagement";
    }

    @Override
    public String getDescription() {
        return "Remove Gradle settings pluginManagement from `settings.gradle(.kts)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsSettingsGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                G.CompilationUnit g = super.visitCompilationUnit(cu, ctx);
                return g.withStatements(ListUtils.map(g.getStatements(),
                        s -> s instanceof J.MethodInvocation &&
                             "pluginManagement".equals(((J.MethodInvocation) s).getName().getSimpleName()) ?
                                null : s));
            }
        });
    }
}
