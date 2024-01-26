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

import org.openrewrite.*;
import org.openrewrite.java.cleanup.UnnecessaryParenthesesVisitor;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

public class RemoveObjectsIsNull extends Recipe {
    private static final MethodMatcher IS_NULL = new MethodMatcher("java.util.Objects isNull(..)");
    private static final MethodMatcher NON_NULL = new MethodMatcher("java.util.Objects nonNull(..)");

    @Override
    public String getDisplayName() {
        return "Transform calls to `Objects.isNull(..)` and `Objects.nonNull(..)`";
    }

    @Override
    public String getDescription() {
        return "Replace calls to `Objects.isNull(..)` and `Objects.nonNull(..)` with a simple null check. " +
               "Using these methods outside of stream predicates is not idiomatic.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(new UsesMethod<>(IS_NULL), new UsesMethod<>(NON_NULL)), new JavaVisitor<ExecutionContext>() {
            @Override
            public Expression visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (IS_NULL.matches(m)) {
                    return replace(ctx, m, "(#{any(boolean)}) == null");
                } else if (NON_NULL.matches(m)) {
                    return replace(ctx, m, "(#{any(boolean)}) != null");
                }
                return m;
            }

            private Expression replace(ExecutionContext ctx, J.MethodInvocation m, String pattern) {
                maybeRemoveImport("java.util.Objects");
                maybeRemoveImport("java.util.Objects." + m.getSimpleName());

                // Upcasted primitives are never null; simplify logic for use in SimplifyConstantIfBranchExecution
                JavaType type = m.getArguments().get(0).getType();
                if (type instanceof JavaType.Primitive && JavaType.Primitive.String != type) {
                    boolean replacementValue = NON_NULL.matches(m);
                    return new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, replacementValue, String.valueOf(replacementValue), null, JavaType.Primitive.Boolean);
                }

                // Replace the method invocation with a simple null check
                Expression e = m.getArguments().get(0);
                Expression replaced = JavaTemplate.apply(pattern, getCursor(), m.getCoordinates().replace(), e);
                return (Expression) new UnnecessaryParenthesesVisitor().visitNonNull(replaced, ctx, getCursor());
            }
        });
    }
}
