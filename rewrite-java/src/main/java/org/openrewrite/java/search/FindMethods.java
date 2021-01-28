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
import org.openrewrite.marker.SearchResult;

import java.util.Set;

/**
 * A Java search visitor that will return a list of matching method invocations within the abstract syntax tree.
 *
 * See {@link  MethodMatcher} for details on the expression's syntax.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public final class FindMethods extends Recipe {

    /**
     * A method pattern, expressed as a pointcut expression, that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    private final String methodPattern;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new FindMethodsVisitor(methodPattern);
    }

    public static Set<J.MethodInvocation> find(J j, String methodPattern) {
        //noinspection ConstantConditions
        return new FindMethodsVisitor(methodPattern)
                .visit(j, ExecutionContext.builder().build())
                .findMarkedWith(SearchResult.class);
    }

    private static class FindMethodsVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher matcher;

        /**
         * See {@link FindMethods} for details on how the signature should be formatted.
         *
         * @param signature Pointcut expression for matching methods.
         */
        public FindMethodsVisitor(String signature) {
            this.matcher = new MethodMatcher(signature);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (matcher.matches(method)) {
                return m.mark(new SearchResult());
            }
            return super.visitMethodInvocation(m, ctx);
        }
    }
}
