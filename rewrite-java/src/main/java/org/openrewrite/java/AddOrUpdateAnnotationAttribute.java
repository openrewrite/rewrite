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
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.marker.Markers.EMPTY;

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
            description = "The value to set the attribute to. If the attribute is an array, provide values separated by comma to add multiple attributes at once. Set to `null` to remove the attribute.",
            required = false,
            example = "500")
    @Nullable
    String attributeValue;

    @Option(displayName = "Old Attribute value",
            description = "The current value of the attribute, this can be used to filter where the change is applied. Set to `null` for wildcard behavior.",
            required = false,
            example = "400")
    @Nullable
    String oldAttributeValue;

    @Option(displayName = "Add only",
            description = "If `true`, disables upgrading existing annotation attribute values, thus the recipe will only add the attribute if it does not already exist. " +
                    "If omitted or `false`, the recipe adds the attribute if missing or updates its value if present.",
            required = false)
    @Nullable
    Boolean addOnly;

    @Option(displayName = "Append array",
            description = "If the attribute is an array and attribute is present, setting this option to `true` will append the value(s). Duplicate values will not be added. " +
                    "Otherwise, the recipe will replace the existing value(s) with the new value(s).",
            required = false)
    @Nullable
    Boolean appendArray;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(annotationType, false), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext ctx) {
                J.Annotation original = super.visitAnnotation(a, ctx);
                if (!TypeUtils.isOfClassType(a.getType(), annotationType)) {
                    return original;
                }

                String newAttributeValue = maybeQuoteStringArgument(a, attributeValue);
                List<Expression> currentArgs = a.getArguments();
                // ADD the value when the annotation has no arguments, like `@Foo`
                if (currentArgs == null || currentArgs.isEmpty() || currentArgs.get(0) instanceof J.Empty) {
                    if (newAttributeValue == null || oldAttributeValue != null) {
                        return a;
                    }

                    if (attributeName == null || "value".equals(attributeName)) {
                        return JavaTemplate
                                .apply("#{}", getCursor(), a.getCoordinates().replaceArguments(), newAttributeValue);
                    }

                    String newAttributeValueResult = attributeIsArray(a) ? getAttributeValuesAsString() : newAttributeValue;
                    return JavaTemplate
                            .apply("#{} = #{}", getCursor(), a.getCoordinates().replaceArguments(), attributeName, newAttributeValueResult);
                } else if (!TRUE.equals(addOnly)) {
                    // UPDATE the value when the annotation has arguments, like `@Foo(name="example")`
                    AtomicBoolean foundOrSetAttributeWithDesiredValue = new AtomicBoolean(false);
                    final J.Annotation finalA = a;
                    List<Expression> newArgs = ListUtils.map(currentArgs, it -> {
                        if (it instanceof J.Assignment) {
                            J.Assignment as = (J.Assignment) it;
                            J.Identifier var_ = (J.Identifier) as.getVariable();
                            if ((attributeName == null && !"value".equals(var_.getSimpleName())) || (attributeName != null && !attributeName.equals(var_.getSimpleName()))) {
                                return it;
                            }
                            foundOrSetAttributeWithDesiredValue.set(true);
                            if (newAttributeValue == null) {
                                return null;
                            }
                            if (as.getAssignment() instanceof J.NewArray) {
                                List<Expression> initializerList = requireNonNull(((J.NewArray) as.getAssignment()).getInitializer());
                                updateInitializerList(finalA, initializerList, getAttributeValues());
                                return as.withAssignment(((J.NewArray) as.getAssignment()).withInitializer(initializerList));
                            } else {
                                Expression exp = as.getAssignment();
                                if (exp instanceof J.Literal) {
                                    J.Literal literal = (J.Literal) exp;
                                    if (!valueMatches(literal, oldAttributeValue) || newAttributeValue.equals(literal.getValueSource())) {
                                        return it;
                                    }
                                    if (attributeIsArray(finalA)) {
                                        //noinspection ConstantConditions
                                        return as.withAssignment(((J.Annotation) JavaTemplate
                                                .apply("#{}", getCursor(), finalA.getCoordinates().replaceArguments(), getAttributeValuesAsString()))
                                                .getArguments().get(0));
                                    }
                                    return as.withAssignment(literal.withValue(newAttributeValue).withValueSource(newAttributeValue));
                                } else if (exp instanceof J.FieldAccess) {
                                    if (oldAttributeValue != null) {
                                        return it;
                                    }
                                    //noinspection ConstantConditions
                                    return ((J.Annotation) JavaTemplate
                                            .apply("#{} = #{}", getCursor(), as.getCoordinates().replace(), var_.getSimpleName(), newAttributeValue))
                                            .getArguments().get(finalA.getArguments().indexOf(as));
                                }
                            }
                        } else if (it instanceof J.Literal) {
                            // The only way anything except an assignment can appear is if there's an implicit assignment to "value"
                            if (attributeName == null || "value".equals(attributeName)) {
                                foundOrSetAttributeWithDesiredValue.set(true);
                                if (newAttributeValue == null) {
                                    return null;
                                }
                                J.Literal literal = (J.Literal) it;
                                if (!valueMatches(literal, oldAttributeValue) || newAttributeValue.equals(literal.getValueSource())) {
                                    return it;
                                }
                                if (attributeIsArray(finalA)) {
                                    //noinspection ConstantConditions
                                    return ((J.Annotation) JavaTemplate
                                            .apply("#{}", getCursor(), finalA.getCoordinates().replaceArguments(), getAttributeValuesAsString()))
                                            .getArguments().get(0);
                                }
                                return literal.withValue(newAttributeValue).withValueSource(newAttributeValue);
                            } else if (oldAttributeValue == null) {
                                // Without an oldAttributeValue and an attributeName not matching `value` we want to add an extra argument to the annotation.
                                // Make the attribute name explicit, before we add the new value below
                                return createAnnotationAssignment(finalA, "value", it);
                            }
                        } else if (it instanceof J.FieldAccess) {
                            // The only way anything except an assignment can appear is if there's an implicit assignment to "value"
                            if (attributeName == null || "value".equals(attributeName)) {
                                foundOrSetAttributeWithDesiredValue.set(true);
                                if (newAttributeValue == null) {
                                    return null;
                                }
                                if (!valueMatches(it, oldAttributeValue) || newAttributeValue.equals(((J.FieldAccess) it).toString())) {
                                    return it;
                                }
                                //noinspection ConstantConditions
                                return ((J.Annotation) JavaTemplate
                                        .apply(newAttributeValue, getCursor(), finalA.getCoordinates().replaceArguments()))
                                        .getArguments().get(0);
                            }
                            // Make the attribute name explicit, before we add the new value below
                            return createAnnotationAssignment(finalA, "value", it);
                        } else if (it instanceof J.NewArray) {
                            if (newAttributeValue == null) {
                                return null;
                            }
                            if (attributeName != null && !attributeValue.equals("value")) {
                                return isAnnotationWithOnlyValueMethod(finalA) ? it : createAnnotationAssignment(finalA, "value", it);
                            }
                            J.NewArray arrayValue = (J.NewArray) it;
                            List<Expression> initializerList = requireNonNull(arrayValue.getInitializer());
                            updateInitializerList(finalA, initializerList, getAttributeValues());
                            return arrayValue.withInitializer(initializerList);
                        }
                        return it;
                    });

                    if (newArgs != currentArgs) {
                        a = a.withArguments(newArgs);
                    }
                    if (!foundOrSetAttributeWithDesiredValue.get() && !attributeValIsAlreadyPresent(newArgs, newAttributeValue) && oldAttributeValue == null && !isAnnotationWithOnlyValueMethod(a)) {
                        // There was no existing value to update and no requirements on a pre-existing old value, so add a new value into the argument list
                        J.Assignment as = createAnnotationAssignment(a, attributeName(), newAttributeValue);
                        a = a.withArguments(ListUtils.concat(as, a.getArguments()));
                    }
                }

                return maybeAutoFormat(original, a, ctx);
            }

            private J.Assignment createAnnotationAssignment(J.Annotation annotation, String name, @Nullable Object parameter) {
                //noinspection ConstantConditions
                return (J.Assignment) ((J.Annotation) JavaTemplate
                        .apply(name + " = " + (parameter instanceof J ? "#{any()}" : "#{}"), getCursor(), annotation.getCoordinates().replaceArguments(), parameter))
                        .getArguments().get(0);
            }
        });
    }

    private String attributeName() {
        return attributeName == null ? "value" : attributeName;
    }

    private void updateInitializerList(J.Annotation finalA, List<Expression> initializerList, List<String> attributeList) {
        // If `oldAttributeValue` is defined, replace the old value with the new value(s). Ignore the `appendArray` option in this case.
        if (oldAttributeValue != null) {
            for (int i = 0; i < initializerList.size(); i++) {
                // TODO: support `oldAttributeValue` with multiple values (just like `attributeValue` can have multiple values)
                if (initializerList.get(i) instanceof J.Literal && oldAttributeValue.equals(((J.Literal) initializerList.get(i)).getValue())) {
                    initializerList.remove(i);
                    for (int j = 0; j < attributeList.size(); j++) {
                        String newAttributeListValue = maybeQuoteStringArgument(finalA, attributeList.get(j));
                        J.Literal newLiteral = new J.Literal(randomId(), initializerList.get(i).getPrefix(), EMPTY, newAttributeListValue, newAttributeListValue, null, JavaType.Primitive.String);
                        initializerList.add(i + j, newLiteral);
                    }
                    i--;
                }
            }
            return;
        }

        // If `appendArray` is true, add the new value(s) to the existing array.
        if (TRUE.equals(appendArray)) {
            for (String attribute : attributeList) {
                String newAttributeListValue = maybeQuoteStringArgument(finalA, attribute);
                if (attributeValIsAlreadyPresent(initializerList, newAttributeListValue)) {
                    continue;
                }

                J.Literal newLiteral = new J.Literal(randomId(), Space.SINGLE_SPACE, EMPTY, newAttributeListValue, newAttributeListValue, null, JavaType.Primitive.String);
                initializerList.add(newLiteral);
            }
            return;
        }

        // If no option is defined, replace the old array elements with the new elements. The logic is a little complicated, to make sure the second time the recipe is run, no changes are performed on the initializerList.
        for (int i = 0; i < initializerList.size(); i++) {
            if (i >= attributeList.size()) {
                initializerList.remove(i);
                i--;
                continue;
            }

            String newAttributeListValue = maybeQuoteStringArgument(finalA, attributeList.get(i));
            if (attributeValIsAlreadyPresent(initializerList, newAttributeListValue)) {
                continue;
            }

            J.Literal newLiteral = new J.Literal(randomId(), initializerList.get(i).getPrefix(), EMPTY, newAttributeListValue, newAttributeListValue, null, JavaType.Primitive.String);
            initializerList.set(i, newLiteral);
        }
        for (int j = initializerList.size(); j < attributeList.size(); j++) {
            String newAttributeListValue = maybeQuoteStringArgument(finalA, attributeList.get(j));
            initializerList.add(new J.Literal(randomId(), Space.SINGLE_SPACE, EMPTY, newAttributeListValue, newAttributeListValue, null, JavaType.Primitive.String));
        }
    }

    private List<String> getAttributeValues() {
        if (attributeValue == null) {
            return emptyList();
        }
        String attributeValueCleanedUp = attributeValue.replaceAll("\\s+", "").replaceAll("[\\s+{}\"]", "");
        return Arrays.asList(attributeValueCleanedUp.contains(",") ? attributeValueCleanedUp.split(",") : new String[]{attributeValueCleanedUp});
    }

    private String getAttributeValuesAsString() {
        return getAttributeValues().stream()
                .map(String::valueOf)
                .collect(Collectors.joining("\", \"", "{\"", "\"}"));
    }

    private static boolean isAnnotationWithOnlyValueMethod(J.Annotation annotation) {
        return getMethods(annotation).size() == 1 && getMethods(annotation).get(0).getName().equals("value");
    }

    private static boolean valueMatches(@Nullable Expression expression, @Nullable String oldAttributeValue) {
        if (expression == null) {
            return oldAttributeValue == null;
        } else if (oldAttributeValue == null) { // null means wildcard
            return true;
        } else if (expression instanceof J.Literal) {
            return oldAttributeValue.equals(((J.Literal) expression).getValue());
        } else if (expression instanceof J.FieldAccess) {
            J.FieldAccess fa = (J.FieldAccess) expression;
            String currentValue = ((J.Identifier) fa.getTarget()).getSimpleName() + "." + fa.getSimpleName();
            return oldAttributeValue.equals(currentValue);
        } else if (expression instanceof J.Identifier) { // class names, static variables, ...
            if (oldAttributeValue.endsWith(".class")) {
                String className = TypeUtils.toString(requireNonNull(expression.getType())) + ".class";
                return className.endsWith(oldAttributeValue);
            }
            return oldAttributeValue.equals(((J.Identifier) expression).getSimpleName());
        }
        throw new IllegalArgumentException("Unexpected expression type: " + expression.getClass());
    }

    @Contract("_, null -> null; _, !null -> !null")
    private @Nullable String maybeQuoteStringArgument(J.Annotation annotation, @Nullable String attributeValue) {
        if (attributeValue != null && attributeIsString(annotation)) {
            return "\"" + attributeValue + "\"";
        }
        return attributeValue;
    }

    private boolean attributeIsArray(J.Annotation annotation) {
        for (JavaType.Method m : getMethods(annotation)) {
            if (attributeName().equals(m.getName())) {
                return m.getReturnType() instanceof JavaType.Array;
            }
        }
        return false;
    }

    private boolean attributeIsString(J.Annotation annotation) {
        for (JavaType.Method m : getMethods(annotation)) {
            if (attributeName().equals(m.getName())) {
                return TypeUtils.isOfClassType(m.getReturnType(), "java.lang.String");
            }
        }
        return false;
    }

    private static List<JavaType.Method> getMethods(J.Annotation annotation) {
        return ((JavaType.FullyQualified) requireNonNull(annotation.getAnnotationType().getType())).getMethods();
    }

    private static boolean attributeValIsAlreadyPresent(@Nullable List<Expression> expression, @Nullable String attributeValue) {
        if (expression == null) {
            return attributeValue == null;
        }
        for (Expression e : expression) {
            if (e instanceof J.Literal) {
                J.Literal literal = (J.Literal) e;
                if (literal.getValueSource() != null && literal.getValueSource().equals(attributeValue)) {
                    return true;
                }
            }
            if (e instanceof J.NewArray) {
                return attributeValIsAlreadyPresent(((J.NewArray) e).getInitializer(), attributeValue);
            }
        }
        return false;
    }
}
