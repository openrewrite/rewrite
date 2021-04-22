/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class ModifierOrder extends Recipe {
    @Override
    public String getDisplayName() {
        return "Modifier order";
    }

    @Override
    public String getDescription() {
        return "Modifiers should be declared in the correct order as recommended by the JLS.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, executionContext);
                return c.withModifiers(sortModifiers(c.getModifiers()));
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, executionContext);
                return m.withModifiers(sortModifiers(m.getModifiers()));
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
                J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, executionContext);
                return v.withModifiers(sortModifiers(v.getModifiers()));
            }

            private List<J.Modifier> sortModifiers(List<J.Modifier> modifiers) {
                List<J.Modifier.Type> sortedTypes = modifiers.stream()
                        .map(J.Modifier::getType)
                        .sorted(Comparator.comparingInt(J.Modifier.Type::ordinal))
                        .collect(toList());

                return ListUtils.map(modifiers, (i, mod) -> mod.getType() == sortedTypes.get(i) ? mod : mod.withType(sortedTypes.get(i)));
            }
        };
    }
}
