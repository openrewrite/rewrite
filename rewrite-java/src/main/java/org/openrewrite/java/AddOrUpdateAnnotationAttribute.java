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
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddOrUpdateAnnotationAttribute extends Recipe {
    @Option(displayName = "Annotation type",
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
    @Option(displayName = "Add only",
            description = "When set to `true` will not change existing annotation attribute values.")
    @Nullable
    Boolean addOnly;
    @Option(displayName = "Append array",
            description = "If the attribute is an array, setting this option to `true` will append the value(s). " +
                          "In conjunction with `addOnly`, it is possible to control duplicates:" +
                          "`addOnly=true`, always append. " +
                          "`addOnly=false`, only append if the value is not already present.")
    @Nullable
    Boolean appendArray;

    private static @Nullable String maybeQuoteStringArgument(@Nullable String attributeName, @Nullable String attributeValue, J.Annotation annotation) {
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

    private static boolean attributeValIsAlreadyPresentOrNull(List<Expression> expression, @Nullable String attributeValue) {
        for (Expression e : expression) {
            if (e instanceof J.Literal) {
                J.Literal literal = (J.Literal) e;
                if (literal.getValueSource() != null && literal.getValueSource().equals(attributeValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static J.Literal constructLiteral(String attributeValue, Space prefix) {
        return new J.Literal(randomId(), prefix, Markers.EMPTY, attributeValue, attributeValue, null, JavaType.Primitive.String);
    }

//    private static List<String> getAttributeListValues(List<String> attributeList, J.Annotation finalA) {
//        return attributeList.stream().map(attribute -> maybeQuoteStringArgument(finalA, attribute)).collect(Collectors.toList());
//    }

    @Override
    public String getDisplayName() {
        return "Add or update annotation attribute";
    }

    @Override
    public String getDescription() {
        return "Some annotations accept arguments. This recipe sets an existing argument to the specified value, " +
               "or adds the argument if it is not already set.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(annotationType, false), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext ctx) {
                J.Annotation original = a;
                if (!TypeUtils.isOfClassType(a.getType(), annotationType)) {
                    return a;
                }

                String newAttributeValue = maybeQuoteStringArgument(attributeName, attributeValue, a);
                List<Expression> currentArgs = a.getArguments();
                if (currentArgs == null || currentArgs.isEmpty() || currentArgs.get(0) instanceof J.Empty) {
                    if (newAttributeValue == null) {
                        return a;
                    }

                    if (attributeName == null || "value".equals(attributeName)) {
                        return JavaTemplate.builder("#{}")
                                .contextSensitive()
                                .build()
                                .apply(getCursor(), a.getCoordinates().replaceArguments(), newAttributeValue);
                    } else {
                        String newAttributeValueResult = newAttributeValue;
                        if (((JavaType.FullyQualified) Objects.requireNonNull(a.getAnnotationType().getType())).getMethods().stream().anyMatch(method -> method.getReturnType().toString().equals("java.lang.String[]"))) {
                            String attributeValueCleanedUp = attributeValue.replaceAll("\\s+", "").replaceAll("[\\s+{}\"]", "");
                            List<String> attributeList = Arrays.asList(attributeValueCleanedUp.contains(",") ? attributeValueCleanedUp.split(",") : new String[]{attributeValueCleanedUp});
                            newAttributeValueResult = attributeList.stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.joining("\", \"", "{\"", "\"}"));
                        }
                        return JavaTemplate.builder("#{} = #{}")
                                .contextSensitive()
                                .build()
                                .apply(getCursor(), a.getCoordinates().replaceArguments(), attributeName, newAttributeValueResult);
                    }
                } else {
                    // First assume the value exists amongst the arguments and attempt to update it
                    AtomicBoolean foundOrSetAttributeWithDesiredValue = new AtomicBoolean(false);
                    final J.Annotation finalA = a;
                    List<Expression> newArgs = ListUtils.map(currentArgs, it -> {
                        if (it instanceof J.Assignment) {
                            J.Assignment as = (J.Assignment) it;
                            J.Identifier var = (J.Identifier) as.getVariable();
                            if (attributeName == null || !attributeName.equals(var.getSimpleName())) {
                                return it;
                            }
                            foundOrSetAttributeWithDesiredValue.set(true);

                            if (newAttributeValue == null) {
                                return null;
                            }

                            if (as.getAssignment() instanceof J.NewArray) {
                                for (Marker m : as.getMarkers().getMarkers()) {
                                    if (m instanceof AlreadyAppended) {
                                        return as;
                                    }
                                }

                                List<Expression> jLiteralList = ((J.NewArray) as.getAssignment()).getInitializer();
                                String attributeValueCleanedUp = attributeValue.replaceAll("\\s+", "").replaceAll("[\\s+{}\"]", "");
                                List<String> attributeList = Arrays.asList(attributeValueCleanedUp.contains(",") ? attributeValueCleanedUp.split(",") : new String[]{attributeValueCleanedUp});
                                List<String> newAttributeListValues = attributeList.stream().map(attribute -> maybeQuoteStringArgument(attributeName, attribute, finalA)).collect(Collectors.toList());

                                if (Boolean.TRUE.equals(appendArray)) {
                                    if (Boolean.FALSE.equals(addOnly)) {
                                        newAttributeListValues = newAttributeListValues.stream()
                                                .filter(attribute -> !attributeValIsAlreadyPresentOrNull(jLiteralList, attribute)).collect(Collectors.toList());
                                    }

                                    // In case of annotation with empty array.
                                    if (jLiteralList.size() == 1 && jLiteralList.get(0) instanceof J.Empty) {
                                        jLiteralList.clear();
                                    }

                                    for (int i = 0, j = jLiteralList.size(); i < newAttributeListValues.size(); j++, i++) {
                                        Space prefix = (j == 0) ? Space.EMPTY : jLiteralList.get(j - 1).getPrefix();
                                        jLiteralList.add(j, new J.Literal(randomId(), prefix, Markers.EMPTY, newAttributeListValues.get(i), newAttributeListValues.get(i), null, JavaType.Primitive.String));
                                    }

                                    return as.withAssignment(((J.NewArray) as.getAssignment()).withInitializer(jLiteralList)).withMarkers(as.getMarkers().add(new AlreadyAppended(randomId(), newAttributeValue)));
                                } else if (Boolean.FALSE.equals(addOnly) || jLiteralList == null || jLiteralList.isEmpty()) {
                                    List<Expression> newAttributeLiterals = new ArrayList<>();
                                    boolean hasChanged = jLiteralList == null || newAttributeListValues.size() != jLiteralList.size();
                                    for (int i = 0; i < newAttributeListValues.size(); i++) {
                                        if (!hasChanged && !newAttributeListValues.get(i).equals(((J.Literal) jLiteralList.get(i)).getValueSource())) {
                                            hasChanged = true;
                                        }
                                        newAttributeLiterals.add(constructLiteral(newAttributeListValues.get(i), Space.EMPTY));
                                    }

                                    if (hasChanged) {
                                        return as.withAssignment(((J.NewArray) as.getAssignment())
                                                        .withInitializer(newAttributeLiterals))
                                                .withMarkers(as.getMarkers().add(new AlreadyAppended(randomId(), newAttributeValue)));
                                    }
                                    return as;
                                }
                                return as;
                            } else {
                                J.Literal value = (J.Literal) as.getAssignment();
                                if (newAttributeValue.equals(value.getValueSource()) || Boolean.TRUE.equals(addOnly)) {
                                    return it;
                                }
                                return as.withAssignment(value.withValue(newAttributeValue).withValueSource(newAttributeValue));
                            }
                        } else if (it instanceof J.Literal) {
                            // The only way anything except an assignment can appear is if there's an implicit assignment to "value"
                            if (attributeName == null || "value".equals(attributeName)) {
                                foundOrSetAttributeWithDesiredValue.set(true);
                                if (newAttributeValue == null) {
                                    return null;
                                }
                                J.Literal value = (J.Literal) it;
                                if (newAttributeValue.equals(value.getValueSource()) || Boolean.TRUE.equals(addOnly)) {
                                    return it;
                                }
                                return ((J.Literal) it).withValue(newAttributeValue).withValueSource(newAttributeValue);
                            } else {
                                // Make the attribute name explicit, before we add the new value below
                                //noinspection ConstantConditions
                                return ((J.Annotation) JavaTemplate.builder("value = #{}")
                                        .contextSensitive()
                                        .build()
                                        .apply(getCursor(), finalA.getCoordinates().replaceArguments(), it)
                                ).getArguments().get(0);
                            }
                        } else if (it instanceof J.FieldAccess) {
                            // The only way anything except an assignment can appear is if there's an implicit assignment to "value"
                            if (attributeName == null || "value".equals(attributeName)) {
                                foundOrSetAttributeWithDesiredValue.set(true);
                                if (newAttributeValue == null) {
                                    return null;
                                }
                                J.FieldAccess value = (J.FieldAccess) it;
                                if (newAttributeValue.equals(value.toString()) || Boolean.TRUE.equals(addOnly)) {
                                    return it;
                                }
                                //noinspection ConstantConditions
                                return ((J.Annotation) JavaTemplate.apply(newAttributeValue, getCursor(), finalA.getCoordinates().replaceArguments()))
                                        .getArguments().get(0);
                            } else {
                                // Make the attribute name explicit, before we add the new value below
                                //noinspection ConstantConditions
                                return ((J.Annotation) JavaTemplate.builder("value = #{any()}")
                                        .contextSensitive()
                                        .build()
                                        .apply(getCursor(), finalA.getCoordinates().replaceArguments(), it))
                                        .getArguments().get(0);
                            }
                        }
                        return it;
                    });
                    if (newArgs != currentArgs) {
                        a = a.withArguments(newArgs);
                    }
                    if (foundOrSetAttributeWithDesiredValue.get()) {
                        a = maybeAutoFormat(original, a, ctx);
                        return a;
                    }
                    // There was no existing value to update, so add a new value into the argument list
                    String effectiveName = (attributeName == null) ? "value" : attributeName;
                    //noinspection ConstantConditions
                    J.Assignment as = (J.Assignment) ((J.Annotation) JavaTemplate.builder("#{} = #{}")
                            .contextSensitive()
                            .build()
                            .apply(getCursor(), a.getCoordinates().replaceArguments(), effectiveName, newAttributeValue))
                            .getArguments().get(0);
                    a = a.withArguments(ListUtils.concat(as, a.getArguments()));
                    a = maybeAutoFormat(original, a, ctx);
                }

                return a;
            }
        });
    }

    @Value
    @With
    private static class AlreadyAppended implements Marker {
        UUID id;
        String values;
    }
}
