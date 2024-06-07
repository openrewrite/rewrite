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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaCoordinates;
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

    @Option(displayName = "Classpath resource",
            description = "If the annotation's type is defined by a jar within the META-INF/rewrite/classpath directory provide its name here " +
                          "so that it can be loaded. " +
                          "When this parameter is not passed the runtime classpath of the recipe is provided to the parser producing the new annotation. " +
                          "This is necessary when the annotation is not on the runtime classpath of the recipe and isn't in the Java standard library.",
            required = false)
    @Nullable
    String classpathResourceName;

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
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                JavaTemplate.Builder templateBuilder = JavaTemplate.builder(annotationTemplateToInsert);
                if (classpathResourceName == null) {
                    templateBuilder.javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
                } else {
                    templateBuilder.javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, classpathResourceName));
                }
                return new ReplaceAnnotationVisitor(new AnnotationMatcher(annotationPatternToReplace), templateBuilder.build())
                        .visit(tree, ctx);
            }
        };
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class ReplaceAnnotationVisitor extends JavaIsoVisitor<ExecutionContext> {
        AnnotationMatcher matcher;
        JavaTemplate replacement;

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
            J.Annotation a = super.visitAnnotation(annotation, ctx);

            if (!matcher.matches(a)) {
                return a;
            }

            maybeRemoveImport(TypeUtils.asFullyQualified(a.getType()));
            JavaCoordinates replaceCoordinate = a.getCoordinates().replace();
            a = replacement.apply(getCursor(), replaceCoordinate);
            doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(a));
            return a;
        }
    }
}
