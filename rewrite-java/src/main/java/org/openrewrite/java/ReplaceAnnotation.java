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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceAnnotation extends Recipe {
    @Option(displayName = "Annotation pattern to replace",
            description = "An annotation pattern, expressed as a method pattern to replace.",
            example = "@org.jetbrains.annotations.NotNull(\"Test\")")
    String annotationPatternToReplace;
    @Option(displayName = "Annotation template to insert",
            description = "An annotation template to add instead of original one, will be parsed with `JavaTemplate`.",
            example = "@NonNull")
    String annotationTemplateToInsert;
    @Option(displayName = "Type of inserted Annotation",
            description = "The fully qualified class name of the annotation to insert.",
            example = "lombok.NonNull")
    String annotationFQN;
    @Option(displayName = "Templates Artifact id",
            description = "The Maven artifactId to load the inserted annotations type from, defaults to JDK internals.",
            example = "lombok",
            required = false)
    String artifactId;

    @Override
    public String getDisplayName() {
        return "Replace Annotation";
    }

    @Override
    public String getDescription() {
        return "Replace an Annotation with another one if the annotation pattern matches. " +
               "Only fixed parameters can be set in the replacement.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(annotationFQN, false),
                new ReplaceAnnotationVisitor(
                        new AnnotationMatcher(annotationPatternToReplace),
                        JavaTemplate.builder(annotationTemplateToInsert)
                                .imports(annotationFQN)
                                .javaParser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true)
                                        .classpath(artifactId))
                                .build()));
    }

    public static class ReplaceAnnotationVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final AnnotationMatcher matcher;
        private final JavaTemplate replacement;

        public ReplaceAnnotationVisitor(AnnotationMatcher annotationMatcher, JavaTemplate replacement) {
            super();
            this.matcher = annotationMatcher;
            this.replacement = replacement;
        }

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
            annotation = super.visitAnnotation(annotation, ctx);

            boolean replaceAnnotation = matcher.matches(annotation);
            if (replaceAnnotation) {
                JavaType replacedAnnotationType = annotation.getType();
                annotation = replacement.apply(getCursor(), annotation.getCoordinates().replace());
                JavaType insertedAnnotationType = annotation.getType();
                maybeRemoveImport(TypeUtils.asFullyQualified(replacedAnnotationType));
                maybeAddImport(TypeUtils.asFullyQualified(insertedAnnotationType).getFullyQualifiedName(), false);
            }

            return annotation;
        }
    }
}

