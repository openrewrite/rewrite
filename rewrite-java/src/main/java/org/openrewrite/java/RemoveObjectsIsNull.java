/*
 * Copyright 2022 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.cleanup.UnnecessaryParenthesesVisitor;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

public class RemoveObjectsIsNull extends Recipe {

    @JsonCreator
    public RemoveObjectsIsNull() {
    }

    @Override
    public String getDisplayName() {
        return "Transform calls to `Objects.isNull(..)` and `Objects.nonNull(..)`";
    }

    @Override
    public String getDescription() {
        return "Replace calls to `Objects.isNull(..)` and `Objects.nonNull(..)` with a simple null check. Using these methods outside of stream predicates is not idiomatic.";
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new TransformCallsToObjectsIsNullVisitor();
    }

    private static final MethodMatcher isNullmatcher = new MethodMatcher("java.util.Objects isNull(..)");
    private static final MethodMatcher nonNullmatcher = new MethodMatcher("java.util.Objects nonNull(..)");



    private class TransformCallsToObjectsIsNullVisitor extends JavaVisitor<ExecutionContext> {
        @Override
        public Expression visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
            if(isNullmatcher.matches(m)) {
                return replace(executionContext, m, "(#{any(boolean)}) == null");
            } else if(nonNullmatcher.matches(m)) {
                return replace(executionContext, m, "(#{any(boolean)}) != null");
            }
            return m;
        }

        @NonNull
        private Expression replace(ExecutionContext executionContext, J.MethodInvocation m, String pattern) {
            JavaTemplate template = JavaTemplate.builder(this::getCursor, pattern).build();
            Expression e = m.getArguments().get(0);
            Expression replaced = m.withTemplate(template, m.getCoordinates().replace(), e);
            UnnecessaryParenthesesVisitor<ExecutionContext> v = new UnnecessaryParenthesesVisitor<>(Checkstyle.unnecessaryParentheses());
            return (Expression) v.visitNonNull(replaced, executionContext, getCursor());
        }
    }

}
