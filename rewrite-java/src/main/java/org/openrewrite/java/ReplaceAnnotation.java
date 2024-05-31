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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaCoordinates;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceAnnotation extends Recipe {

    @Option(displayName = "Annotation to replace",
            description = "An annotation matcher, expressed as a method pattern to replace.",
            example = "@org.jetbrains.annotations.NotNull(\"Test\")")
    String annotationPatternToReplace;

    @Option(displayName = "Annotation template to insert",
            description = "An annotation template to add instead of original one, will be parsed with `JavaTemplate`.",
            example = "@org.jetbrains.annotations.NotNull(\"Null not permitted\")")
    String annotationTemplateToInsert;

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
        return new ReplaceAnnotationVisitor(
                new AnnotationMatcher(annotationPatternToReplace),
                JavaTemplate.builder(annotationTemplateToInsert).javaParser(JavaParser.fromJavaVersion()).build());
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
        public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext ctx) {
            J.Annotation maybeReplacingAnnotation = super.visitAnnotation(a, ctx);

            boolean keepAnnotation = !matcher.matches(maybeReplacingAnnotation);
            if (keepAnnotation) {
                return maybeReplacingAnnotation;
            }


            JavaType.FullyQualified replacedAnnotationType = TypeUtils.asFullyQualified(maybeReplacingAnnotation.getType());
            maybeRemoveImport(replacedAnnotationType);

            JavaCoordinates replaceCoordinate = maybeReplacingAnnotation.getCoordinates().replace();
            J.Annotation replacement = this.replacement.apply(getCursor(), replaceCoordinate);

            doAfterVisit(ShortenFullyQualifiedTypeReferences.modifyOnly(replacement));

            return replacement;
        }
    }
}

