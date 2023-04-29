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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class ForLoopIncrementInUpdate extends Recipe {

    @Override
    public String getDisplayName() {
        return "`for` loop counters incremented in update";
    }

    @Override
    public String getDescription() {
        return "The increment should be moved to the loop's increment clause if possible.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1994");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(20);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitForLoop(J.ForLoop forLoop, ExecutionContext ctx) {
                Statement init = forLoop.getControl().getInit().get(0);
                if (init instanceof J.VariableDeclarations) {
                    J.VariableDeclarations initVars = (J.VariableDeclarations) init;

                    Statement body = forLoop.getBody();
                    Statement lastStatement;
                    if (body instanceof J.Block) {
                        List<Statement> statements = ((J.Block) body).getStatements();
                        if (statements.isEmpty()) {
                            return super.visitForLoop(forLoop, ctx);
                        }
                        lastStatement = statements.get(statements.size() - 1);
                    } else {
                        return super.visitForLoop(forLoop, ctx);
                    }

                    if (lastStatement instanceof J.Unary) {
                        J.Unary unary = (J.Unary) lastStatement;
                        if (unary.getExpression() instanceof J.Identifier) {
                            String unaryTarget = ((J.Identifier) unary.getExpression()).getSimpleName();
                            for (J.VariableDeclarations.NamedVariable initVar : initVars.getVariables()) {
                                if (initVar.getSimpleName().equals(unaryTarget)) {
                                    J.ForLoop f = forLoop.withControl(forLoop.getControl().withUpdate(ListUtils.insertInOrder(
                                            ListUtils.map(forLoop.getControl().getUpdate(), u -> u instanceof J.Empty ? null : u),
                                            unary.withPrefix(Space.format(" ")),
                                            Comparator.comparing(s -> s.printTrimmed(getCursor()), Comparator.naturalOrder())
                                    )));

                                    //noinspection ConstantConditions
                                    f = f.withBody((Statement) new JavaVisitor<ExecutionContext>() {
                                        @Nullable
                                        @Override
                                        public J visit(@Nullable Tree tree, ExecutionContext executionContext) {
                                            return tree == unary ? null : super.visit(tree, executionContext);
                                        }
                                    }.visit(f.getBody(), ctx));

                                    return f;
                                }
                            }
                        }
                    }
                }

                return super.visitForLoop(forLoop, ctx);
            }
        };
    }
}
