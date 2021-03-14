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
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.Collections;

import static org.openrewrite.Tree.randomId;

@EqualsAndHashCode(callSuper = true)
@Value
public class RemoveAnnotation extends Recipe {
    private static final J.Block EMPTY_BLOCK = new J.Block(randomId(), Space.EMPTY,
            Markers.EMPTY, new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY),
            Collections.emptyList(), Space.EMPTY);

    /**
     * An annotation pattern, expressed as a pointcut expression.
     * See {@link AnnotationMatcher} for syntax.
     */
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
        return "Remove matching annotations wherever they occur";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        AnnotationMatcher annotationMatcher = new AnnotationMatcher(annotationPattern);
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                Boolean annotationRemoved = getCursor().<Boolean>pollMessage("annotationRemoved");
                if (annotationRemoved != null && annotationRemoved) {
                    c = autoFormat(c.withBody(EMPTY_BLOCK), ctx, getCursor().getParentOrThrow()).withBody(c.getBody());
                }
                return c;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                Boolean annotationRemoved = getCursor().<Boolean>pollMessage("annotationRemoved");
                if (annotationRemoved != null && annotationRemoved) {
                    m = autoFormat(m.withBody(null), ctx, getCursor().getParentOrThrow()).withBody(m.getBody());
                }
                return m;
            }

            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                if (annotationMatcher.matches(annotation)) {
                    getCursor().getParentOrThrow().putMessage("annotationRemoved", true);

                    //noinspection ConstantConditions
                    return null;
                }
                return super.visitAnnotation(annotation, ctx);
            }
        };
    }
}
