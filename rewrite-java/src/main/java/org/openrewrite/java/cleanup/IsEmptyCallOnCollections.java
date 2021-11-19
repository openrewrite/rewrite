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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class IsEmptyCallOnCollections extends Recipe {
    private static final MethodMatcher COLLECTION_SIZE = new MethodMatcher("java.util.Collection size()", true);

    @Override
    public String getDisplayName() {
        return "Use `Collections#isEmpty()` instead of comparing `size()`";
    }

    @Override
    public String getDescription() {
        return "Also check for _not_ `isEmpty()` when testing for not equal to zero size.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-3981");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(COLLECTION_SIZE);
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            final JavaTemplate isEmpty = JavaTemplate.builder(this::getCursor, "#{}#{any(java.util.Collection)}.isEmpty()")
                    .build();

            @Override
            public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                if (binary.getOperator() == J.Binary.Type.Equal || binary.getOperator() == J.Binary.Type.NotEqual) {
                    if (COLLECTION_SIZE.matches(binary.getLeft()) || COLLECTION_SIZE.matches(binary.getRight())) {
                        if (isZero(binary.getLeft()) || isZero(binary.getRight())) {
                            J.MethodInvocation sizeCall = (J.MethodInvocation) (COLLECTION_SIZE.matches(binary.getLeft()) ?
                                    binary.getLeft() : binary.getRight());
                            return sizeCall.withTemplate(isEmpty, sizeCall.getCoordinates().replace(),
                                            binary.getOperator() == J.Binary.Type.Equal ? "" : "!",
                                            sizeCall.getSelect())
                                    .withPrefix(binary.getPrefix());
                        }
                    }
                }
                return super.visitBinary(binary, ctx);
            }
        };
    }

    private static boolean isZero(Expression expression) {
        return expression instanceof J.Literal && Integer.valueOf(0).equals(((J.Literal) expression).getValue());
    }
}
