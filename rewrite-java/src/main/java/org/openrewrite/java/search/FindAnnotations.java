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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaSearchResult;
import org.openrewrite.java.tree.J;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.openrewrite.Tree.randomId;

@EqualsAndHashCode(callSuper = true)
@Value
public class FindAnnotations extends Recipe {
    /**
     * An annotation pattern, expressed as a pointcut expression.
     * See {@link AnnotationMatcher} for syntax.
     */
    @Option(displayName = "Annotation pattern",
            description = "An annotation pattern, expressed as a pointcut expression.",
            example = "@java.lang.SuppressWarnings(\"deprecation\")")
    String annotationPattern;

    UUID id = randomId();

    @Override
    public String getDisplayName() {
        return "Find annotations";
    }

    @Override
    public String getDescription() {
        return "Find all annotations matching the annotation pattern.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        AnnotationMatcher annotationMatcher = new AnnotationMatcher(annotationPattern);
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                J.Annotation a = super.visitAnnotation(annotation, ctx);
                if (annotationMatcher.matches(annotation)) {
                    a = a.withMarkers(a.getMarkers().addOrUpdate(new JavaSearchResult(id, FindAnnotations.this)));
                }
                return a;
            }
        };
    }

    public static Set<J.Annotation> find(J j, String annotationPattern) {
        AnnotationMatcher annotationMatcher = new AnnotationMatcher(annotationPattern);
        JavaIsoVisitor<Set<J.Annotation>> findVisitor = new JavaIsoVisitor<Set<J.Annotation>>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, Set<J.Annotation> as) {
                if (annotationMatcher.matches(annotation)) {
                    as.add(annotation);
                }
                return super.visitAnnotation(annotation, as);
            }
        };

        Set<J.Annotation> as = new HashSet<>();
        findVisitor.visit(j, as);
        return as;
    }
}
