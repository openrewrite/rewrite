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

import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class IndexOfChecksShouldUseAStartPosition extends Recipe {
    private static final MethodMatcher STRING_INDEX_MATCHER = new MethodMatcher("java.lang.String indexOf(String)");

    @Override
    public String getDisplayName() {
        return "Use `indexOf(String, int)`";
    }

    @Override
    public String getDescription() {
        return "Replaces `indexOf(String)` in binary operations if the compared value is an int and not less than 1.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2912");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(STRING_INDEX_MATCHER), new JavaIsoVisitor<ExecutionContext>() {

            private boolean isValueNotCompliant(J.Literal literal) {
                return !(literal.getValue() instanceof Integer && ((Integer) (literal.getValue()) <= 0));
            }

            @Override
            public J.Binary visitBinary(J.Binary binary, ExecutionContext ctx) {
                J.Binary b = super.visitBinary(binary, ctx);
                if (b.getLeft() instanceof J.MethodInvocation && STRING_INDEX_MATCHER.matches(b.getLeft()) &&
                    b.getOperator() == J.Binary.Type.GreaterThan &&
                    b.getRight() instanceof J.Literal && isValueNotCompliant((J.Literal) b.getRight())) {

                    J.MethodInvocation m = (J.MethodInvocation) b.getLeft();
                    b = b.withLeft(m.withTemplate(JavaTemplate.builder(this::getCursor, "#{any(java.lang.String)}, #{any(int)}").build(),
                            m.getCoordinates().replaceArguments(),
                            m.getArguments().get(0),
                            b.getRight()));

                    b = b.withRight(new J.Literal(
                            Tree.randomId(),
                            b.getRight().getPrefix(),
                            Markers.EMPTY,
                            -1,
                            "-1",
                            null,
                            JavaType.Primitive.Int));
                }
                return b;
            }
        });
    }
}
