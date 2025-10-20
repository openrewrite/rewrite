/*
 * Copyright 2025 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

import java.util.List;

public class SimplifySingleElementAnnotation extends Recipe {

    @Override
    public String getDisplayName() {
        return "Simplify single-element annotation";
    }

    @Override
    public String getDescription() {
        return "This recipe will remove the attribute `value` on single-element annotations. " +
                "According to JLS, a _single-element annotation_, is a shorthand designed for use with single-element annotation types.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new SimplifySingleElementAnnotationVisitor(null);
    }

    public static <J2 extends J> TreeVisitor<?, ExecutionContext> modifyOnly(J2 scope) {
        return new SimplifySingleElementAnnotationVisitor(scope);
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class SimplifySingleElementAnnotationVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Nullable
        J scope;

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext executionContext) {
            J.Annotation an = super.visitAnnotation(annotation, executionContext);
            if (an.getArguments() != null &&
                    an.getArguments().size() == 1 &&
                    (scope == null || an.equals(scope))) {
                return an.withArguments(ListUtils.mapFirst(an.getArguments(), v -> {
                    if (v instanceof J.Assignment &&
                            ((J.Assignment) v).getVariable() instanceof J.Identifier &&
                            "value".equals(((J.Identifier) ((J.Assignment) v).getVariable()).getSimpleName())) {
                        Expression assignment = ((J.Assignment) v).getAssignment();
                        if (assignment instanceof J.NewArray) {
                            J.NewArray na = (J.NewArray) assignment;
                            List<Expression> initializer = na.getInitializer();
                            if (initializer != null && initializer.size() == 1 && !(initializer.get(0) instanceof J.Empty)) {
                                return initializer.get(0).withPrefix(Space.EMPTY);
                            }
                        }
                        return assignment.withPrefix(Space.EMPTY);
                    }
                    return v;
                }));
            }
            return an;
        }
    }
}
