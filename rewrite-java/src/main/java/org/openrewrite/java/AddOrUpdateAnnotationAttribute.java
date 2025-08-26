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
import org.openrewrite.java.tree.*;

import java.util.*;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
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
        TreeVisitor<?, ExecutionContext> v = Preconditions.check(new UsesType<>(annotationType, false), new JavaIsoVisitor<ExecutionContext>() {
            private String attributeNameOrDefault() {
                return attributeName == null ? "value" : attributeName;
            }

            private List<JavaType.Method> getMethods(J.Annotation annotation) {
                return ((JavaType.FullyQualified) requireNonNull(annotation.getAnnotationType().getType())).getMethods();
            }

            private Optional<JavaType.Method> findMethod(J.Annotation annotation, String methodName) {
                for (JavaType.Method it : getMethods(annotation)) {
                    if (methodName.equals(it.getName())) {
                        return Optional.of(it);
                    }
                }
                return Optional.empty();
            }

            private String getUsefulNameFromFieldAccess(J.FieldAccess fa) {
                if (!(fa.getTarget() instanceof J.Identifier)) {
                    return fa.toString();
                }
                return ((J.Identifier) fa.getTarget()).getSimpleName() + "." + fa.getSimpleName();
            }

            private void addPossibleClassImports(@Nullable String value) {
                if (value == null) {
                    return;
                }
                for (String singleVal : value.split(",")) {
                    if (singleVal.endsWith(".class") && StringUtils.countOccurrences(singleVal, ".") > 1) {
                        maybeAddImport(singleVal.substring(0, singleVal.length() - 6));
                    }
                }
            }

            private boolean attributeMatchesName(Expression e, String name) {
                if (e instanceof J.Assignment) {
                    J.Assignment as = (J.Assignment) e;
                    if (as.getVariable() instanceof J.Identifier) {
                        return ((J.Identifier) as.getVariable()).getSimpleName().equals(name);
                    }
                }
                return name.equals("value");
            }

            private boolean alreadyContainsAttributeOfName(J.Annotation annotation, String name) {
                List<Expression> existingArguments = annotation.getArguments();
                if (existingArguments == null) {
                    return false;
                }
                for (Expression e : annotation.getArguments()) {
                    if (attributeMatchesName(e, name)) {
                        return true;
                    }
                }
                return false;
            }

            private boolean valueMatches(Expression expression, String oldAttributeValue) {
                if (expression instanceof J.Literal) {
                    return oldAttributeValue.equals(((J.Literal) expression).getValue());
                } else if (expression instanceof J.FieldAccess) {
                    J.FieldAccess fa = (J.FieldAccess) expression;
                    String currentValue = getUsefulNameFromFieldAccess(fa);
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

            private J.Empty newEmpty() {
                return new J.Empty(randomId(), SINGLE_SPACE, EMPTY);
            }

            private List<Expression> updateInitializerDroppingMatched(@Nullable List<Expression> initializer, String searchValue) {
                List<Expression> updatedInitializer = ListUtils.filter(ListUtils.map(initializer, e -> {
                    if (valueMatches(e, searchValue)) {
                        return newEmpty();
                    }
                    return e;
                }), e -> !(e instanceof J.Empty));
                return updatedInitializer == null ? emptyList() : updatedInitializer;
            }

            private List<Expression> updateInitializerChangingMatched(@Nullable List<Expression> initializer, String searchValue, String newValue) {
                List<Expression> updatedInitializer = ListUtils.map(initializer, e -> {
                    if (valueMatches(e, searchValue)) {
                        // TODO - Change from this to specific setup based on newValue and appendArray
                        return e;
                    }
                    return e;
                });
                return updatedInitializer == null ? emptyList() : updatedInitializer;
            }

            // attributeValue == null
            private J.Annotation tryRemoveAnnotationAttribute(J.Annotation annotation, String searchAttribute, @Nullable String searchValue) {
                List<Expression> updatedArgs = ListUtils.map(annotation.getArguments(), it -> {
                    if (attributeMatchesName(it, searchAttribute)) {
                        if (searchValue == null) {
                            return newEmpty();
                        }
                        if (it instanceof J.Assignment) {
                            J.Assignment as = (J.Assignment) it;
                            Expression asValue = as.getAssignment();
                            if (asValue instanceof J.NewArray) {
                                J.NewArray asArray = (J.NewArray) asValue;
                                List<Expression> updatedInitializer = updateInitializerDroppingMatched(asArray.getInitializer(), searchValue);
                                return as.withAssignment(asArray.withInitializer(updatedInitializer));
                            }
                            if (valueMatches(asValue, searchValue)) {
                                return newEmpty();
                            }
                        } else if (it instanceof J.NewArray) {
                            J.NewArray itArray = (J.NewArray) it;
                            List<Expression> updatedInitializer = updateInitializerDroppingMatched(itArray.getInitializer(), searchValue);
                            return itArray.withInitializer(updatedInitializer);
                        } else if (valueMatches(it, searchValue)) {
                            return newEmpty();
                        }
                    }
                    return it;
                });
                return annotation.withArguments(ListUtils.filter(updatedArgs, it -> !(it instanceof J.Empty)));
            }

            private J.Annotation tryAddAnnotationAttribute(J.Annotation annotation, String newAttribute, String newValue) {
                // TODO
                return annotation;
            }

            private J.Annotation tryUpdateAnnotationAttribute(J.Annotation annotation, String searchAttribute, @Nullable String searchValue, String newValue) {
                List<Expression> updatedArgs = ListUtils.map(annotation.getArguments(), it -> {
                    if (attributeMatchesName(it, searchAttribute)) {
                        if (searchValue == null) {
                            if (it instanceof J.Assignment) {
                                J.Assignment as = (J.Assignment) it;
                                // TODO - overwriting using as.withAssignment(...), but differs by newValue typing and appendArray
                            }
                            // TODO - overwriting using new, but differs by newValue typing and appendArray
                        } else {
                            if (it instanceof J.Assignment) {
                                J.Assignment as = (J.Assignment) it;
                                Expression asValue = as.getAssignment();
                                if (asValue instanceof J.NewArray) {
                                    J.NewArray asArray = (J.NewArray) asValue;
                                    List<Expression> updatedInitializer = updateInitializerChangingMatched(asArray.getInitializer(), searchValue, newValue);
                                    return as.withAssignment(asArray.withInitializer(updatedInitializer));
                                }
                                if (valueMatches(asValue, searchValue)) {
                                    // TODO instantiate the correct typing
                                }
                            }
                            // TODO: else
                        }
                    }
                    return it;
                });
                return annotation.withArguments(updatedArgs);
            }

            @Override
            public J.Annotation visitAnnotation(J.Annotation original, ExecutionContext ctx) {
                J.Annotation a = super.visitAnnotation(original, ctx);
                String searchAttribute = attributeNameOrDefault();
                String searchValue = oldAttributeValue;
                // if not the right type of annotation or cannot find the method for a non-shallow class
                if (
                        !TypeUtils.isOfClassType(a.getType(), annotationType) ||
                        !(a.getType() instanceof JavaType.ShallowClass || findMethod(a, searchAttribute).isPresent())
                ) {
                    return a;
                }
                boolean existingAttribute = alreadyContainsAttributeOfName(a, searchAttribute);
                // if only want to add, but it already has attribute, ignores new attributeValue
                if (TRUE.equals(addOnly) && existingAttribute) {
                    return a;
                }

                // if you want to remove
                if (attributeValue == null) {
                    // if you can't update anything
                    if (!existingAttribute || TRUE.equals(addOnly)) {
                        return a;
                    }
                    a = tryRemoveAnnotationAttribute(a, searchAttribute, searchValue);
                } else {
                    // if you can't update anything
                    if (existingAttribute && TRUE.equals(addOnly)) {
                        return a;
                    }
                    if (existingAttribute) {
                        a = tryUpdateAnnotationAttribute(a, searchAttribute, searchValue, attributeValue);
                    } else {
                        a = tryAddAnnotationAttribute(a, searchAttribute, attributeValue);
                    }
                }
                addPossibleClassImports(attributeValue);

                // TODO: double check this (and also simplification in general)
                if (original != a) {
                    doAfterVisit(new SimplifySingleElementAnnotation().getVisitor());
                }
                return maybeAutoFormat(original, a, ctx);
            }
        });


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
                if (oldAttributeValue == null && newAttributeValue != null && !attributeNameAlreadyPresent(a)) {
                    J.Assignment as = createAnnotationAssignment(a, attributeName(), newAttributeValue);
                    a = a.withArguments(ListUtils.concat(as, a.getArguments()));
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
                Expression exp = as.getAssignment();
                if (newAttributeValue == null) {
                    if (exp instanceof J.NewArray) {
                        List<Expression> initializerList = requireNonNull(((J.NewArray) exp).getInitializer());
                        List<Expression> updatedList = updateInitializer(annotation, initializerList, getAttributeValues());
                        if (updatedList.isEmpty()) {
                            return null;
                        }
                        return as.withAssignment(((J.NewArray) exp)
                                .withInitializer(updatedList));
                    }
                    if (valueMatches(as.getAssignment(), oldAttributeValue)) {
                        return null;
                    }
                    return as;
                }
                if (exp instanceof J.NewArray) {
                    List<Expression> initializerList = requireNonNull(((J.NewArray) exp).getInitializer());
                    List<Expression> updatedList = updateInitializer(annotation, initializerList, getAttributeValues());
                    if (updatedList.isEmpty()) {
                        return null;
                    }
                    return as.withAssignment(((J.NewArray) exp)
                            .withInitializer(updatedList));
                }
                if (exp instanceof J.Literal) {
                    if (!valueMatches(exp, oldAttributeValue) || newAttributeValue.equals(((J.Literal) exp).getValueSource())) {
                        return as;
                    }
                    if (TRUE.equals(appendArray) && attributeIsArray(annotation)) {
                        List<Expression> updatedList = updateInitializer(annotation, singletonList(as.getAssignment()), getAttributeValues());
                        Expression flattenedList = createAnnotationLiteralFromString(
                                annotation,
                                wrapValues(updatedList.stream()
                                        .map(e -> {
                                            if (e instanceof J.Literal) {
                                                return ((J.Literal) e).getValueSource();
                                            } else if (e instanceof J.FieldAccess) {
                                                return getUsefulNameFromFieldAccess(((J.FieldAccess) e));
                                            }
                                            return "<bad parse>";
                                        })
                                        .collect(toList()), true)
                        );
                        return as.withAssignment(flattenedList);
                    }
                    return as.withAssignment(createAnnotationLiteral(annotation, newAttributeValue));
                }
                if (exp instanceof J.FieldAccess) {
                    if (!valueMatches(exp, oldAttributeValue) || newAttributeValue.equals(exp.toString())) {
                        return as;
                    }
                    if (isFullyQualifiedClass() && getFullyQualifiedClass(newAttributeValue).equals(exp.toString())) {
                        return as;
                    }
                    if (TRUE.equals(appendArray) && attributeIsArray(annotation)) {
                        List<Expression> updatedList = updateInitializer(annotation, singletonList(exp), getAttributeValues());
                        return as.withAssignment(createAnnotationLiteralFromString(
                                annotation,
                                wrapValues(updatedList.stream()
                                        .map(e -> {
                                            if (e instanceof J.Literal) {
                                                return ((J.Literal) e).getValueSource();
                                            } else if (e instanceof J.FieldAccess) {
                                                return getUsefulNameFromFieldAccess(((J.FieldAccess) e));
                                            }
                                            return "<bad parse>";
                                        })
                                        .collect(toList()), true)
                        ));
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
                        if (valueMatches(literal, oldAttributeValue)) {
                            return null;
                        }
                        return literal;
                    }
                    if (!valueMatches(literal, oldAttributeValue) || newAttributeValue.equals(literal.getValueSource())) {
                        return literal;
                    }
                    if (TRUE.equals(appendArray) && attributeIsArray(annotation)) {
                        List<Expression> updatedList = updateInitializer(annotation, singletonList(literal), getAttributeValues());
                        return createAnnotationLiteralFromString(
                                annotation,
                                wrapValues(updatedList.stream()
                                        .map(e -> {
                                            if (e instanceof J.Literal) {
                                                return ((J.Literal) e).getValueSource();
                                            } else if (e instanceof J.FieldAccess) {
                                                return getUsefulNameFromFieldAccess(((J.FieldAccess) e));
                                            }
                                            return "<bad parse>";
                                        })
                                        .collect(toList()), true)
                        );
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
                        if (valueMatches(fieldAccess, oldAttributeValue)) {
                            return null;
                        }
                        return fieldAccess;
                    }
                    if (isFullyQualifiedClass() && getFullyQualifiedClass(newAttributeValue).equals(fieldAccess.toString())) {
                        return fieldAccess;
                    }
                    if (!valueMatches(fieldAccess, oldAttributeValue) || newAttributeValue.equals(fieldAccess.toString())) {
                        return fieldAccess;
                    }
                    if (TRUE.equals(appendArray) && attributeIsArray(annotation)) {
                        List<Expression> updatedList = updateInitializer(annotation, singletonList(fieldAccess), getAttributeValues());
                        Expression flattenedList = createAnnotationLiteralFromString(
                                annotation,
                                wrapValues(updatedList.stream()
                                        .map(e -> {
                                            if (e instanceof J.Literal) {
                                                return ((J.Literal) e).getValueSource();
                                            } else if (e instanceof J.FieldAccess) {
                                                return getUsefulNameFromFieldAccess(((J.FieldAccess) e));
                                            }
                                            return "<bad parse>";
                                        })
                                        .collect(toList()), true)
                        );
                        return flattenedList;
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
                    List<Expression> initializerList = requireNonNull(arrayValue.getInitializer());
                    List<Expression> updatedList = updateInitializer(annotation, initializerList, getAttributeValues());
                    if (updatedList.isEmpty()) {
                        return null;
                    }
                    return arrayValue.withInitializer(updatedList);
                }
                if (attributeName != null && !"value".equals(attributeValue)) {
                    return isAnnotationWithOnlyValueMethod(annotation) ? arrayValue : createAnnotationAssignment(annotation, "value", arrayValue);
                }
                List<Expression> updatedList = updateInitializer(annotation, requireNonNull(arrayValue.getInitializer()), getAttributeValues());
                if (updatedList.isEmpty()) {
                    return null;
                }
                return arrayValue.withInitializer(updatedList);
            }

            private Expression createAnnotationLiteral(J.Annotation annotation, String newAttributeValue) {
                String attrVal = newAttributeValue.contains(",") && attributeIsArray(annotation) ? getAttributeValuesAsString() : newAttributeValue;
                //noinspection ConstantConditions
                return JavaTemplate.<J.Annotation>apply("#{}", getCursor(), annotation.getCoordinates().replaceArguments(), attrVal)
                        .getArguments().get(0);
            }

            private Expression createAnnotationLiteralFromString(J.Annotation annotation, String updatedAttributeValue) {
                //noinspection ConstantConditions
                return JavaTemplate.<J.Annotation>apply("#{}", getCursor(), annotation.getCoordinates().replaceArguments(), updatedAttributeValue)
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
        if (TRUE.equals(appendArray)) {
            if (oldAttributeValue != null) {
                // if initializer contains old attribute value
                    // append new values (de-duped) to end of attribute's existing value
                // else
                    // do not append
            } else {
                // append new values (de-duped) to end of attribute's existing value
            }
        } else {
            if (oldAttributeValue != null) {
                // if initializer contains old attribute value
                    // replace existing old attribute value in initializer with new values
                // else
                    // do not replace
            } else {
                // replace initializer with new values
            }
        }

        // If `oldAttributeValue` is defined, replace the old value with the new value(s). Ignore the `appendArray` option in this case.
        if (oldAttributeValue != null) {
            return ListUtils.flatMap(initializerList, it -> {
                List<Expression> newItemsList = new ArrayList<>();
                if ((it instanceof J.Literal || it instanceof J.FieldAccess) && valueMatches(it, oldAttributeValue)) {
                    for (String attribute : attributeList) {
                        J.Literal newLiteral = new J.Literal(randomId(), SINGLE_SPACE, EMPTY, attribute, maybeQuoteStringArgument(annotation, attribute), null, JavaType.Primitive.String);
                        newItemsList.add(newLiteral);
                    }
                    if (!TRUE.equals(appendArray)) {
                        return newItemsList;
                    }
                } else if (it instanceof J.Empty) {
                    return new ArrayList<>();
                }
                if (TRUE.equals(appendArray)) {
                    newItemsList.add(0, it);
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
        return wrapValues(getAttributeValues(), false);
    }

    private String wrapValues(List<@Nullable String> values, boolean quoteless) {
        if (quoteless) {
            return values.stream().map(String::valueOf).collect(joining(", ", "{", "}"));
        }
        return values.stream().map(String::valueOf).collect(joining("\", \"", "{\"", "\"}"));
    }

    private static boolean isAnnotationWithOnlyValueMethod(J.Annotation annotation) {
        return getMethods(annotation).size() == 1 && "value".equals(getMethods(annotation).get(0).getName());
    }

    private static String getUsefulNameFromFieldAccess(J.FieldAccess fa) {
        if (!(fa.getTarget() instanceof J.Identifier)) {
            return fa.toString();
        }
        return ((J.Identifier) fa.getTarget()).getSimpleName() + "." + fa.getSimpleName();
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
            String currentValue = getUsefulNameFromFieldAccess(fa);
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

    private boolean attributeNameAlreadyPresent(J.Annotation a) {
        List<Expression> existingArguments = a.getArguments();
        if (existingArguments == null) {
            return false;
        }
        for (Expression e : a.getArguments()) {
            if (e instanceof J.Assignment) {
                J.Assignment as = (J.Assignment) e;
                if (as.getVariable() instanceof J.Identifier) {
                    if (((J.Identifier) as.getVariable()).getSimpleName().equals(attributeName())) {
                        return true;
                    }
                }
            } else if (attributeName().equals("value")) {
                return true;
            }
        }
        return false;
    }
}
