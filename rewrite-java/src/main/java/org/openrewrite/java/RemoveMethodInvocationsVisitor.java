/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;

import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.openrewrite.Tree.randomId;

/**
 * This visitor removes method calls matching some criteria.
 * Tries to intelligently remove within chains without breaking other methods in the chain.
 */
public class RemoveMethodInvocationsVisitor extends JavaVisitor<ExecutionContext> {
    private final Map<MethodMatcher, Predicate<List<Expression>>> matchers;

    public RemoveMethodInvocationsVisitor(Map<MethodMatcher, Predicate<List<Expression>>> matchers) {
        this.matchers = matchers;
    }

    public RemoveMethodInvocationsVisitor(List<String> methodSignatures) {
        this(methodSignatures.stream().collect(toMap(
            MethodMatcher::new,
            signature -> args -> true
        )));
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

        if (inMethodCallChain()) {
            List<Expression> newArgs = ListUtils.map(m.getArguments(), arg -> (Expression) this.visit(arg, ctx));
            return m.withArguments(newArgs);
        }

        J j = removeMethods(m, 0, isLambdaBody(), new Stack<>());
        if (j != null) {
            j = j.withPrefix(m.getPrefix());
            // There should always be
            if (!m.getArguments().isEmpty() && m.getArguments().stream().allMatch(ToBeRemoved::hasMarker)) {
                return ToBeRemoved.withMarker(j);
            }
        }

        //noinspection DataFlowIssue allow returning null to remove the element
        return j;
    }

    private @Nullable J removeMethods(@Nullable Expression expression, int depth, boolean isLambdaBody, Stack<Space> selectAfter) {
        if (!(expression instanceof J.MethodInvocation) || ((J.MethodInvocation) expression).getMethodType() == null) {
            return expression;
        }

        J.MethodInvocation m = (J.MethodInvocation) expression;
        boolean isStatic = m.getMethodType().hasFlags(Flag.Static);

        if (isStatic && !isStatementInParentBlock(m)) {
            return expression;
        }

        boolean isStatement = isStatement();

        if (matchers.entrySet().stream().anyMatch(entry -> matches(m, entry.getKey(), entry.getValue()))) {
            boolean hasSameReturnType = m.getSelect() != null && TypeUtils.isAssignableTo(m.getMethodType().getReturnType(), m.getSelect().getType());
            boolean removable = ((isStatement || isStatic) && depth == 0) || hasSameReturnType;
            if (!removable) {
                return expression;
            }

            maybeRemoveImport(m.getMethodType().getDeclaringType());

            if (m.getSelect() == null) {
                return null;
            } else if (m.getSelect() instanceof J.Identifier || m.getSelect() instanceof J.NewClass) {
                boolean keepSelect = depth != 0;
                if (keepSelect) {
                    selectAfter.add(getSelectAfter(m));
                    return m.getSelect();
                } else {
                    if (isStatement) {
                        return null;
                    } else if (isLambdaBody) {
                        return ToBeRemoved.withMarker(J.Block.createEmptyBlock());
                    } else {
                        return m.getSelect();
                    }
                }
            } else if (m.getSelect() instanceof J.MethodInvocation) {
                return removeMethods(m.getSelect(), depth, isLambdaBody, selectAfter);
            }
        }

        J.MethodInvocation method = m.withSelect((Expression) removeMethods(m.getSelect(), depth + 1, isLambdaBody, selectAfter));

        // inherit prefix
        if (!selectAfter.isEmpty()) {
            method = inheritSelectAfter(method, selectAfter);
        }

        return method;
    }

    private boolean matches(J.MethodInvocation m, MethodMatcher matcher, Predicate<List<Expression>> argsMatches) {
        return matcher.matches(m) && argsMatches.test(m.getArguments());
    }

    private boolean isStatement() {
        return getCursor().dropParentUntil(p -> p instanceof J.Block ||
                                                p instanceof J.Assignment ||
                                                p instanceof J.VariableDeclarations.NamedVariable ||
                                                p instanceof J.Return ||
                                                p instanceof JContainer ||
                                                p == Cursor.ROOT_VALUE
        ).getValue() instanceof J.Block;
    }

    private boolean isStatementInParentBlock(Statement method) {
        J.Block parentBlock = getCursor().firstEnclosing(J.Block.class);
        return parentBlock == null || parentBlock.getStatements().contains(method);
    }

    private boolean isLambdaBody() {
        if (getCursor().getParent() == null) {
            return false;
        }
        Object parent = getCursor().getParent().getValue();
        return parent instanceof J.Lambda && ((J.Lambda) parent).getBody() == getCursor().getValue();
    }

