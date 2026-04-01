/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;

@Incubating(since = "8.12.0")
public class AnnotationService {

    public boolean matches(Cursor cursor, AnnotationMatcher matcher) {
        for (J.Annotation annotation : getAllAnnotations(cursor)) {
            if (matcher.matches(annotation)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public List<J.Annotation> getAllAnnotations(Cursor cursor) {
        J j = cursor.getValue();
        if (j instanceof J.VariableDeclarations) {
            return getAllAnnotations((J.VariableDeclarations) j);
        } else if (j instanceof J.MethodDeclaration) {
            return ((J.MethodDeclaration) j).getAllAnnotations();
        } else if (j instanceof J.ClassDeclaration) {
            return ((J.ClassDeclaration) j).getAllAnnotations();
        } else if (j instanceof J.TypeParameter) {
            return ((J.TypeParameter) j).getAnnotations();
        } else if (j instanceof J.TypeParameters) {
            return ((J.TypeParameters) j).getAnnotations();
        } else if (j instanceof J.Package) {
            return ((J.Package) j).getAnnotations();
        } else if (j instanceof J.AnnotatedType) {
            return getAllAnnotations((J.AnnotatedType) j);
        } else if (j instanceof J.ArrayType) {
            return getAllAnnotations((J.ArrayType) j);
        } else if (j instanceof J.FieldAccess) {
            return getAllAnnotations((J.FieldAccess) j);
        } else if (j instanceof J.Identifier) {
            return getAllAnnotations((J.Identifier) j);
        }
        return emptyList();
    }

    private List<J.Annotation> getAllAnnotations(J j) {
        if (j instanceof J.AnnotatedType) {
            return getAllAnnotations((J.AnnotatedType) j);
        } else if (j instanceof J.ArrayType) {
            return getAllAnnotations((J.ArrayType) j);
        } else if (j instanceof J.Identifier) {
            return getAllAnnotations((J.Identifier) j);
        } else if (j instanceof J.FieldAccess) {
            return getAllAnnotations((J.FieldAccess) j);
        } else if (j instanceof J.VariableDeclarations) {
            return getAllAnnotations((J.VariableDeclarations) j);
        }
        return emptyList();
    }

    private List<J.Annotation> getAllAnnotations(J.VariableDeclarations variableDeclarations) {
        List<J.Annotation> allAnnotations = new ArrayList<>(variableDeclarations.getLeadingAnnotations());
        for (J.Modifier modifier : variableDeclarations.getModifiers()) {
            allAnnotations.addAll(modifier.getAnnotations());
        }
        if (variableDeclarations.getTypeExpression() instanceof J.AnnotatedType) {
            allAnnotations.addAll(getAllAnnotations(((J.AnnotatedType) variableDeclarations.getTypeExpression())));
        }
        return allAnnotations;
    }

    private List<J.Annotation> getAllAnnotations(J.AnnotatedType annotatedType) {
        List<J.Annotation> targetAnnotations = getAllAnnotations(annotatedType.getTypeExpression());
        if (targetAnnotations.isEmpty()) {
            return annotatedType.getAnnotations();
        }
        List<J.Annotation> annotations = new ArrayList<>(annotatedType.getAnnotations().size() + targetAnnotations.size());
        annotations.addAll(annotatedType.getAnnotations());
        annotations.addAll(targetAnnotations);
        return annotations;
    }

    private List<J.Annotation> getAllAnnotations(J.ArrayType arrayType) {
        if (arrayType.getAnnotations() != null) {
            return arrayType.getAnnotations();
        }
        return emptyList();
    }

    private List<J.Annotation> getAllAnnotations(J.FieldAccess fieldAccess) {
        return getAllAnnotations(fieldAccess.getName());
    }

    private List<J.Annotation> getAllAnnotations(J.Identifier identifier) {
        return identifier.getAnnotations();
    }

    /**
     * Checks if the given element has the specified annotation,
     * searching through type information including:
     * - Direct annotations on the element
     * - Inherited annotations from parent classes (for class declarations)
     * - Inherited annotations from overridden methods (for method declarations)
     * - Meta-annotations (annotations on annotations)
     *
     * @param j The element to check (J.ClassDeclaration, J.MethodDeclaration, or J.VariableDeclarations)
     * @param annotationFqn The fully qualified name of the annotation to search for
     * @return true if the annotation is found anywhere in the hierarchy, false otherwise
     */
    public boolean isAnnotatedWith(J j, String annotationFqn) {
        return isAnnotatedWith(j, annotationFqn, true);
    }

    /**
     * Checks if the given element has the specified annotation,
     * searching through type information including:
     * - Direct annotations on the element
     * - Inherited annotations from parent classes (for class declarations)
     * - Inherited annotations from overridden methods (for method declarations)
     * - Optionally, meta-annotations (annotations on annotations)
     *
     * @param j The element to check (J.ClassDeclaration, J.MethodDeclaration, or J.VariableDeclarations)
     * @param annotationFqn The fully qualified name of the annotation to search for
     * @param includeMetaAnnotations Whether to search for meta-annotations (annotations on annotations)
     * @return true if the annotation is found anywhere in the hierarchy, false otherwise
     */
    public boolean isAnnotatedWith(J j, String annotationFqn, boolean includeMetaAnnotations) {
        return !annotatedWith(j, annotationFqn, includeMetaAnnotations).isEmpty();
    }

    /**
     * Finds all instances of the specified annotation in the type hierarchy,
     * including direct annotations, inherited annotations, and meta-annotations.
     *
     * @param j The element to check (J.ClassDeclaration, J.MethodDeclaration, or J.VariableDeclarations)
     * @param annotationFqn The fully qualified name of the annotation to search for
     * @return A list of all matching annotations found in the hierarchy
     */
    public List<JavaType.FullyQualified> annotatedWith(J j, String annotationFqn) {
        return annotatedWith(j, annotationFqn, true);
    }

    /**
     * Finds all instances of the specified annotation in the type hierarchy,
     * including direct annotations, inherited annotations, and optionally meta-annotations.
     *
     * @param j The element to check (J.ClassDeclaration, J.MethodDeclaration, or J.VariableDeclarations)
     * @param annotationFqn The fully qualified name of the annotation to search for
     * @param includeMetaAnnotations Whether to search for meta-annotations (annotations on annotations)
     * @return A list of all matching annotations found in the hierarchy
     */
    public List<JavaType.FullyQualified> annotatedWith(J j, String annotationFqn, boolean includeMetaAnnotations) {
        List<JavaType.FullyQualified> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        if (j instanceof J.ClassDeclaration) {
            J.ClassDeclaration classDecl = (J.ClassDeclaration) j;
            JavaType.FullyQualified type = TypeUtils.asFullyQualified(classDecl.getType());
            if (type != null) {
                collectClassAnnotations(type, annotationFqn, result, visited, includeMetaAnnotations);
            }
        } else if (j instanceof J.MethodDeclaration) {
            J.MethodDeclaration methodDecl = (J.MethodDeclaration) j;
            if (methodDecl.getMethodType() != null) {
                collectMethodAnnotations(methodDecl.getMethodType(), annotationFqn, result, visited, includeMetaAnnotations);
            }
        } else if (j instanceof J.VariableDeclarations) {
            J.VariableDeclarations varDecls = (J.VariableDeclarations) j;
            for (J.Annotation ann : getAllAnnotations(varDecls)) {
                JavaType.FullyQualified annType = TypeUtils.asFullyQualified(ann.getType());
                if (annType != null) {
                    collectAnnotationAndMeta(annType, annotationFqn, result, visited, includeMetaAnnotations);
                }
            }
        }

        return result;
    }

    private void collectClassAnnotations(JavaType.FullyQualified type, String annotationFqn,
                                        List<JavaType.FullyQualified> result, Set<String> visited,
                                        boolean includeMetaAnnotations) {
        if (!visited.add(type.getFullyQualifiedName())) {
            return;
        }

        for (JavaType.FullyQualified annotation : type.getAnnotations()) {
            collectAnnotationAndMeta(annotation, annotationFqn, result, visited, includeMetaAnnotations);
        }

        JavaType.@Nullable FullyQualified supertype = type.getSupertype();
        if (supertype != null && !(supertype instanceof JavaType.Unknown)) {
            collectClassAnnotations(supertype, annotationFqn, result, visited, includeMetaAnnotations);
        }

        for (JavaType.FullyQualified _interface : type.getInterfaces()) {
            if (!(_interface instanceof JavaType.Unknown)) {
                collectClassAnnotations(_interface, annotationFqn, result, visited, includeMetaAnnotations);
            }
        }
    }

    private void collectMethodAnnotations(JavaType.Method method, String annotationFqn,
                                         List<JavaType.FullyQualified> result, Set<String> visited,
                                         boolean includeMetaAnnotations) {
        for (JavaType.FullyQualified annotation : method.getAnnotations()) {
            collectAnnotationAndMeta(annotation, annotationFqn, result, visited, includeMetaAnnotations);
        }

        JavaType.Method override = method.getOverride();
        if (override != null) {
            collectMethodAnnotations(override, annotationFqn, result, visited, includeMetaAnnotations);
        }
    }

    private void collectAnnotationAndMeta(JavaType.FullyQualified annotation, String annotationFqn,
                                         List<JavaType.FullyQualified> result, Set<String> visited,
                                         boolean includeMetaAnnotations) {
        String annFqn = annotation.getFullyQualifiedName();
        if (visited.contains(annFqn)) {
            return;
        }

        if (TypeUtils.isAssignableTo(annotationFqn, annotation)) {
            result.add(annotation);
        }

        visited.add(annFqn);

        // Check meta-annotations (annotations on this annotation) if enabled
        if (includeMetaAnnotations && !annFqn.startsWith("java.lang.annotation.") && !annFqn.startsWith("jdk.internal.")) {
            for (JavaType.FullyQualified metaAnnotation : annotation.getAnnotations()) {
                collectAnnotationAndMeta(metaAnnotation, annotationFqn, result, visited, includeMetaAnnotations);
            }
        }
    }
}
