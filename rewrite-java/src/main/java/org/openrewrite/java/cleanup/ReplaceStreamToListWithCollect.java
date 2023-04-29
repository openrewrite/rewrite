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
package org.openrewrite.java.cleanup;

import org.openrewrite.Preconditions;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;

import java.time.Duration;

public class ReplaceStreamToListWithCollect extends Recipe {

    private static final MethodMatcher STREAM_TO_LIST = new MethodMatcher("java.util.stream.Stream toList()");

    @Override
    public String getDisplayName() {
        return "Replace Stream.toList() with Stream.collect(Collectors.toList())";
    }

    @Override
    public String getDescription() {
        return "Replace Java 16 `Stream.toList()` with Java 11 `Stream.collect(Collectors.toList())`.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.and(new UsesJavaVersion<>(16),
                new UsesMethod<>(STREAM_TO_LIST)), new JavaVisitor<ExecutionContext>() {

            private final JavaTemplate template = JavaTemplate
                    .builder(this::getCursor, "#{any(java.util.stream.Stream)}.collect(Collectors.toList())")
                    .imports("java.util.stream.Collectors")
                    .build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation result = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (STREAM_TO_LIST.matches(method)) {
                    JRightPadded<Expression> select = result.getPadding().getSelect();
                    result = result.withTemplate(template, result.getCoordinates().replace(), result.getSelect());
                    result = result.getPadding().withSelect(select);
                    maybeAddImport("java.util.stream.Collectors");
                }
                return result;
            }
        });
    }
}
