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
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

@Incubating(since = "7.10.0")
public class IndexOfShouldNotCompareGreaterThanZero extends Recipe {
    private static final MethodMatcher STRING_INDEX_MATCHER = new MethodMatcher("java.lang.String indexOf(String)");
    private static final MethodMatcher LIST_INDEX_MATCHER = new MethodMatcher("java.util.List indexOf(Object)");

    @Override
    public String getDisplayName() {
        return "`indexOf` should not compare greater than zero";
    }

    @Override
    public String getDescription() {
        return "Replaces `String#indexOf(String) > 0` and `List#indexOf(Object) > 0` with `>=1`. " +
               "Checking `indexOf` against `>0` ignores the first element, whereas `>-1` is inclusive of the first element. " +
               "For clarity, `>=1` is used, because `>0` and `>=1` are semantically equal. Using `>0` may appear to be a mistake " +
               "with the intent of including all elements. If the intent is to check whether a value in included in a `String` or `List`, " +
               "the `String#contains(String)` or `List#contains(Object)` methods may be better options altogether.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2692");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(
                        new UsesMethod<>(STRING_INDEX_MATCHER),
                        new UsesMethod<>(LIST_INDEX_MATCHER)
                ),
                new IndexOfShouldNotCompareGreaterThanZeroVisitor()
        );
    }

    private static class IndexOfShouldNotCompareGreaterThanZeroVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.Binary visitBinary(J.Binary binary, ExecutionContext ctx) {
            J.Binary b = super.visitBinary(binary, ctx);
            if (b.getOperator() == J.Binary.Type.GreaterThan) {
                if (b.getLeft() instanceof J.MethodInvocation) {
                    J.MethodInvocation mi = (J.MethodInvocation) b.getLeft();
                    if (STRING_INDEX_MATCHER.matches(mi) || LIST_INDEX_MATCHER.matches(mi)) {
                        if (b.getRight() instanceof J.Literal && "0".equals(((J.Literal) b.getRight()).getValueSource())) {
                            b = b.withRight(((J.Literal) b.getRight()).withValueSource("1")).withOperator(J.Binary.Type.GreaterThanOrEqual);
                        }
                    }
                }
            }
            return b;
        }
    }

}
