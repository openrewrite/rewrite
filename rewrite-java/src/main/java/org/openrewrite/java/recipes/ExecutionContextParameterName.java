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
package org.openrewrite.java.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RenameVariable;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

public class ExecutionContextParameterName extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use a standard name for `ExecutionContext`";
    }

    @Override
    public String getDescription() {
        return "Visitors that are parameterized with `ExecutionContext` should use the parameter name `ctx`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.openrewrite.Recipe", false), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                for (Statement parameter : m.getParameters()) {
                    if (parameter instanceof J.VariableDeclarations) {
                        J.VariableDeclarations param = (J.VariableDeclarations) parameter;
                        if (TypeUtils.isOfClassType(param.getType(),
                                "org.openrewrite.ExecutionContext")) {
                            m = (J.MethodDeclaration) new RenameVariable<ExecutionContext>(param.getVariables().get(0), "ctx")
                                    .visitNonNull(m, ctx);
                        }
                    }
                }

                return m;
            }
        });
    }
}
