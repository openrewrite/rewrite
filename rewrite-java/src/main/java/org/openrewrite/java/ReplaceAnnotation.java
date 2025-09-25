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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

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
            example = "annotations",
            required = false)
    @Nullable
    String classpathResourceName;

    @Override
    public String getDisplayName() {
        return "Replace annotation";
    }

    @Override
    public String getDescription() {
        return "Replace an Annotation with another one if the annotation pattern matches. " +
                "Only fixed parameters can be set in the replacement.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        AnnotationMatcher matcher = new AnnotationMatcher(annotationPatternToReplace);
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                J.Annotation a = super.visitAnnotation(annotation, ctx);

                if (!matcher.matches(a)) {
                    return a;
                }

                // Remove imports for the annotation itself, as well as any arguments
                maybeRemoveImport(TypeUtils.asFullyQualified(a.getType()));
                collectTypesFromArguments(a).forEach(this::maybeRemoveImport);

                a = JavaTemplate.builder(annotationTemplateToInsert)
                        .javaParser(classpathResourceName == null ?
                                JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()) :
                                JavaParser.fromJavaVersion().classpathFromResources(ctx, classpathResourceName))
                        .build()
                        .apply(getCursor(), a.getCoordinates().replace());
                doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(a));
                return a;
            }

            private Collection<JavaType.FullyQualified> collectTypesFromArguments(J.Annotation a) {
                if (a.getArguments() == null || a.getArguments().isEmpty() || a.getArguments().get(0) instanceof J.Empty) {
                    return emptySet();
                }

                Set<JavaType.FullyQualified> typesToRemove = new HashSet<>();
                for (Expression arg : a.getArguments()) {
                    collectTypesFromExpression(arg, typesToRemove);
                }
                return typesToRemove;
            }

            private void collectTypesFromExpression(Expression expr, Set<JavaType.FullyQualified> types) {
                if (expr instanceof J.FieldAccess) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) expr;
                    // Check if this is a class literal (e.g., String.class)
                    if ("class".equals(fieldAccess.getSimpleName())) {
                        Expression target = fieldAccess.getTarget();
                        JavaType targetType = target.getType();
                        if (targetType instanceof JavaType.FullyQualified) {
                            types.add((JavaType.FullyQualified) targetType);
                        }
                    }
                    // Recursively check the target for nested field accesses
                    collectTypesFromExpression(fieldAccess.getTarget(), types);
                } else if (expr instanceof J.NewArray) {
                    // Handle array initializers like {String.class, List.class}
                    J.NewArray newArray = (J.NewArray) expr;
                    if (newArray.getInitializer() != null) {
                        for (Expression element : newArray.getInitializer()) {
                            collectTypesFromExpression(element, types);
                        }
                    }
                } else if (expr instanceof J.Assignment) {
                    // Handle named arguments like value = String.class
                    J.Assignment assignment = (J.Assignment) expr;
                    collectTypesFromExpression(assignment.getAssignment(), types);
                } else if (expr instanceof J.Identifier) {
                    // Handle simple identifiers that might be class references
                    J.Identifier identifier = (J.Identifier) expr;
                    JavaType type = identifier.getType();
                    if (type instanceof JavaType.Class) {
                        JavaType.Class classType = (JavaType.Class) type;
                        // Check if this is a reference to a Class type (e.g., in "String.class")
                        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(classType);
                        if (fq != null && !fq.getFullyQualifiedName().startsWith("java.lang.Class")) {
                            types.add(fq);
                        }
                    }
                }
            }
        };
    }
}
