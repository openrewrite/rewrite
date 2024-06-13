/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

public class RecipeEqualsAndHashCodeCallSuper extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use of `@EqualsAndHashCode` on `Recipe`";
    }

    @Override
    public String getDescription() {
        return "Recipes are value objects, so should use `@EqualsAndHashCode(callSuper = false)`. " +
               "While in most cases recipes do not extend other classes and so the option is moot, as " +
               "a matter of stylistic consistency and to enforce the idea that recipes are value objects, " +
               "this value should be set to `false`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>("org.openrewrite.Recipe", true),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                        if (TypeUtils.isOfClassType(annotation.getType(), "lombok.EqualsAndHashCode") &&
                            getCursor().getParentTreeCursor().getValue() instanceof J.ClassDeclaration) {
                            return (J.Annotation) new JavaIsoVisitor<ExecutionContext>() {
                                @Override
                                public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                                    if (assignment.getVariable() instanceof J.Identifier &&
                                        "callSuper".equals(((J.Identifier) assignment.getVariable()).getSimpleName()) &&
                                        J.Literal.isLiteralValue(assignment.getAssignment(), true)) {
                                        return assignment.withAssignment(((J.Literal) assignment.getAssignment())
                                                .withValue(false).withValueSource("false"));
                                    }
                                    return super.visitAssignment(assignment, ctx);
                                }
                            }.visitNonNull(annotation, ctx, getCursor().getParentOrThrow());
                        }
                        return super.visitAnnotation(annotation, ctx);
                    }
                });
    }
}
