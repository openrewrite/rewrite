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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.java.internal.grammar.TemplateParameterParser;
import org.openrewrite.java.internal.grammar.TemplateParameterParser.TypedPatternContext;
import org.openrewrite.java.internal.template.TemplateParameter;
import org.openrewrite.java.internal.template.TypeParameter;
import org.openrewrite.java.internal.template.VarargsMatch;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.openrewrite.Tree.randomId;

class JavaTemplateSemanticallyEqual extends SemanticallyEqual {

    @Value
    static class TemplateMatchResult {
        boolean match;
        List<J> matchedParameters;
    }

    static TemplateMatchResult matchesTemplate(JavaTemplate template, Cursor input) {
        JavaCoordinates coordinates;
        if (input.getValue() instanceof Expression) {
            coordinates = ((Expression) input.getValue()).getCoordinates().replace();
        } else if (input.getValue() instanceof Statement) {
            coordinates = ((Statement) input.getValue()).getCoordinates().replace();
        } else {
            throw new IllegalArgumentException("Only expressions and statements can be matched against a template: " + input.getClass());
        }

        J[] parameters = createTemplateParameters(template.getCode(), template.getGenericTypes());
        try {
            J templateTree = template.apply(input, coordinates, (Object[]) parameters);
            return matchTemplate(templateTree, input);
        } catch (RuntimeException e) {
            // FIXME this is just a workaround, as template matching finds many new corner cases in `JavaTemplate` which we need to fix
            return new TemplateMatchResult(false, emptyList());
        }
    }

    private static J[] createTemplateParameters(String code, Set<String> genericTypes) {
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper(
                "#{", "}", null);

        Map<String, JavaType.GenericTypeVariable> generics = TypeParameter.parseGenericTypes(genericTypes);
        List<J> parameters = new ArrayList<>();
        String substituted = code;
        Map<String, String> typedPatternByName = new HashMap<>();
        while (true) {
            String previous = substituted;
            substituted = propertyPlaceholderHelper.replacePlaceholders(substituted, key -> {
                String s;
                if (!key.isEmpty()) {
                    TemplateParameterParser.MatcherPatternContext ctx = TypeParameter.parser(key).matcherPattern();
                    if (ctx.typedPattern() == null) {
                        String paramName = ctx.parameterName().Identifier().getText();
                        s = typedPatternByName.get(paramName);
                        if (s == null) {
                            throw new IllegalArgumentException("The parameter " + paramName + " must be defined before it is referenced.");
                        }
                    } else {
                        TypedPatternContext typedPattern = ctx.typedPattern();
                        String matcherName = typedPattern.patternType().matcherName().Identifier().getText();
                        JavaType type = typedParameter(key, typedPattern, generics);
                        s = TypeUtils.toString(type);

                        String name = null;
                        if (typedPattern.parameterName() != null) {
                            name = typedPattern.parameterName().Identifier().getText();
                            typedPatternByName.put(name, s);
                        }

                        Markers markers = Markers.build(singleton(new TemplateParameter(randomId(), type, name, matcherName)));
                        parameters.add(new J.Empty(randomId(), Space.EMPTY, markers));
                    }
                } else {
                    throw new IllegalArgumentException("Only typed placeholders are allowed.");
                }

                return s;
            });

            if (previous.equals(substituted)) {
                break;
            }
        }

        return parameters.toArray(new J[0]);
    }

    private static JavaType typedParameter(String key, TypedPatternContext typedPattern, Map<String, JavaType.GenericTypeVariable> generics) {
        String matcherName = typedPattern.patternType().matcherName().Identifier().getText();
        if ("any".equals(matcherName)) {
            return TypeParameter.toJavaType(typedPattern.patternType().type(), generics);
        } else if ("anyArray".equals(matcherName)) {
            return new JavaType.Array(null, TypeParameter.toJavaType(typedPattern.patternType().type(), generics), null);
        } else {
            throw new IllegalArgumentException("Unsupported template matcher '" + key + "'");
        }
    }

    private static TemplateMatchResult matchTemplate(J templateTree, Cursor cursor) {
        if (templateTree == cursor.getValue()) {
            // When `JavaTemplate#apply()` returns the input itself, it could not be matched
            return new TemplateMatchResult(false, emptyList());
        }

        JavaTemplateSemanticallyEqualVisitor semanticallyEqualVisitor = new JavaTemplateSemanticallyEqualVisitor();
        semanticallyEqualVisitor.visit(templateTree, cursor.getValue(), cursor.getParentOrThrow());
        return new TemplateMatchResult(semanticallyEqualVisitor.isEqual(), new ArrayList<>(
                semanticallyEqualVisitor.matchedParameters.keySet()));
    }

