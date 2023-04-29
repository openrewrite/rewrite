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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

import java.util.*;

import static org.openrewrite.java.format.TabsAndIndents.formatTabsAndIndents;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseAsBuilder extends Recipe {
    @Option
    String builderType;

    @Option(
            displayName = "Immutable state",
            description = "The builder is immutable if you must assign the result of calls to intermediate variables " +
                          "or use directly. Defaults to true as many purpose-built builders will be immutable.",
            required = false
    )
    @Nullable
    Boolean immutable;

    @Option(
            displayName = "Builder creator method",
            description = "The method that creates the builder instance, which may not be a method of the builder itself.",
            required = false
    )
    @Nullable
    String builderCreator;

    @Override
    public String getDisplayName() {
        return "Use the builder pattern where possible";
    }

    @Override
    public String getDescription() {
        return "When an API has been designed as a builder, use it that way rather than as a series of setter calls.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaIsoVisitor<ExecutionContext> v = new JavaIsoVisitor<ExecutionContext>() {
            final MethodMatcher builderCall = new MethodMatcher(builderType + " *(..)");

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);

                Map<String, List<Statement>> builderCalls = collectBuilderMethodsByVariable(b);

                if (builderCalls.values().stream().map(List::size).max(Integer::sum).orElse(0) > 1) {
                    List<Statement> statements = consolidateAllBuilderCalls(block, builderCalls);
                    return b.withStatements(statements);
                }

                return b;
            }

            private List<Statement> consolidateAllBuilderCalls(J.Block block, Map<String, List<Statement>> builderCalls) {
                List<Statement> statements = new ArrayList<>();

                // intermediate statements between builder methods that should move ahead of the builder
                List<Statement> beforeBuilder = new ArrayList<>();
                List<Statement> afterBuilder = new ArrayList<>();

                J.VariableDeclarations consolidatedBuilder = null;

                Iterator<Statement> builderCallIter = builderCalls.values()
                        .stream().flatMap(List::stream).iterator();
                Statement currentBuilderCall = builderCallIter.next();

                for (Statement statement : block.getStatements()) {
                    if (currentBuilderCall != null) {
                        if (statement == currentBuilderCall) {
                            if (statement instanceof J.VariableDeclarations) {
                                if (consolidatedBuilder != null) {
                                    statements.addAll(beforeBuilder);
                                    statements.add(consolidatedBuilder);
                                    statements.addAll(afterBuilder);

                                    beforeBuilder.clear();
                                    afterBuilder.clear();
                                }
                                consolidatedBuilder = (J.VariableDeclarations) statement;
                            } else {
                                assert consolidatedBuilder != null;
                                if (statement instanceof J.Assignment) {
                                    J.Assignment assign = (J.Assignment) statement;
                                    consolidatedBuilder = consolidateBuilder(consolidatedBuilder,
                                            (J.MethodInvocation) assign.getAssignment());
                                } else if (statement instanceof J.MethodInvocation) {
                                    consolidatedBuilder = consolidateBuilder(consolidatedBuilder,
                                            (J.MethodInvocation) statement);
                                }

                                beforeBuilder.addAll(afterBuilder);
                                afterBuilder.clear();
                            }
                            currentBuilderCall = builderCallIter.hasNext() ?
                                    builderCallIter.next() : null;
                        } else {
                            // we consider it to be "after" the builder until we can prove
                            // that there is a subsequent builder call, at which point it shifts
                            // to the "before" builder list.
                            afterBuilder.add(statement);
                        }
                    } else {
                        afterBuilder.add(statement);
                    }
                }

                statements.addAll(beforeBuilder);
                if (consolidatedBuilder != null) {
                    statements.add(consolidatedBuilder);
                }
                statements.addAll(afterBuilder);
                return statements;
            }

            private Map<String, List<Statement>> collectBuilderMethodsByVariable(J.Block b) {
                Map<String, List<Statement>> builderCalls = new LinkedHashMap<>();
                for (Statement stat : b.getStatements()) {
                    if (stat instanceof J.VariableDeclarations) {
                        J.VariableDeclarations varDecs = (J.VariableDeclarations) stat;
                        for (J.VariableDeclarations.NamedVariable namedVar : varDecs.getVariables()) {
                            if (matchesBuilder(namedVar.getInitializer())) {
                                builderCalls.computeIfAbsent(namedVar.getSimpleName(), n -> new ArrayList<>())
                                        .add(stat);
                            }
                        }
                    } else if (stat instanceof J.Assignment) {
                        J.Assignment assign = (J.Assignment) stat;
                        if (matchesBuilder(assign.getAssignment())) {
                            builderCalls.computeIfAbsent(assign.getVariable().printTrimmed(getCursor()),
                                    n -> new ArrayList<>()).add(stat);
                        }
                    } else if (!Boolean.FALSE.equals(immutable)) {
                        if (stat instanceof J.MethodInvocation) {
                            J.MethodInvocation method = (J.MethodInvocation) stat;
                            if (matchesBuilder(method) && method.getSelect() != null) {
                                builderCalls.computeIfAbsent(method.getSelect().printTrimmed(getCursor()),
                                        n -> new ArrayList<>()).add(stat);
                            }
                        }
                    }
                }
                return builderCalls;
            }

            private boolean matchesBuilder(@Nullable Expression j) {
                return builderCall.matches(j) || (builderCreator != null &&
                                                  new MethodMatcher(builderCreator).matches(j));
            }

            private J.VariableDeclarations consolidateBuilder(J.VariableDeclarations consolidatedBuilder,
                                                              J.MethodInvocation builderCall) {
                J.VariableDeclarations cb = consolidatedBuilder.withVariables(
                        ListUtils.map(consolidatedBuilder.getVariables(), nv -> {
                            Expression init = nv.getInitializer();
                            assert init != null;
                            return nv
                                    .withInitializer(builderCall
                                            .getPadding()
                                            .withSelect(JRightPadded
                                                    .build((Expression) init.withPrefix(Space.EMPTY))
                                                    .withAfter(Space.format("\n")))
                                    );
                        })
                );
                cb = formatTabsAndIndents(cb, getCursor());
                return cb;
            }
        };

        return builderCreator == null ? v : Preconditions.check(new UsesMethod<>(builderCreator), v);
    }
}
