/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * @author: Alex Boyko
 */
package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaCoordinates;
import org.openrewrite.template.SourceTemplate;

import java.util.List;

public class AddOrReplaceAnnotationAttributeVisitor extends JavaIsoVisitor<ExecutionContext> {

    private final J.Annotation targetAnnotation;
    private final String attribute;
    private final Object value;
    private final Class valueType;

    public AddOrReplaceAnnotationAttributeVisitor(J.Annotation targetAnnotation, String attribute, Object value, Class valueType) {
        this.targetAnnotation = targetAnnotation;
        this.attribute = attribute.trim();
        this.value = value;
        this.valueType = valueType;
    }

    @Override
    public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext executionContext) {
        if (!targetAnnotation.getId().equals(annotation.getId())) {
            return super.visitAnnotation(annotation, executionContext);
        }

        String templateString = renderTemplateString(annotation);

        SourceTemplate<J, JavaCoordinates> template = JavaTemplate.builder(() -> getCursor(), templateString).build();
        return annotation.withTemplate(template, annotation.getCoordinates().replace());
    }

    private String renderTemplateString(J.Annotation annotation) {
        boolean attributeHandled = false;
        List<Expression> annotationArguments = annotation.getArguments();

        StringBuilder templateString = new StringBuilder("@" + annotation.getSimpleName());
        templateString.append("(");
        if (hasArguments(annotationArguments)) {
            for (Expression exp : annotationArguments) {
                if (exp.getClass().isAssignableFrom(J.Assignment.class)) {
                    J.Assignment assignment = (J.Assignment) exp;
                    if (assignment.getVariable().print().equals(attribute)) {
                        attributeHandled = true;
                        renderAttribute(templateString);
                    } else {
                        templateString.append(assignment.printTrimmed());
                    }
                    if (hasMoreElements(annotationArguments, exp)) {
                        templateString.append(", ");
                    }
                }
            }
            if (!attributeHandled) {
                templateString.append(", ");
            }
        }
        if (!attributeHandled) {
            renderAttribute(templateString);
        }
        templateString.append(")");
        return templateString.toString();
    }

    private boolean hasArguments(List<Expression> annotationArguments) {
        return annotationArguments != null && !annotationArguments.isEmpty();
    }

    private void renderAttribute(StringBuilder templateString) {
        templateString.append(attribute.trim());
        templateString.append(" = ");
        templateString.append(renderValue(value, valueType));
    }

    private boolean hasMoreElements(List<Expression> annotationArguments, Expression exp) {
        return annotationArguments.indexOf(exp) < annotationArguments.size() - 1;
    }

    private String renderValue(Object value, Class valueType) {
        if (valueType == String.class) {
            return "\"" + value.toString().trim() + "\"";
        } else if (value == Character.class) {
            return "'" + value + "'";
        }
        return value.toString();
    }
}