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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import java.util.concurrent.atomic.AtomicBoolean;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveAnnotationAttribute extends Recipe {

    @Option(displayName = "Annotation Type",
            description = "The fully qualified name of the annotation.",
            example = "org.junit.Test")
    String annotationType;

    @Option(displayName = "Attribute name",
            description = "The name of attribute to remove.",
            example = "timeout")
    String attributeName;

    @Override
    public String getDisplayName() {
        return "Remove annotation attribute";
    }

    @Override
    public String getInstanceNameSuffix() {
        String shortType = annotationType.substring(annotationType.lastIndexOf('.') + 1);
        return String.format("`@%s(%s)`", shortType, attributeName);
    }

    @Override
    public String getDescription() {
        return "Some annotations accept arguments. This recipe removes an existing attribute.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(annotationType, false), new JavaIsoVisitor<ExecutionContext>() {
            private final AnnotationMatcher annotationMatcher = new AnnotationMatcher(annotationType);

            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                J.Annotation a = super.visitAnnotation(annotation, ctx);
                if (!annotationMatcher.matches(a)) {
                    return a;
                }

                AtomicBoolean didPassFirstAttribute = new AtomicBoolean(false);
                AtomicBoolean shouldTrimNextPrefix = new AtomicBoolean(false);
                return a.withArguments(ListUtils.map(a.getArguments(), arg -> {
                    try {
                        if (arg instanceof J.Assignment) {
                            J.Assignment assignment = (J.Assignment) arg;
                            J.Identifier variable = (J.Identifier) assignment.getVariable();
                            if (attributeName.equals(variable.getSimpleName())) {
                                if (!didPassFirstAttribute.get()) {
                                    shouldTrimNextPrefix.set(true);
                                }
                                return null;
                            }
                        } else if (attributeName.equals("value")) {
                            if (!didPassFirstAttribute.get()) {
                                shouldTrimNextPrefix.set(true);
                            }
                            return null;
                        }

                        if (shouldTrimNextPrefix.get()) {
                            shouldTrimNextPrefix.set(false);
                            return arg.withPrefix(arg.getPrefix().withWhitespace(""));
                        }
                    } finally {
                        didPassFirstAttribute.set(true);
                    }

                    return arg;
                }));
            }
        });
    }
}
