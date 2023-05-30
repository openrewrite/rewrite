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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.DeleteStatement;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class SimplifyBooleanReturn extends Recipe {

    @Override
    public String getDisplayName() {
        return "Simplify boolean return";
    }

    @Override
    public String getDescription() {
        return "Simplifies Boolean expressions by removing redundancies, e.g.: `a && true` simplifies to `a`.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1126");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final JavaTemplate notIfConditionReturn = JavaTemplate.builder("return !(#{any(boolean)});")
                    .context(this::getCursor)
                    .build();

            @Override
            public J visitIf(J.If iff, ExecutionContext ctx) {
                J.If i = visitAndCast(iff, ctx, super::visitIf);

                Cursor parent = getCursor().getParentTreeCursor();

                if (parent.getValue() instanceof J.Block &&
                    parent.getParentOrThrow().getValue() instanceof J.MethodDeclaration &&
                    thenHasOnlyReturnStatement(iff) &&
                    elseWithOnlyReturn(i)) {
                    List<Statement> followingStatements = followingStatements();
                    Optional<Expression> singleFollowingStatement = Optional.ofNullable(followingStatements.isEmpty() ? null : followingStatements.get(0))
                            .flatMap(stat -> Optional.ofNullable(stat instanceof J.Return ? (J.Return) stat : null))
                            .map(J.Return::getExpression);

                    if (followingStatements.isEmpty() || singleFollowingStatement.map(r -> isLiteralFalse(r) || isLiteralTrue(r)).orElse(false)) {
                        J.Return retrn = getReturnIfOnlyStatementInThen(iff).orElse(null);
                        assert retrn != null;

                        Expression ifCondition = i.getIfCondition().getTree();

                        if (isLiteralTrue(retrn.getExpression())) {
                            if (singleFollowingStatement.map(this::isLiteralFalse).orElse(false) && i.getElsePart() == null) {
                                doAfterVisit(new DeleteStatement<>(followingStatements().get(0)));
                                return maybeAutoFormat(retrn, retrn.withExpression(ifCondition), ctx, parent);
                            } else if (!singleFollowingStatement.isPresent() &&
                                       getReturnExprIfOnlyStatementInElseThen(i).map(this::isLiteralFalse).orElse(false)) {
                                if (i.getElsePart() != null) {
                                    doAfterVisit(new DeleteStatement<>(i.getElsePart().getBody()));
                                }
                                return maybeAutoFormat(retrn, retrn.withExpression(ifCondition), ctx, parent);
                            }
                        } else if (isLiteralFalse(retrn.getExpression())) {
                            boolean returnThenPart = false;

                            if (singleFollowingStatement.map(this::isLiteralTrue).orElse(false) && i.getElsePart() == null) {
                                doAfterVisit(new DeleteStatement<>(followingStatements().get(0)));
                                returnThenPart = true;
                            } else if (!singleFollowingStatement.isPresent() && getReturnExprIfOnlyStatementInElseThen(i)
                                    .map(this::isLiteralTrue).orElse(false)) {
                                if (i.getElsePart() != null) {
                                    doAfterVisit(new DeleteStatement<>(i.getElsePart().getBody()));
                                }
                                returnThenPart = true;
                            }

                            if (returnThenPart) {
                                // we need to NOT the expression inside the if condition
                                return i.withTemplate(notIfConditionReturn, getCursor(), i.getCoordinates().replace(), ifCondition);
                            }
                        }
                    }
                }

                return i;
            }

            private boolean elseWithOnlyReturn(J.If i) {
                return i.getElsePart() == null || !(i.getElsePart().getBody() instanceof J.If);
            }

            private boolean thenHasOnlyReturnStatement(J.If iff) {
                return getReturnIfOnlyStatementInThen(iff)
                        .map(retrn -> isLiteralFalse(retrn.getExpression()) || isLiteralTrue(retrn.getExpression()))
                        .orElse(false);
            }

            private List<Statement> followingStatements() {
                J.Block block = getCursor().getParentTreeCursor().getValue();
                AtomicBoolean dropWhile = new AtomicBoolean(false);
                return block.getStatements().stream()
                        .filter(s -> {
                            dropWhile.set(dropWhile.get() || s == getCursor().getValue());
                            return dropWhile.get();
                        })
                        .skip(1)
                        .collect(Collectors.toList());
            }

            private boolean isLiteralTrue(@Nullable J tree) {
                return tree instanceof J.Literal && ((J.Literal) tree).getValue() == Boolean.valueOf(true);
            }

            private boolean isLiteralFalse(@Nullable J tree) {
                return tree instanceof J.Literal && ((J.Literal) tree).getValue() == Boolean.valueOf(false);
            }

            private Optional<J.Return> getReturnIfOnlyStatementInThen(J.If iff) {
                if (iff.getThenPart() instanceof J.Return) {
                    return Optional.of((J.Return) iff.getThenPart());
                }
                if (iff.getThenPart() instanceof J.Block) {
                    J.Block then = (J.Block) iff.getThenPart();
                    if (then.getStatements().size() == 1 && then.getStatements().get(0) instanceof J.Return) {
                        return Optional.of((J.Return) then.getStatements().get(0));
                    }
                }
                return Optional.empty();
            }

            private Optional<Expression> getReturnExprIfOnlyStatementInElseThen(J.If iff2) {
                if (iff2.getElsePart() == null) {
                    return Optional.empty();
                }

                Statement elze = iff2.getElsePart().getBody();
                if (elze instanceof J.Return) {
                    return Optional.ofNullable(((J.Return) elze).getExpression());
                }

                if (elze instanceof J.Block) {
                    List<Statement> statements = ((J.Block) elze).getStatements();
                    if (statements.size() == 1) {
                        J statement = statements.get(0);
                        if (statement instanceof J.Return) {
                            return Optional.ofNullable(((J.Return) statement).getExpression());
                        }
                    }
                }

                return Optional.empty();
            }
        };
    }

    @Override
    public boolean causesAnotherCycle() {
        return true;
    }
}
