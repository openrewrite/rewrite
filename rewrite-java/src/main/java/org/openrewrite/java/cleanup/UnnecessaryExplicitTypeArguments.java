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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;

public class UnnecessaryExplicitTypeArguments extends Recipe {

    @Override
    public String getDisplayName() {
        return "Unnecessary explicit type arguments";
    }

    @Override
    public String getDescription() {
        return "When explicit type arguments are inferrable by the compiler, they may be removed.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (m.getMethodType() != null) {
                    Object enclosing = getCursor().getParentTreeCursor().getValue();
                    JavaType enclosingType = null;

                    if(enclosing instanceof J.MethodInvocation) {
                        // Cannot remove type parameters if it would introduce ambiguity about which method should be called
                        J.MethodInvocation enclosingMethod = (J.MethodInvocation) enclosing;
                        if(enclosingMethod.getMethodType() == null) {
                            return m;
                        }
                        if(!(enclosingMethod.getMethodType().getDeclaringType() instanceof JavaType.Class)) {
                            return m;
                        }
                        JavaType.Class declaringClass = (JavaType.Class) enclosingMethod.getMethodType().getDeclaringType();
                        // If there's another method on the class with the same name, skip removing type parameters
                        // More nuanced detection of ambiguity introduction is possible
                        if(declaringClass.getMethods().stream()
                                .filter(it -> it.getName().equals(enclosingMethod.getSimpleName()))
                                .count() > 1) {
                            return m;
                        }
                        enclosingType = enclosingMethod.getType();
                    } else if (enclosing instanceof Expression) {
                        enclosingType = ((Expression) enclosing).getType();
                    } else if (enclosing instanceof NameTree) {
                        if (enclosing instanceof J.VariableDeclarations.NamedVariable) {
                            J.VariableDeclarations decl = getCursor().getParentTreeCursor().getParentTreeCursor().getValue();
                            if (decl.getTypeExpression() instanceof J.Identifier && "var".equals(((J.Identifier) decl.getTypeExpression()).getSimpleName())) {
                                return m;
                            }
                        }
                        enclosingType = ((NameTree) enclosing).getType();
                    } else if (enclosing instanceof J.Return) {
                        Object e = getCursor().dropParentUntil(p -> p instanceof J.MethodDeclaration || p instanceof J.Lambda).getValue();
                        if (e instanceof J.MethodDeclaration) {
                            J.MethodDeclaration methodDeclaration = (J.MethodDeclaration) e;
                            if (methodDeclaration.getReturnTypeExpression() != null) {
                                enclosingType = methodDeclaration.getReturnTypeExpression().getType();
                            }
                        } else if (e instanceof J.Lambda) {
                            enclosingType = ((J.Lambda) e).getType();
                        }
                    }

                    if (enclosingType != null && enclosingType.equals(m.getMethodType().getReturnType())) {
                        m = m.withTypeParameters(null);
                    }
                }

                return m;
            }
        };
    }
}
