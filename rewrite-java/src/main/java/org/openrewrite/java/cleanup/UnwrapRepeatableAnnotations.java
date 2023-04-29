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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.FindRepeatableAnnotations;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

public class UnwrapRepeatableAnnotations extends Recipe {
    @Override
    public String getDisplayName() {
        return "Unwrap `@Repeatable` annotations";
    }

    @Override
    public String getDescription() {
        return "Java 8 introduced the concept of `@Repeatable` annotations, " +
               "making the wrapper annotation unnecessary.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindRepeatableAnnotations(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                List<J.Annotation> ann = unwrap(m.getLeadingAnnotations());
                if (ann.isEmpty()) {
                    return m;
                }
                return maybeAutoFormat(m, m.withLeadingAnnotations(ann), ann.get(ann.size() - 1), ctx,
                        getCursor().getParentOrThrow());
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                List<J.Annotation> ann = unwrap(c.getLeadingAnnotations());
                if (ann.isEmpty()) {
                    return c;
                }
                return maybeAutoFormat(c, c.withLeadingAnnotations(ann), ann.get(ann.size() - 1), ctx,
                        getCursor().getParentOrThrow());
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
                List<J.Annotation> ann = unwrap(v.getLeadingAnnotations());
                if (ann.isEmpty()) {
                    return v;
                }
                return maybeAutoFormat(v, v.withLeadingAnnotations(ann), ann.get(ann.size() - 1), ctx,
                        getCursor().getParentOrThrow());
            }

            private List<J.Annotation> unwrap(List<J.Annotation> annotations) {
                return ListUtils.flatMap(annotations, a -> {
                    List<J.Annotation> unwrapped = new ArrayList<>(1);

                    new JavaVisitor<Integer>() {
                        @Override
                        public J visitAnnotation(J.Annotation annotation, Integer p) {
                            if (annotation != a && FindRepeatableAnnotations.isRepeatable(annotation.getType())) {
                                unwrapped.add(annotation);
                            }
                            return super.visitAnnotation(annotation, p);
                        }
                    }.visit(a, 0);

                    return unwrapped.isEmpty() ? a : unwrapped;
                });
            }
        });
    }
}