    @SuppressWarnings("ConstantConditions")
    private static class JavaTemplateSemanticallyEqualVisitor extends SemanticallyEqualVisitor {
        final Map<J, String> matchedParameters = new LinkedHashMap<>();

        public JavaTemplateSemanticallyEqualVisitor() {
            super(true);
        }

        private boolean matchTemplateParameterPlaceholder(J.Empty empty, J j) {
            if (j instanceof TypedTree) {
                if (j instanceof J.Primitive || j instanceof J.Identifier && ((J.Identifier) j).getFieldType() == null) {
                    // don't match types, only expressions
                    return false;
                }
                TemplateParameter marker = (TemplateParameter) empty.getMarkers().getMarkers().get(0);

                if (marker.getName() != null) {
                    for (Map.Entry<J, String> matchedParameter : matchedParameters.entrySet()) {
                        if (matchedParameter.getValue().equals(marker.getName())) {
                            return SemanticallyEqual.areEqual(matchedParameter.getKey(), j);
                        }
                    }
                }

                if (isAssignableTo(marker.getType(), ((TypedTree) j).getType())) {
                    registerMatch(j, marker.getName());
                    return true;
                }
            }
            return false;
        }

        private void registerMatch(J j, @Nullable String name) {
            matchedParameters.put(j, name);
        }

