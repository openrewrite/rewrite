/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.csharp.rpc;

import org.openrewrite.ExecutionContext;
import org.openrewrite.csharp.CSharpVisitor;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

/** Checks descriptors after their native producer sends them to Java. */
public class ConstructorProbe extends CSharpVisitor<ExecutionContext> {
    private int constructors;

    @Override
    public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
        if (method.getReturnTypeExpression() == null && !method.getSimpleName().startsWith("~")) {
            var type = method.getMethodType();
            if (type == null || method.getName().getType() != type || type.getDeclaringType() == null) {
                throw new IllegalStateException("Missing constructor descriptor: " + method.getSimpleName());
            }
            for (var statement : method.getParameters()) {
                if (statement instanceof J.VariableDeclarations parameter) {
                    for (var variable : parameter.getVariables()) {
                        if (variable.getVariableType() == null || variable.getVariableType().getOwner() != type) {
                            throw new IllegalStateException("Missing constructor parameter owner");
                        }
                    }
                }
            }
            constructors++;
        }
        return super.visitMethodDeclaration(method, ctx);
    }

    @Override
    public J visitCompilationUnit(Cs.CompilationUnit cu, ExecutionContext ctx) {
        cu = (Cs.CompilationUnit) super.visitCompilationUnit(cu, ctx);
        return SearchResult.found(cu, "Attributed constructors: " + constructors);
    }
}