    private boolean inMethodCallChain() {
        return getCursor().dropParentUntil(p -> !(p instanceof JRightPadded)).getValue() instanceof J.MethodInvocation;
    }

    private J.MethodInvocation inheritSelectAfter(J.MethodInvocation method, Stack<Space> prefix) {
        return (J.MethodInvocation) new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right,
                    JRightPadded.Location loc,
                    ExecutionContext executionContext) {
                if (right == null) return null;
                return prefix.isEmpty() ? right : right.withAfter(prefix.pop());
            }
        }.visitNonNull(method, new InMemoryExecutionContext());
    }

    private Space getSelectAfter(J.MethodInvocation method) {
        return new JavaIsoVisitor<List<Space>>() {
            @Override
            public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right,
                    JRightPadded.Location loc,
                    List<Space> selectAfter) {
                if (selectAfter.isEmpty()) {
                    selectAfter.add(right == null ? Space.EMPTY : right.getAfter());
                }
                return right;
            }
        }.reduce(method, new ArrayList<>()).get(0);
    }

    @SuppressWarnings("unused") // used in rewrite-spring / convenient for consumers
    public static Predicate<List<Expression>> isTrueArgument() {
        return args -> args.size() == 1 && isTrue(args.get(0));
    }

    @SuppressWarnings("unused") // used in rewrite-spring / convenient for consumers
    public static Predicate<List<Expression>> isFalseArgument() {
        return args -> args.size() == 1 && isFalse(args.get(0));
    }

    public static boolean isTrue(Expression expression) {
        return isBoolean(expression, Boolean.TRUE);
    }

    public static boolean isFalse(Expression expression) {
        return isBoolean(expression, Boolean.FALSE);
    }

    private static boolean isBoolean(Expression expression, Boolean b) {
        if (expression instanceof J.Literal) {
            return expression.getType() == JavaType.Primitive.Boolean && b.equals(((J.Literal) expression).getValue());
        }
        return false;
    }

    @Override
    public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext ctx) {
        lambda = (J.Lambda) super.visitLambda(lambda, ctx);
        J body = lambda.getBody();
        if (body instanceof J.MethodInvocation && ToBeRemoved.hasMarker(body)) {
            Expression select = ((J.MethodInvocation) body).getSelect();
            List<J> parameters = lambda.getParameters().getParameters();
            if (select instanceof J.Identifier && !parameters.isEmpty() && parameters.get(0) instanceof J.VariableDeclarations) {
                J.VariableDeclarations declarations = (J.VariableDeclarations) parameters.get(0);
                if (((J.Identifier) select).getSimpleName().equals(declarations.getVariables().get(0).getSimpleName())) {
                    return ToBeRemoved.withMarker(lambda);
                }
            } else if (select instanceof J.MethodInvocation) {
                return lambda.withBody(select.withPrefix(body.getPrefix()));
            }
        } else if (body instanceof J.Block && ToBeRemoved.hasMarker(body)) {
            return ToBeRemoved.withMarker(lambda.withBody(ToBeRemoved.removeMarker(body)));
        }
        return lambda;
    }

    @Override
    public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
        int statementsCount = block.getStatements().size();

        block = (J.Block) super.visitBlock(block, ctx);
        List<Statement> statements = block.getStatements();
        if (!statements.isEmpty() && statements.stream().allMatch(ToBeRemoved::hasMarker)) {
            return ToBeRemoved.withMarker(block.withStatements(emptyList()));
        }

        if (statementsCount > 0 && statements.isEmpty()) {
            return ToBeRemoved.withMarker(block.withStatements(emptyList()));
        }

        if (statements.stream().anyMatch(ToBeRemoved::hasMarker)) {
            //noinspection DataFlowIssue
            return block.withStatements(statements.stream()
                .filter(s -> !ToBeRemoved.hasMarker(s) || s instanceof J.MethodInvocation && ((J.MethodInvocation) s).getSelect() instanceof J.MethodInvocation)
                .map(s -> s instanceof J.MethodInvocation && ToBeRemoved.hasMarker(s) ? ((J.MethodInvocation) s).getSelect().withPrefix(s.getPrefix()) : s)
                .collect(toList()));
        }
        return block;
    }

    @Value
    @With
    private static class ToBeRemoved implements Marker {
        UUID id;
        static <J2 extends J> J2 withMarker(J2 j) {
            return j.withMarkers(j.getMarkers().addIfAbsent(new ToBeRemoved(randomId())));
        }
        static <J2 extends J> J2 removeMarker(J2 j) {
            return j.withMarkers(j.getMarkers().removeByType(ToBeRemoved.class));
        }
        static boolean hasMarker(J j) {
            return j.getMarkers().findFirst(ToBeRemoved.class).isPresent();
        }
    }
}
