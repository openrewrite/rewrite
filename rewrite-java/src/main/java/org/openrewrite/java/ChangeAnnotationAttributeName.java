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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;
import static org.openrewrite.java.tree.Space.SINGLE_SPACE;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeAnnotationAttributeName extends Recipe {

    @Option(displayName = "Annotation Type",
            description = "The fully qualified name of the annotation.",
            example = "org.junit.Test")
    String annotationType;

    @Option(displayName = "Old attribute name",
            description = "The name of attribute to change.",
            example = "timeout")
    String oldAttributeName;

    @Option(displayName = "New attribute name",
            description = "The new attribute name to use.",
            example = "waitFor")
    String newAttributeName;

    String displayName = "Change annotation attribute name";

    @Override
    public String getInstanceNameSuffix() {
        String shortType = annotationType.substring(annotationType.lastIndexOf('.') + 1);
        return String.format("`@%s(%s)` to `@%s(%s)`",
                shortType, oldAttributeName,
                shortType, newAttributeName);
    }

    String description = "Some annotations accept arguments. This recipe renames an existing attribute.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(UsesType.named(annotationType), new JavaIsoVisitor<ExecutionContext>() {
            private final AnnotationMatcher annotationMatcher = new AnnotationMatcher('@' + annotationType);

            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                J.Annotation a = super.visitAnnotation(annotation, ctx);
                if (!annotationMatcher.matches(a)) {
                    return a;
                }
                // the parser types an attribute's name as the annotation's method of that name
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(a.getType());
                JavaType.Method attribute = TypeUtils.findDeclaredMethod(type, newAttributeName, emptyList()).orElse(null);
                return a.withArguments(ListUtils.map(a.getArguments(), arg -> {
                    if (arg instanceof J.Assignment) {
                        if (!oldAttributeName.equals(newAttributeName)) {
                            J.Assignment assignment = (J.Assignment) arg;
                            J.Identifier variable = (J.Identifier) assignment.getVariable();
                            if (oldAttributeName.equals(variable.getSimpleName())) {
                                variable = variable.withSimpleName(newAttributeName);
                                JavaType.Method renamed = attribute == null ? withNewName(variable.getType()) : attribute;
                                return renamed == null ? assignment.withVariable(variable) :
                                        assignment.withVariable(variable.withType(renamed))
                                                .withType(renamed.getReturnType());
                            }
                        }
                    } else if ("value".equals(oldAttributeName) && !(arg instanceof J.Empty)) {
                        JavaType.Method renamed = attribute == null ?
                                withNewName(TypeUtils.findDeclaredMethod(type, "value", emptyList()).orElse(null)) : attribute;
                        J.Identifier name = new J.Identifier(randomId(), arg.getPrefix(), Markers.EMPTY, emptyList(), newAttributeName,
                                renamed == null ? arg.getType() : renamed, null);
                        return new J.Assignment(randomId(), EMPTY, arg.getMarkers(), name, new JLeftPadded<>(SINGLE_SPACE, arg.withPrefix(SINGLE_SPACE), Markers.EMPTY),
                                renamed == null ? arg.getType() : renamed.getReturnType());
                    }
                    return arg;
                }));
            }

            /**
             * An annotation type can lack the new attribute, such as one a later {@link ChangeType} replaces,
             * so the old attribute's method stands in for it under the new name.
             */
            private JavaType.@Nullable Method withNewName(@Nullable JavaType oldAttribute) {
                return oldAttribute instanceof JavaType.Method ?
                        ((JavaType.Method) oldAttribute).withName(newAttributeName) : null;
            }
        });
    }
}
