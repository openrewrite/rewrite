/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveAnnotationVisitor extends JavaIsoVisitor<ExecutionContext> {
    AnnotationMatcher annotationMatcher;

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
        J.Annotation annotationRemoved = getCursor().pollMessage("annotationRemoved");

        List<J.Annotation> leadingAnnotations = classDecl.getLeadingAnnotations();
        if (annotationRemoved != null && !leadingAnnotations.isEmpty()) {
            if (leadingAnnotations.get(0) == annotationRemoved && leadingAnnotations.size() == 1) {
                if (!c.getModifiers().isEmpty()) {
                    c = c.withModifiers(Space.formatFirstPrefix(c.getModifiers(), Space.firstPrefix(c.getModifiers()).withWhitespace("")));
                } else if (c.getPadding().getTypeParameters() != null) {
                    c = c.getPadding().withTypeParameters(c.getPadding().getTypeParameters().withBefore(c.getPadding().getTypeParameters().getBefore().withWhitespace("")));
                } else {
                    c = c.getAnnotations().withKind(c.getAnnotations().getKind().withPrefix(c.getAnnotations().getKind().getPrefix().withWhitespace("")));
                }
            } else {
                List<J.Annotation> newLeadingAnnotations = removeAnnotationOrEmpty(leadingAnnotations, annotationRemoved);
                if (!newLeadingAnnotations.isEmpty()) {
                    c = c.withLeadingAnnotations(newLeadingAnnotations);
                }
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
            if (leadingAnnotations.get(0) == annotationRemoved && leadingAnnotations.size() == 1) {
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
                List<J.Annotation> newLeadingAnnotations = removeAnnotationOrEmpty(leadingAnnotations, annotationRemoved);
                if (!newLeadingAnnotations.isEmpty()) {
                    m = m.withLeadingAnnotations(newLeadingAnnotations);
                }
            }
        }
        return m;
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
        J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
        J.Annotation annotationRemoved = getCursor().pollMessage("annotationRemoved");

        List<J.Annotation> leadingAnnotations = multiVariable.getLeadingAnnotations();
        if (annotationRemoved != null && !leadingAnnotations.isEmpty()) {
            if (leadingAnnotations.get(0) == annotationRemoved && leadingAnnotations.size() == 1) {
                if (!v.getModifiers().isEmpty()) {
                    v = v.withModifiers(Space.formatFirstPrefix(v.getModifiers(), Space.firstPrefix(v.getModifiers()).withWhitespace("")));
                } else if (v.getTypeExpression() != null) {
                    v = v.withTypeExpression(v.getTypeExpression().withPrefix(v.getTypeExpression().getPrefix().withWhitespace("")));
                }
            } else {
                List<J.Annotation> newLeadingAnnotations = removeAnnotationOrEmpty(leadingAnnotations, annotationRemoved);
                if (!newLeadingAnnotations.isEmpty()) {
                    v = v.withLeadingAnnotations(newLeadingAnnotations);
                }
            }
        }

        return v;
    }

    @Override
    public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
        if (annotationMatcher.matches(annotation)) {
            getCursor().getParentOrThrow().putMessage("annotationRemoved", annotation);
            maybeRemoveImport(TypeUtils.asFullyQualified(annotation.getType()));
            maybeRemoveAnnotationParameterImports(annotation);
            //noinspection ConstantConditions
            return null;
        }
        return super.visitAnnotation(annotation, ctx);
    }

    /* Returns a list of leading annotations with the target removed or an empty list if no changes are necessary.
     * A prefix only needs to change if the index == 0 and the prefixes of the target annotation and next annotation are not equal.
     */
    private List<J.Annotation> removeAnnotationOrEmpty(List<J.Annotation> leadingAnnotations, J.Annotation targetAnnotation) {
        int index = leadingAnnotations.indexOf(targetAnnotation);
        List<J.Annotation> newLeadingAnnotations = new ArrayList<>();
        if (index == 0) {
            J.Annotation nextAnnotation = leadingAnnotations.get(1);
            if (!nextAnnotation.getPrefix().equals(targetAnnotation.getPrefix())) {
                newLeadingAnnotations.add(nextAnnotation.withPrefix(targetAnnotation.getPrefix()));
                for (int i = 2; i < leadingAnnotations.size(); ++i) {
                    newLeadingAnnotations.add(leadingAnnotations.get(i));
                }
            }
        }
        return newLeadingAnnotations;
    }

    /**
     * If the annotation has parameters, then the imports for the parameter types may need to be removed.
     *
     * @param annotation the annotation to check
     */
    private void maybeRemoveAnnotationParameterImports(@NonNull J.Annotation annotation) {
        if (ListUtils.nullIfEmpty(annotation.getArguments()) == null) {
            return;
        }

        List<Expression> arguments = annotation.getArguments();

        arguments.forEach(argument -> {
            if (argument instanceof J.Assignment) {
                J.Assignment assignment = (J.Assignment) argument;
                Expression expression = assignment.getAssignment();
                maybeRemoveImportFromExpression(expression);
            } else {
                maybeRemoveImport(TypeUtils.asFullyQualified(argument.getType()));
            }
        });
    }

    private void maybeRemoveImportFromExpression(Expression expression) {
        if (expression instanceof J.NewArray) {
            maybeRemoveAnnotationFromArray((J.NewArray) expression);
        } else if (expression instanceof J.FieldAccess) {
            maybeRemoveAnnotationFromFieldAccess((J.FieldAccess) expression);
        } else if (expression instanceof J.Identifier) {
            JavaType.Variable fieldType = ((J.Identifier) expression).getFieldType();
            if (fieldType != null) {
                maybeRemoveImport(TypeUtils.asFullyQualified(fieldType.getOwner()));
            }
        } else {
            maybeRemoveImport(TypeUtils.asFullyQualified(expression.getType()));
        }
    }

    private void maybeRemoveAnnotationFromArray(@NonNull J.NewArray newArray) {
        List<Expression> initializer = newArray.getInitializer();
        if (ListUtils.nullIfEmpty(initializer) != null) {
            initializer.forEach(this::maybeRemoveImportFromExpression);
        }
    }

    private void maybeRemoveAnnotationFromFieldAccess(@NonNull J.FieldAccess fa) {
        JavaType.Variable fieldType = fa.getName().getFieldType();
        if (fieldType != null && fieldType.getOwner() != null) {
            maybeRemoveImport(TypeUtils.asFullyQualified(fieldType.getOwner()));
        }
    }
}
