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

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaVarKeyword;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class UseDiamondOperator extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use diamond operator";
    }

    @Override
    public String getDescription() {
        return "The diamond operator (`<>`) should be used. Java 7 introduced the diamond operator (<>) to reduce the verbosity of generics code. For instance, instead of having to declare a List's type in both its declaration and its constructor, you can now simplify the constructor declaration with `<>`, and the compiler will infer the type.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2293");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {

        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
                J.NewClass n = super.visitNewClass(newClass, executionContext);
                if (n.getClazz() instanceof J.ParameterizedType && n.getBody() == null) {
                    J.ParameterizedType parameterizedType = (J.ParameterizedType) n.getClazz();
                    if (useDiamondOperator(newClass, parameterizedType)) {
                        n = n.withClazz(parameterizedType.withTypeParameters(singletonList(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY))));
                        if (parameterizedType.getTypeParameters() != null) {
                            parameterizedType.getTypeParameters().stream()
                                    .map(e -> TypeUtils.asFullyQualified(e.getType()))
                                    .forEach(this::maybeRemoveImport);
                        }
                    }
                }
                return n;
            }

            private boolean useDiamondOperator(J.NewClass newClass, J.ParameterizedType parameterizedType) {
                if (parameterizedType.getTypeParameters() == null || parameterizedType.getTypeParameters().isEmpty()
                        || parameterizedType.getTypeParameters().get(0) instanceof J.Empty) {
                    return false;
                }

                Cursor c = getCursor().dropParentUntil(J.class::isInstance);

                if (c.getValue() instanceof J.VariableDeclarations.NamedVariable) {
                    //If the immediate parent is named variable, check the variable declaration to make sure it's
                    //not using local variable type inference.
                    J.VariableDeclarations variableDeclaration = c.firstEnclosing(J.VariableDeclarations.class);
                    return variableDeclaration != null && (variableDeclaration.getTypeExpression() == null ||
                            !variableDeclaration.getTypeExpression().getMarkers().findFirst(JavaVarKeyword.class).isPresent());
                } else if (c.getValue() instanceof J.MethodInvocation) {
                    //Do not remove the type parameters if the newClass is the receiver of a method invocation.
                    J.MethodInvocation invocation = c.getValue();
                    return invocation.getSelect() != newClass;
                } else {
                    //If the immediate parent is a block, this new operation is a statement and there is no left side to
                    //infer the type parameters.
                    return !(c.getValue() instanceof J.Block);
                }
            }
        };
    }
}
