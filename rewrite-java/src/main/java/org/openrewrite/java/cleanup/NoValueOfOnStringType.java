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
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class NoValueOfOnStringType extends Recipe {
    private static final MethodMatcher VALUE_OF = new MethodMatcher("java.lang.String valueOf(..)");

    @Override
    public String getDisplayName() {
        return "Unnecessary String#valueOf(..)";
    }

    @Override
    public String getDescription() {
        return "Replace unnecessary `String#valueOf(..)` method invocations with the argument directly. " +
               "This occurs when the argument to `String#valueOf(arg)` is a string literal, such as `String.valueOf(\"example\")`. " +
               "Or, when the `String#valueOf(..)` invocation is used in a concatenation, such as `\"example\" + String.valueOf(\"example\")`.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1153");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(4);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(new MethodMatcher("java.lang.String valueOf(..)")), new JavaVisitor<ExecutionContext>() {
            private final JavaTemplate t = JavaTemplate.builder(this::getCursor, "#{any(java.lang.String)}").build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (VALUE_OF.matches(method.getSelect())) {
                    return method;
                }

                J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (VALUE_OF.matches(mi) && mi.getArguments().size() == 1) {
                    Expression argument = mi.getArguments().get(0);

                    if (TypeUtils.isString(argument.getType()) || removeValueOfFromBinaryExpression(argument)) {
                        return mi.withTemplate(t, mi.getCoordinates().replace(), argument);
                    }
                }
                return mi;
            }

            /**
             * If the String#valueOf method is within a binary expression and the argument is a primitive, the valueOf
             * can be removed if the binary expression's type is a String.
             *
             * @param argument The argument of the valueOf method.
             * @return True if the method can be removed.
             */
            private boolean removeValueOfFromBinaryExpression(Expression argument) {

                if (TypeUtils.asPrimitive(argument.getType()) != null) {
                    J parent = getCursor().getParent() != null ? getCursor().getParent().firstEnclosing(J.class) : null;
                    if (parent instanceof J.Binary) {
                        J.Binary b = (J.Binary) parent;
                        JavaType otherType = b.getRight() == getCursor().getValue() ? b.getLeft().getType() : b.getRight().getType();
                        return TypeUtils.isString(otherType) && b.getOperator() == J.Binary.Type.Addition;
                    }
                }
                return false;
            }
        });
    }
}
