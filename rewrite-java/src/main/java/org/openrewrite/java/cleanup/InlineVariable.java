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
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class InlineVariable extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inline variable";
    }

    @Override
    public String getDescription() {
        return "Inline variables when they are immediately used to return or throw.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1488");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.of(2, ChronoUnit.MINUTES);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext executionContext) {
                J.Block bl = super.visitBlock(block, executionContext);
                List<Statement> statements = bl.getStatements();
                if (statements.size() > 1) {
                    String identReturned = identReturned(statements);
                    if (identReturned != null) {
                        if (statements.get(statements.size() - 2) instanceof J.VariableDeclarations) {
                            J.VariableDeclarations varDec = (J.VariableDeclarations) statements.get(statements.size() - 2);
                            J.VariableDeclarations.NamedVariable identDefinition = varDec.getVariables().get(0);
                            if (varDec.getLeadingAnnotations().isEmpty() && identDefinition.getSimpleName().equals(identReturned)) {
                                bl = bl.withStatements(ListUtils.map(statements, (i, statement) -> {
                                    if (i == statements.size() - 2) {
                                        return null;
                                    } else if (i == statements.size() - 1) {
                                        if (statement instanceof J.Return) {
                                            J.Return retrn = (J.Return) statement;
                                            return retrn.withExpression(requireNonNull(identDefinition.getInitializer())
                                                            .withPrefix(requireNonNull(retrn.getExpression()).getPrefix()))
                                                    .withPrefix(retrn.getPrefix().withComments(varDec.getComments()));
                                        } else if (statement instanceof J.Throw) {
                                            J.Throw thrown = (J.Throw) statement;
                                            return thrown.withException(requireNonNull(identDefinition.getInitializer())
                                                            .withPrefix(requireNonNull(thrown.getException()).getPrefix()))
                                                    .withPrefix(thrown.getPrefix().withComments(varDec.getComments()));
                                        }
                                    }
                                    return statement;
                                }));
                            }
                        }
                    }
                }
                return bl;
            }

            @Nullable
            private String identReturned(List<Statement> stats) {
                if (stats.get(stats.size() - 1) instanceof J.Return) {
                    J.Return retrn = (J.Return) stats.get(stats.size() - 1);
                    if (retrn.getExpression() instanceof J.Identifier) {
                        return ((J.Identifier) retrn.getExpression()).getSimpleName();
                    }
                } else if (stats.get(stats.size() - 1) instanceof J.Throw) {
                    J.Throw thr = (J.Throw) stats.get(stats.size() - 1);
                    if (thr.getException() instanceof J.Identifier) {
                        return ((J.Identifier) thr.getException()).getSimpleName();
                    }
                }
                return null;
            }
        };
    }
}
