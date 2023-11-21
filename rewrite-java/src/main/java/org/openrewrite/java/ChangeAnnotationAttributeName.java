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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeAnnotationAttributeName extends Recipe {
    @Override
    public String getDisplayName() {
        return "Change annotation attribute name";
    }

    @Override
    public String getDescription() {
        return "Some annotations accept arguments. This recipe renames an existing attribute.";
    }

    @Option(displayName = "Annotation Type",
            description = "The fully qualified name of the annotation.",
            example = "org.junit.Test")
    String annotationType;

    @Option(displayName = "Old attribute name",
            description = "The name of attribute to change.",
            required = false,
            example = "timeout")
    String oldAttributeName;

    @Option(displayName = "New attribute name",
            description = "The new attribute name to use.",
            example = "waitFor")
    String newAttributeName;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(annotationType, false), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext context) {
                J.Annotation a = super.visitAnnotation(annotation, context);
                if (!new AnnotationMatcher(annotationType).matches(a)) {
                    return a;
                }
                return a.withArguments(ListUtils.map(a.getArguments(), arg -> {
                    if (arg instanceof J.Assignment) {
                        J.Assignment assignment = (J.Assignment) arg;
                        J.Identifier variable = (J.Identifier) assignment.getVariable();
                        if (oldAttributeName.equals(variable.getSimpleName())) {
                            return assignment.withVariable(variable.withSimpleName(newAttributeName));
                        }
                    }
                    return arg;
                }));
            }
        });
    }
}
