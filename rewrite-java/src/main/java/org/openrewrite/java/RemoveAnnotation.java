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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Value
public class RemoveAnnotation extends Recipe {
    @Option(displayName = "Annotation pattern",
            description = "An annotation pattern, expressed as a pointcut expression.",
            example = "@java.lang.SuppressWarnings(\"deprecation\")")
    String annotationPattern;

    @Override
    public String getDisplayName() {
        return "Remove annotation";
    }

    @Override
    public String getDescription() {
        return "Remove matching annotations wherever they occur.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        AnnotationMatcher annotationMatcher = new AnnotationMatcher(annotationPattern);
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                J.Annotation annotationRemoved = getCursor().pollMessage("annotationRemoved");

                List<J.Annotation> leadingAnnotations = classDecl.getLeadingAnnotations();
                if (annotationRemoved != null) {
                    if (leadingAnnotations.get(0) == annotationRemoved || leadingAnnotations.size() == 1) {
                        if (!c.getModifiers().isEmpty()) {
                            c = c.withModifiers(Space.formatFirstPrefix(c.getModifiers(), Space.firstPrefix(c.getModifiers()).withWhitespace("")));
                        } else if (c.getPadding().getTypeParameters() != null) {
                            c = c.getPadding().withTypeParameters(c.getPadding().getTypeParameters().withBefore(c.getPadding().getTypeParameters().getBefore().withWhitespace("")));
                        } else {
                            c = c.withName(c.getName().withPrefix(c.getName().getPrefix().withWhitespace("")));
                        }
                    } else {
                        c = autoFormat(c, c.getName(), ctx, getCursor().getParentOrThrow());
                    }
                }
                return c;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                J.Annotation annotationRemoved = getCursor().pollMessage("annotationRemoved");

                List<J.Annotation> leadingAnnotations = method.getLeadingAnnotations();
                if (annotationRemoved != null && !leadingAnnotations.isEmpty()) {
                    if (leadingAnnotations.get(0) == annotationRemoved || leadingAnnotations.size() == 1) {
                        if (!m.getModifiers().isEmpty()) {
                            m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(), Space.firstPrefix(m.getModifiers()).withWhitespace("")));
                        } else if (m.getPadding().getTypeParameters() != null) {
                            m = m.getPadding().withTypeParameters(m.getPadding().getTypeParameters().withPrefix(m.getPadding().getTypeParameters().getPrefix().withWhitespace("")));
                        } else if (m.getReturnTypeExpression() != null) {
                            m = m.withReturnTypeExpression(m.getReturnTypeExpression().withPrefix(m.getReturnTypeExpression().getPrefix().withWhitespace("")));
                        } else {
                            m = m.withName(m.getName().withPrefix(m.getName().getPrefix().withWhitespace("")));
                        }
                    } else {
                        m = autoFormat(m, m.getName(), ctx, getCursor().getParentOrThrow());
                    }
                }
                return m;
            }

            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                if (annotationMatcher.matches(annotation)) {
                    getCursor().getParentOrThrow().putMessage("annotationRemoved", annotation);

                    //noinspection ConstantConditions
                    return null;
                }
                return super.visitAnnotation(annotation, ctx);
            }
        };
    }
}
