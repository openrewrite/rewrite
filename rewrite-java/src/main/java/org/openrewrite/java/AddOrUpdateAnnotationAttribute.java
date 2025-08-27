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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.*;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.SINGLE_SPACE;
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
                    "If omitted or `false`, the recipe will replace the existing value(s) with the new value(s).",
            required = false)
    @Nullable
    Boolean appendArray;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(annotationType, false), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation original, ExecutionContext ctx) {
                J.Annotation a = super.visitAnnotation(original, ctx);
                if (!TypeUtils.isOfClassType(a.getType(), annotationType) ||
                        !(a.getType() instanceof JavaType.ShallowClass || findMethod(a, attributeName()).isPresent())) {
                    return a;
                }

                String newAttributeValue;
                if (attributeValue != null && attributeValue.endsWith(".class") && StringUtils.countOccurrences(attributeValue, ".") > 1) {
                    maybeAddImport(attributeValue.substring(0, attributeValue.length() - 6));
                    newAttributeValue = attributeValue;
                } else {
                    newAttributeValue = maybeQuoteStringArgument(a, attributeValue);
                }
                List<Expression> currentArgs = a.getArguments();

                // ADD the value when the annotation has no arguments, e.g. @Foo` to @Foo(name="new")
                if (currentArgs == null || currentArgs.isEmpty() || currentArgs.get(0) instanceof J.Empty) {
                    if (newAttributeValue == null || oldAttributeValue != null) {
                        return a;
                    }
                    if ("value".equals(attributeName())) {
                        return JavaTemplate.apply("#{}", getCursor(), a.getCoordinates().replaceArguments(), newAttributeValue);
                    }
                    String attrVal = newAttributeValue.contains(",") && attributeIsArray(a) ? getAttributeValuesAsString() : newAttributeValue;
                    return JavaTemplate.apply("#{} = #{}", getCursor(), a.getCoordinates().replaceArguments(), attributeName, attrVal);
                }

                // UPDATE the value when the annotation has arguments, e.g. @Foo(name="old") to `@Foo(name="new")
                if (!TRUE.equals(addOnly)) {
                    final J.Annotation finalA = a;
                    a = a.withArguments(ListUtils.map(currentArgs, it -> {
                        if (it instanceof J.Assignment) {
                            return update((J.Assignment) it, finalA, newAttributeValue);
                        } else if (it instanceof J.Literal) {
                            return update((J.Literal) it, finalA, newAttributeValue);
                        } else if (it instanceof J.FieldAccess) {
                            return update((J.FieldAccess) it, finalA, newAttributeValue);
                        } else if (it instanceof J.NewArray) {
                            return update((J.NewArray) it, finalA, newAttributeValue);
                        }
                        return it;
                    }));
                }

                // ADD the value into the argument list when there was no existing value to update and no requirements on a pre-existing old value, e.g. @Foo(name="old") to @Foo(value="new", name="old")
                if (oldAttributeValue == null && newAttributeValue != null && !attributeNameOrValIsAlreadyPresent(a.getArguments(), getAttributeValues())) {
                    J.Assignment as = createAnnotationAssignment(a, attributeName(), newAttributeValue);
                    List<Expression> args = a.getArguments();
                    // Case for existing attribute: `@Foo("q")` -> @Foo(value = "q")
                    if (args.size() == 1 && !(args.get(0) instanceof J.Assignment)) {
                        args = singletonList(createAnnotationAssignment(a, "value", a.getArguments().get(0)));
                    }
                    a = a.withArguments(ListUtils.concat(as, args));
                }

                if (original != a) {
                    doAfterVisit(new SimplifySingleElementAnnotation().getVisitor());
                }
                return maybeAutoFormat(original, a, ctx);
            }

            private @Nullable Expression update(J.Assignment as, J.Annotation annotation, @Nullable String newAttributeValue) {
                J.Identifier var_ = (J.Identifier) as.getVariable();
                if ((attributeName == null && !"value".equals(var_.getSimpleName())) ||
                        (attributeName != null && !attributeName.equals(var_.getSimpleName()))) {
                    return as;
                }
                if (newAttributeValue == null) {
                    return null;
                }
                Expression exp = as.getAssignment();
                if (exp instanceof J.NewArray) {
                    List<Expression> initializerList = requireNonNull(((J.NewArray) exp).getInitializer());
                    return as.withAssignment(((J.NewArray) exp)
                            .withInitializer(updateInitializer(annotation, initializerList, getAttributeValues())));
                }
                if (exp instanceof J.Literal) {
                    if (!valueMatches(exp, oldAttributeValue) || newAttributeValue.equals(((J.Literal) exp).getValueSource())) {
                        return as;
                    }
                    return as.withAssignment(createAnnotationLiteral(annotation, newAttributeValue));
                }
                if (exp instanceof J.FieldAccess) {
                    if (oldAttributeValue != null) {
                        return as;
                    }
                    if (isFullyQualifiedClass() && getFullyQualifiedClass(newAttributeValue).equals(exp.toString())) {
                        return as;
                    }
                    //noinspection ConstantConditions
                    return JavaTemplate.<J.Annotation>apply("#{} = #{}", getCursor(), as.getCoordinates().replace(), var_.getSimpleName(), newAttributeValue)
                            .getArguments().get(annotation.getArguments().indexOf(as));
                }
                return as;
            }

            private @Nullable Expression update(J.Literal literal, J.Annotation annotation, @Nullable String newAttributeValue) {
                // The only way anything except an assignment can appear is if there's an implicit assignment to "value"
                if ("value".equals(attributeName())) {
                    if (newAttributeValue == null) {
                        return null;
                    }
                    if (!valueMatches(literal, oldAttributeValue) || newAttributeValue.equals(literal.getValueSource())) {
                        return literal;
                    }
                    return createAnnotationLiteral(annotation, newAttributeValue);
                }
                if (oldAttributeValue == null && newAttributeValue != null) {
                    // Without an oldAttributeValue and an attributeName not matching `value` we want to add an extra argument to the annotation.
                    // Make the attribute name explicit, before we add the new value below
                    return createAnnotationAssignment(annotation, "value", literal);
                }
                return literal;
            }

            private @Nullable Expression update(J.FieldAccess fieldAccess, J.Annotation annotation, @Nullable String newAttributeValue) {
                // The only way anything except an assignment can appear is if there's an implicit assignment to "value"
                if ("value".equals(attributeName())) {
                    if (newAttributeValue == null) {
                        return null;
                    }
                    if (isFullyQualifiedClass() && getFullyQualifiedClass(newAttributeValue).equals(fieldAccess.toString())) {
                        return fieldAccess;
                    }
                    if (!valueMatches(fieldAccess, oldAttributeValue) || newAttributeValue.equals(fieldAccess.toString())) {
                        return fieldAccess;
                    }
                    String attrVal = newAttributeValue.contains(",") && attributeIsArray(annotation) ?
                            getAttributeValues().stream().map(String::valueOf).collect(joining(",", "{", "}")) :
                            newAttributeValue;
                    //noinspection ConstantConditions
                    return JavaTemplate.<J.Annotation>apply("#{}", getCursor(), annotation.getCoordinates().replaceArguments(), attrVal)
                            .getArguments().get(0);
                }
                // Make the attribute name explicit, before we add the new value below
                return createAnnotationAssignment(annotation, "value", fieldAccess);
            }

            private @Nullable Expression update(J.NewArray arrayValue, J.Annotation annotation, @Nullable String newAttributeValue) {
                if (newAttributeValue == null) {
                    return null;
                }
                if (attributeName != null && !"value".equals(attributeValue)) {
                    return isAnnotationWithOnlyValueMethod(annotation) ? arrayValue : createAnnotationAssignment(annotation, "value", arrayValue);
                }
                return arrayValue.withInitializer(updateInitializer(annotation, requireNonNull(arrayValue.getInitializer()), getAttributeValues()));
            }

            private Expression createAnnotationLiteral(J.Annotation annotation, String newAttributeValue) {
                String attrVal = newAttributeValue.contains(",") && attributeIsArray(annotation) ? getAttributeValuesAsString() : newAttributeValue;
                //noinspection ConstantConditions
                return JavaTemplate.<J.Annotation>apply("#{}", getCursor(), annotation.getCoordinates().replaceArguments(), attrVal)
                        .getArguments().get(0);
            }

            private J.Assignment createAnnotationAssignment(J.Annotation annotation, String name, @Nullable Object parameter) {
                //noinspection ConstantConditions
                return (J.Assignment) JavaTemplate.<J.Annotation>apply(name + " = " + (parameter instanceof J ? "#{any()}" : "#{}"), getCursor(), annotation.getCoordinates().replaceArguments(), parameter)
                        .getArguments().get(0);
            }
        });
    }

    private boolean isFullyQualifiedClass() {
        return attributeValue != null && attributeValue.endsWith(".class") && StringUtils.countOccurrences(attributeValue, ".") > 1;
    }

    private static String getFullyQualifiedClass(String fqn) {
        String withoutClassSuffix = fqn.substring(0, fqn.length() - 6);
        return withoutClassSuffix.substring(withoutClassSuffix.lastIndexOf('.') + 1) + ".class";
    }

    private String attributeName() {
        return attributeName == null ? "value" : attributeName;
    }

    private List<Expression> updateInitializer(J.Annotation annotation, List<Expression> initializerList, List<String> attributeList) {
        // If `oldAttributeValue` is defined, replace the old value with the new value(s). Ignore the `appendArray` option in this case.
        if (oldAttributeValue != null) {
            return ListUtils.flatMap(initializerList, it -> {
                if (it instanceof J.Literal && oldAttributeValue.equals(((J.Literal) it).getValue())) {
                    List<Expression> newItemsList = new ArrayList<>();
                    for (String attribute : attributeList) {
                        J.Literal newLiteral = new J.Literal(randomId(), SINGLE_SPACE, EMPTY, attribute, maybeQuoteStringArgument(annotation, attribute), null, JavaType.Primitive.String);
                        newItemsList.add(newLiteral);
                    }
                    return newItemsList;
                }
                return it;
            });
        }

        // If `appendArray` is true, add the new value(s) to the existing array (no duplicates)
        if (TRUE.equals(appendArray)) {
            List<Expression> newItemsList = new ArrayList<>();
            for (String attribute : attributeList) {
                if (attributeNameOrValIsAlreadyPresent(initializerList, singleton(attribute))) {
                    continue;
                }
                newItemsList.add(new J.Literal(randomId(), SINGLE_SPACE, EMPTY, attribute, maybeQuoteStringArgument(annotation, attribute), null, JavaType.Primitive.String));
            }
            return ListUtils.concatAll(initializerList, newItemsList);
        }

        // If no option is defined, replace the old array elements with the new elements
        List<Expression> list = ListUtils.map(initializerList, (i, it) -> {
            if (i >= attributeList.size()) {
                return null;
            }
            if (attributeNameOrValIsAlreadyPresent(it, singleton(attributeList.get(i)))) {
                return it;
            }
            return new J.Literal(randomId(), it.getPrefix(), EMPTY, attributeList.get(i), maybeQuoteStringArgument(annotation, attributeList.get(i)), null, JavaType.Primitive.String);
        });
        // and add extra new items if needed
        for (int i = initializerList.size(); i < attributeList.size(); i++) {
            list.add(new J.Literal(randomId(), SINGLE_SPACE, EMPTY, attributeList.get(i), maybeQuoteStringArgument(annotation, attributeList.get(i)), null, JavaType.Primitive.String));
        }
        return list;
    }

    private List<String> getAttributeValues() {
        if (attributeValue == null) {
            return emptyList();
        }
        if (isFullyQualifiedClass()) {
            return singletonList(getFullyQualifiedClass(attributeValue));
        }
        String attributeValueCleanedUp = attributeValue.replaceAll("\\s+", "").replaceAll("[\\s+{}\"]", "");
        return Arrays.asList(attributeValueCleanedUp.contains(",") ? attributeValueCleanedUp.split(",") : new String[]{attributeValueCleanedUp});
    }

    private String getAttributeValuesAsString() {
        return getAttributeValues().stream().map(String::valueOf).collect(joining("\", \"", "{\"", "\"}"));
    }

    private static boolean isAnnotationWithOnlyValueMethod(J.Annotation annotation) {
        return getMethods(annotation).size() == 1 && "value".equals(getMethods(annotation).get(0).getName());
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
            if (!(fa.getTarget() instanceof J.Identifier)) {
                return oldAttributeValue.equals(fa.toString());
            }
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
        return findMethod(annotation, attributeName())
                .map(it -> it.getReturnType() instanceof JavaType.Array)
                .orElse(false);
    }

    private boolean attributeIsString(J.Annotation annotation) {
        return findMethod(annotation, attributeName())
                .map(it -> TypeUtils.isOfClassType(it.getReturnType(), "java.lang.String"))
                .orElse(false);
    }

    private static Optional<JavaType.Method> findMethod(J.Annotation annotation, String methodName) {
        for (JavaType.Method it : getMethods(annotation)) {
            if (methodName.equals(it.getName())) {
                return Optional.of(it);
            }
        }
        return Optional.empty();
    }

    private static List<JavaType.Method> getMethods(J.Annotation annotation) {
        return ((JavaType.FullyQualified) requireNonNull(annotation.getAnnotationType().getType())).getMethods();
    }

    private boolean attributeNameOrValIsAlreadyPresent(Collection<Expression> expression, Collection<?> values) {
        for (Expression e : expression) {
            if (attributeNameOrValIsAlreadyPresent(e, values)) {
                return true;
            }
        }
        return false;
    }

    private boolean attributeNameOrValIsAlreadyPresent(Expression e, Collection<?> values) {
        if (e instanceof J.Assignment) {
            J.Assignment as = (J.Assignment) e;
            if (as.getVariable() instanceof J.Identifier) {
                return ((J.Identifier) as.getVariable()).getSimpleName().equals(attributeName());
            }
        } else if (e instanceof J.Literal) {
            return values.contains(((J.Literal) e).getValue() + "");
        } else if (e instanceof J.FieldAccess) {
            return values.contains(e.toString());
        } else if (e instanceof J.NewArray) {
            List<Expression> initializer = ((J.NewArray) e).getInitializer();
            return (initializer == null && attributeValue == null) || (initializer != null && attributeNameOrValIsAlreadyPresent(initializer, values));
        }
        return false;
    }
}
