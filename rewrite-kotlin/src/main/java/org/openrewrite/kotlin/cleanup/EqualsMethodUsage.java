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
package org.openrewrite.kotlin.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.kotlin.tree.K;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singleton;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class EqualsMethodUsage extends Recipe {

    @SuppressWarnings("ALL")
    private static J.@Nullable Binary equalsBinaryTemplate;

    @Override
    public String getDisplayName() {
        return "Structural equality tests should use `==` or `!=`";
    }

    @Override
    public String getDescription() {
        return "In Kotlin, `==` means structural equality and `!=` structural inequality and both map to the left-side " +
               "term’s `equals()` function. It is, therefore, redundant to call `equals()` as a function. Also, `==` and `!=` " +
               "are more general than `equals()` and `!equals()` because it allows either of both operands to be `null`.\n" +
               "Developers using `equals()` instead of `==` or `!=` is often the result of adapting styles from other " +
               "languages like Java, where `==` means reference equality and `!=` means reference inequality.\n" +
               "The `==` and `!=` operators are a more concise and elegant way to test structural equality than calling a function.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("RSPEC-S6519");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(3);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitUnary(J.Unary unary, ExecutionContext ctx) {
                unary = (J.Unary) super.visitUnary(unary, ctx);
                if (unary.getExpression() instanceof J.Binary &&
                    getCursor().pollMessage("replaced") != null) {
                    J.Binary binary = (J.Binary) unary.getExpression();
                    if (binary.getOperator() == J.Binary.Type.Equal) {
                        return binary.withOperator(J.Binary.Type.NotEqual);
                    }
                }
                return unary;
            }

            @Override
            public <T extends J> J visitParentheses(J.Parentheses<T> parens, ExecutionContext ctx) {
                J pa = super.visitParentheses(parens, ctx);
                if (pa instanceof J.Parentheses && getCursor().pollMessage("replaced") != null) {
                    getCursor().getParentTreeCursor().putMessage("replaced", true);
                    return ((J.Parentheses<?>) pa).getTree();
                }
                return pa;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method,
                                           ExecutionContext ctx) {
                method = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if ("equals".equals(method.getSimpleName()) &&
                    method.getMethodType() != null &&
                    method.getArguments().size() == 1 &&
                    TypeUtils.isOfClassType(method.getMethodType().getReturnType(), "kotlin.Boolean") &&
                    method.getSelect() != null
                ) {
                    Expression lhs = method.getSelect();
                    Expression rhs = method.getArguments().get(0);
                    Cursor parentCursor = getCursor().getParentTreeCursor();
                    parentCursor.putMessage("replaced", true);
                    J.Binary binary = buildEqualsBinary(lhs, rhs);
                    return parentCursor.getValue() instanceof J.Block ? new K.ExpressionStatement(randomId(), binary) : binary;
                }
                return method;
            }
        };
    }

    @SuppressWarnings("all")
    private static J.Binary buildEqualsBinary(Expression left, Expression right) {
        if (equalsBinaryTemplate == null) {
            K.CompilationUnit kcu = KotlinParser.builder().build()
                    .parse("fun method(a : String, b : String) {val isSame = a == b}")
                    .map(K.CompilationUnit.class::cast)
                    .findFirst()
                    .get();

            equalsBinaryTemplate = new KotlinVisitor<AtomicReference<J.Binary>>() {
                @Override
                public J visitBinary(J.Binary binary, AtomicReference<J.Binary> target) {
                    target.set(binary);
                    return binary;
                }
            }.reduce(kcu, new AtomicReference<J.Binary>()).get();
        }

        Space rhsPrefix = right.getPrefix();
        if (rhsPrefix.getWhitespace().isEmpty()) {
            rhsPrefix = rhsPrefix.withWhitespace(" ");
        }
        return equalsBinaryTemplate.withLeft(left.withPrefix(left.getPrefix())).withRight(right.withPrefix(rhsPrefix));
    }
}
