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

import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

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
            return ((J.VariableDeclarations) j).getAllAnnotations();
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
        }
        return emptyList();
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
        return identifier.getAnnotations() == null ? emptyList() : identifier.getAnnotations();
    }
}
