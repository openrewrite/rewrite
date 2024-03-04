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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

@EqualsAndHashCode(callSuper = false)
@Value
public class FindAnnotations extends Recipe {
    /**
     * An annotation pattern, expressed as a method pattern.
     * See {@link AnnotationMatcher} for syntax.
     */
    @Option(displayName = "Annotation pattern",
            description = "An annotation pattern, expressed as a method pattern.",
            example = "@java.lang.SuppressWarnings(\"deprecation\")")
    String annotationPattern;

    @Option(displayName = "Match on meta annotations",
            description = "When enabled, matches on meta annotations of the annotation pattern.",
            required = false)
    @Nullable
    Boolean matchMetaAnnotations;

    @Override
    public String getDisplayName() {
        return "Find annotations";
    }

    @Override
    public String getDescription() {
        return "Find all annotations matching the annotation pattern.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        AnnotationMatcher annotationMatcher = new AnnotationMatcher(annotationPattern, matchMetaAnnotations);
        return Preconditions.check(
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                        if (tree instanceof JavaSourceFile) {
                            JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                            for (JavaType type : cu.getTypesInUse().getTypesInUse()) {
                                if (annotationMatcher.matchesAnnotationOrMetaAnnotation(TypeUtils.asFullyQualified(type))) {
                                    return SearchResult.found(cu);
                                }
                            }
                        }
                        return super.visit(tree, ctx);
                    }

                    @Override
                    public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                        J.Annotation a = super.visitAnnotation(annotation, ctx);
                        if (annotationMatcher.matches(annotation)) {
                            a = SearchResult.found(a);
                        }
                        return a;
                    }
                },
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                        J.Annotation a = super.visitAnnotation(annotation, ctx);
                        if (annotationMatcher.matches(annotation)) {
                            a = SearchResult.found(a);
                        }
                        return a;
                    }
                }
        );
    }

    public static Set<J.Annotation> find(J j, String annotationPattern) {
        return find(j, annotationPattern, false);
    }

    public static Set<J.Annotation> find(J j, String annotationPattern, boolean matchMetaAnnotations) {
        return TreeVisitor.collect(
                        new FindAnnotations(annotationPattern, matchMetaAnnotations).getVisitor(),
                        j,
                        new HashSet<>()
                )
                .stream()
                .filter(a -> a instanceof J.Annotation)
                .map(a -> (J.Annotation) a)
                .collect(toSet());
    }
}