        @Override
        public J.Empty visitEmpty(J.Empty empty, J j) {
            if (isEqual.get()) {
                if (isTemplateParameterPlaceholder(empty)) {
                    isEqual.set(matchTemplateParameterPlaceholder(empty, j));
                    return empty;
                }
                if (!(j instanceof J.Empty)) {
                    isEqual.set(false);
                    return empty;
                }

                J.Empty compareTo = (J.Empty) j;
                if (nullMissMatch(empty.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return empty;
                }
            }
            return empty;
        }

        private static boolean isTemplateParameterPlaceholder(J.Empty empty) {
            Markers markers = empty.getMarkers();
            return markers.getMarkers().size() == 1 && markers.getMarkers().get(0) instanceof TemplateParameter;
        }

        @Override
        protected boolean isOfType(JavaType target, JavaType source) {
            return TypeUtils.isAssignableTo(target, source, TypeUtils.ComparisonContext.INFER);
        }

        @Override
        protected boolean isAssignableTo(JavaType to, JavaType from) {
            return TypeUtils.isAssignableTo(to, from, TypeUtils.ComparisonContext.INFER);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation template, J j) {
            if (!isEqual.get()) {
                return template;
            }
            if (!(j instanceof J.MethodInvocation)) {
                isEqual.set(false);
                return template;
            }

            J.MethodInvocation actual = (J.MethodInvocation) j;
            JavaType.Method templateMethod = template.getMethodType();

            // Check if this is a varargs method with a placeholder at varargs position
            if (templateMethod != null &&
                templateMethod.hasFlags(Flag.Varargs) &&
                hasVarargsPlaceholder(template)) {

                isEqual.set(matchWithVarargs(template, actual));
                return template;
            }

            return super.visitMethodInvocation(template, j);
        }

        private boolean hasVarargsPlaceholder(J.MethodInvocation template) {
            List<Expression> args = template.getArguments();
            if (args.isEmpty()) {
                return false;
            }
            Expression lastArg = args.get(args.size() - 1);
            if (!(lastArg instanceof J.Empty)) {
                return false;
            }
            J.Empty empty = (J.Empty) lastArg;
            if (!isTemplateParameterPlaceholder(empty)) {
                return false;
            }
            TemplateParameter param = (TemplateParameter) empty.getMarkers().getMarkers().get(0);
            // Only use varargs matching for anyArray, not for any(Object[])
            return "anyArray".equals(param.getMatcherName()) && param.getType() instanceof JavaType.Array;
        }

        private boolean matchWithVarargs(J.MethodInvocation template, J.MethodInvocation actual) {
            JavaType.Method templateMethod = template.getMethodType();
            JavaType.Method actualMethod = actual.getMethodType();

            // Check method name matches
            if (!template.getSimpleName().equals(actual.getSimpleName())) {
                return false;
            }

            // Check method types are present
            if (templateMethod == null || actualMethod == null) {
                return false;
            }

            // Check return type is assignable
            if (!isAssignableTo(templateMethod.getReturnType(), actualMethod.getReturnType())) {
                return false;
            }

            // Check static flag compatibility
            boolean templateStatic = templateMethod.hasFlags(Flag.Static);
            boolean actualStatic = actualMethod.hasFlags(Flag.Static);
            if (templateStatic != actualStatic && nullMissMatch(template.getSelect(), actual.getSelect())) {
                return false;
            }

            // Check select expression
            if (!templateStatic) {
                if (nullMissMatch(template.getSelect(), actual.getSelect())) {
                    return false;
                }
                if (template.getSelect() != null && actual.getSelect() != null) {
                    visit(template.getSelect(), actual.getSelect());
                    if (!isEqual.get()) {
                        return false;
                    }
                }
            } else {
                JavaType.FullyQualified templateDeclaringType = templateMethod.getDeclaringType();
                JavaType.FullyQualified actualDeclaringType = actualMethod.getDeclaringType();
                if (!isAssignableTo(
                        templateDeclaringType instanceof JavaType.Parameterized ?
                                ((JavaType.Parameterized) templateDeclaringType).getType() : templateDeclaringType,
                        actualDeclaringType instanceof JavaType.Parameterized ?
                                ((JavaType.Parameterized) actualDeclaringType).getType() : actualDeclaringType)) {
                    return false;
                }
            }

            List<Expression> templateArgs = template.getArguments();
            List<Expression> actualArgs = actual.getArguments();

            // Handle the case where actual has "no arguments" represented as a single J.Empty
            boolean actualHasNoArgs = actualArgs.isEmpty() ||
                    (actualArgs.size() == 1 && actualArgs.get(0) instanceof J.Empty &&
                            !isTemplateParameterPlaceholder((J.Empty) actualArgs.get(0)));
            if (actualHasNoArgs) {
                actualArgs = emptyList();
            }

            int fixedArgCount = templateArgs.size() - 1; // All except the last varargs placeholder

            // Check actual has at least the fixed arguments
            if (actualArgs.size() < fixedArgCount) {
                return false;
            }

            // Match fixed arguments
            for (int i = 0; i < fixedArgCount; i++) {
                visit(templateArgs.get(i), actualArgs.get(i));
                if (!isEqual.get()) {
                    return false;
                }
            }

            // Get the varargs placeholder and its element type
            J.Empty varargsPlaceholder = (J.Empty) templateArgs.get(fixedArgCount);
            TemplateParameter param = (TemplateParameter) varargsPlaceholder.getMarkers().getMarkers().get(0);
            JavaType.Array arrayType = (JavaType.Array) param.getType();
            JavaType elementType = arrayType.getElemType();

            // Check for named parameter reuse
            if (param.getName() != null) {
                for (Map.Entry<J, String> entry : matchedParameters.entrySet()) {
                    if (param.getName().equals(entry.getValue())) {
                        // Named parameter already matched - verify it equals the actual varargs
                        return SemanticallyEqual.areEqual(entry.getKey(), varargsPlaceholder);
                    }
                }
            }

            // Collect varargs elements
            List<J> varargsElements = new ArrayList<>();
            for (int i = fixedArgCount; i < actualArgs.size(); i++) {
                Expression actualArg = actualArgs.get(i);

                // Handle case where explicit array is passed to varargs position
                if (i == fixedArgCount && actualArgs.size() == fixedArgCount + 1) {
                    JavaType actualArgType = actualArg.getType();
                    if (actualArgType instanceof JavaType.Array) {
                        // Single array argument passed to varargs - match it directly
                        if (isAssignableTo(arrayType, actualArgType)) {
                            varargsElements.add(actualArg);
                            break;
                        }
                    }
                }

                // Check each varargs element is assignable to the element type
                if (actualArg instanceof TypedTree) {
                    JavaType actualArgType = ((TypedTree) actualArg).getType();
                    if (!isAssignableTo(elementType, actualArgType)) {
                        return false;
                    }
                    varargsElements.add(actualArg);
                } else {
                    return false;
                }
            }

            // Register the match with VarargsMatch marker
            registerVarargsMatch(varargsElements, param.getName());
            return true;
        }

        private void registerVarargsMatch(List<J> elements, @Nullable String name) {
            Markers markers = Markers.build(singleton(new VarargsMatch(randomId(), elements)));
            J.Empty placeholder = new J.Empty(randomId(), Space.EMPTY, markers);
            matchedParameters.put(placeholder, name);
        }
    }
}
