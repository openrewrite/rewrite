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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddOrUpdateAnnotationAttribute extends Recipe {
    @Override
    public String getDisplayName() {
        return "Add or update annotation attribute";
    }

    @Override
    public String getDescription() {
        return "Some annotations accept arguments. This recipe sets an existing argument to the specified value, " +
               "or adds the argument if it is not already set.";
    }

    @Option(displayName = "Annotation Type",
            description = "The fully qualified name of the annotation.",
            example = "org.junit.Test")
    String annotationType;

    @Option(displayName = "Attribute name",
            description = "The name of attribute to change. If omitted defaults to 'value'.",
            required = false,
            example = "timeout")
    @Nullable
    String attributeName;

    @Option(displayName = "Attribute value",
            description = "The value to set the attribute to. Set to `null` to remove the attribute.",
            example = "500")
    @Nullable
    String attributeValue;

    @Option(displayName = "Add Only",
            description = "When set to `true` will not change existing annotation attribute values.")
    @Nullable
    Boolean addOnly;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(annotationType, false), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext ctx) {
                if (!TypeUtils.isOfClassType(a.getType(), annotationType)) {
                    return a;
                }

                String newAttributeValue = maybeQuoteStringArgument(attributeName, attributeValue, a);
                List<Expression> currentArgs = a.getArguments();
                if (currentArgs == null || currentArgs.isEmpty()) {
                    if (newAttributeValue == null) {
                        return a;
                    }

                    if (attributeName == null || "value".equals(attributeName)) {
                        return JavaTemplate.builder("#{}")
                                .contextSensitive()
                                .build()
                                .apply(getCursor(), a.getCoordinates().replaceArguments(), newAttributeValue);
                    } else {
                        return JavaTemplate.builder("#{} = #{}")
                                .contextSensitive()
                                .build()
                                .apply(getCursor(), a.getCoordinates().replaceArguments(), attributeName, newAttributeValue);
                    }
                } else {
                    // First assume the value exists amongst the arguments and attempt to update it
                    AtomicBoolean foundAttributeWithDesiredValue = new AtomicBoolean(false);
                    final J.Annotation finalA = a;
                    List<Expression> newArgs = ListUtils.map(currentArgs, it -> {
                        if (it instanceof J.Assignment) {
                            J.Assignment as = (J.Assignment) it;
                            J.Identifier var = (J.Identifier) as.getVariable();
                            if (attributeName == null || !attributeName.equals(var.getSimpleName())) {
                                return it;
                            }
                            J.Literal value = (J.Literal) as.getAssignment();
                            if (newAttributeValue == null) {
                                return null;
                            }
                            if (newAttributeValue.equals(value.getValueSource()) || Boolean.TRUE.equals(addOnly)) {
                                foundAttributeWithDesiredValue.set(true);
                                return it;
                            }
                            return as.withAssignment(value.withValue(newAttributeValue).withValueSource(newAttributeValue));
                        } else if (it instanceof J.Literal) {
                            // The only way anything except an assignment can appear is if there's an implicit assignment to "value"
                            if (attributeName == null || "value".equals(attributeName)) {
                                if (newAttributeValue == null) {
                                    return null;
                                }
                                J.Literal value = (J.Literal) it;
                                if (newAttributeValue.equals(value.getValueSource()) || Boolean.TRUE.equals(addOnly)) {
                                    foundAttributeWithDesiredValue.set(true);
                                    return it;
                                }
                                return ((J.Literal) it).withValue(newAttributeValue).withValueSource(newAttributeValue);
                            } else {
                                //noinspection ConstantConditions
                                return ((J.Annotation) JavaTemplate.builder("value = #{}")
                                        .contextSensitive()
                                        .build()
                                        .apply(getCursor(), finalA.getCoordinates().replaceArguments(), it)
                                ).getArguments().get(0);
                            }
                        }
                        return it;
                    });
                    if (foundAttributeWithDesiredValue.get() || newArgs != currentArgs) {
                        return a.withArguments(newArgs);
                    }
                    // There was no existing value to update, so add a new value into the argument list
                    String effectiveName = (attributeName == null) ? "value" : attributeName;
                    //noinspection ConstantConditions
                    J.Assignment as = (J.Assignment) ((J.Annotation) JavaTemplate.builder("#{} = #{}")
                            .contextSensitive()
                            .build()
                            .apply(getCursor(), a.getCoordinates().replaceArguments(), effectiveName, newAttributeValue)
                    ).getArguments().get(0);
                    List<Expression> newArguments = ListUtils.concat(as, a.getArguments());
                    a = a.withArguments(newArguments);
                    a = autoFormat(a, ctx);
                }

                return a;
            }
        });
    }

    @Nullable
    private static String maybeQuoteStringArgument(@Nullable String attributeName, @Nullable String attributeValue, J.Annotation annotation) {
        if ((attributeValue != null) && attributeIsString(attributeName, annotation)) {
            return "\"" + attributeValue + "\"";
        } else {
            return attributeValue;
        }
    }

    private static boolean attributeIsString(@Nullable String attributeName, J.Annotation annotation) {
        String actualAttributeName = (attributeName == null) ? "value" : attributeName;
        JavaType.Class annotationType = (JavaType.Class) annotation.getType();
        if (annotationType != null) {
            for (JavaType.Method m : annotationType.getMethods()) {
                if (m.getName().equals(actualAttributeName)) {
                    return TypeUtils.isOfClassType(m.getReturnType(), "java.lang.String");
                }
            }
        }
        return false;
    }
}
