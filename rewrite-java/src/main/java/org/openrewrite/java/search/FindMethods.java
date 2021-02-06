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
package org.openrewrite.java.search;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.RecipeSearchResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Finds matching method invocations.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public final class FindMethods extends Recipe {
    /**
     * A method pattern, expressed as a pointcut expression, that is used to find matching method invocations.
     * See {@link MethodMatcher} for details on the expression's syntax.
     */
    private final String methodPattern;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern);
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (methodMatcher.matches(method)) {
                    m = m.withMarker(new RecipeSearchResult(FindMethods.this));
                }
                return m;
            }
        };
    }

    public static Set<J.MethodInvocation> find(J j, String methodPattern) {
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern);
        JavaIsoVisitor<Set<J.MethodInvocation>> findVisitor = new JavaIsoVisitor<Set<J.MethodInvocation>>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<J.MethodInvocation> ms) {
                if (methodMatcher.matches(method)) {
                    ms.add(method);
                }
                return super.visitMethodInvocation(method, ms);
            }
        };

        Set<J.MethodInvocation> ms = new HashSet<>();
        findVisitor.visit(j, ms);
        return ms;
    }
}
