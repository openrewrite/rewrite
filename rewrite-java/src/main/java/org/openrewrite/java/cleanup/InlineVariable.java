/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class InlineVariable extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inline variable";
    }

    @Override
    public String getDescription() {
        return "Inline variables when they are immediately used to return.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                J.Block body = m.getBody();
                if (body != null) {
                    List<Statement> stats = body.getStatements();
                    if (stats.size() > 1) {
                        String identReturned = identReturned(stats);
                        if (identReturned != null) {
                            if (stats.get(stats.size() - 2) instanceof J.VariableDeclarations) {
                                J.VariableDeclarations varDec = (J.VariableDeclarations) stats.get(stats.size() - 2);
                                J.VariableDeclarations.NamedVariable identDefinition = varDec.getVariables().get(0);
                                if (identDefinition.getSimpleName().equals(identReturned)) {
                                    m = m.withBody(body.withStatements(ListUtils.map(stats, (i, stat) -> {
                                        if (i == stats.size() - 2) {
                                            return null;
                                        } else if (i == stats.size() - 1) {
                                            J.Return retrn = (J.Return) stat;
                                            return retrn.withExpression(requireNonNull(identDefinition.getInitializer())
                                                    .withPrefix(requireNonNull(retrn.getExpression()).getPrefix()));
                                        }
                                        return stat;
                                    })));
                                }
                            }
                        }
                    }
                }
                return m;
            }

            @Nullable
            private String identReturned(List<Statement> stats) {
                if (stats.get(stats.size() - 1) instanceof J.Return) {
                    J.Return retrn = (J.Return) stats.get(stats.size() - 1);
                    if (retrn.getExpression() instanceof J.Identifier) {
                        return ((J.Identifier) retrn.getExpression()).getSimpleName();
                    }
                }
                return null;
            }
        };
    }
}
