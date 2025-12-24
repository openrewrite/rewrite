/*
 * Copyright 2020 the original author or authors.
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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Validated;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.stream.Collectors.toList;

/**
 * This class accepts an AspectJ method pattern and is used to identify methods that match the expression. The
 * format of the method pattern is as follows:
 * <P><P><B>
 * #declaring class# #method name#(#argument list#)
 * </B><P>
 * <li>The declaring class must be fully qualified.</li>
 * <li>A wildcard character, "*", may be used in either the declaring class or method name.</li>
 * <li>The argument list is expressed as a comma-separated list of the argument types</li>
 * <li>".." can be used in the argument list to match zero or more arguments of any type.</li>
 * <P><PRE>
 * EXAMPLES:
 * <p>
 * *..* *(..)                              - All method invocations
 * java.util.* *(..)                       - All method invocations to classes belonging to java.util (including sub-packages)
 * java.util.Collections *(..)             - All method invocations on java.util.Collections class
 * java.util.Collections unmodifiable*(..) - All method invocations starting with "unmodifiable" on java.util.Collections
 * java.util.Collections min(..)           - All method invocations for all overloads of "min"
 * java.util.Collections emptyList()       - All method invocations on java.util.Collections.emptyList()
 * my.org.MyClass *(boolean, ..)           - All method invocations where the first arg is a boolean in my.org.MyClass
 * </PRE>
 */
@SuppressWarnings("NotNullFieldNotInitialized")
public class MethodMatcher {
    //language=markdown
    public static final String METHOD_PATTERN_DECLARATIONS_DESCRIPTION =
            "A [method pattern](https://docs.openrewrite.org/reference/method-patterns) is used to find matching method declarations. " +
                    "For example, to find all method declarations in the Guava library, use the pattern: " +
                    "`com.google.common..*#*(..)`.<br/><br/>" +
                    "The pattern format is `<PACKAGE>#<METHOD_NAME>(<ARGS>)`. <br/><br/>" +
                    "`..*` includes all subpackages of `com.google.common`. <br/>" +
                    "`*(..)` matches any method name with any number of arguments. <br/><br/>" +
                    "For more specific queries, like Guava's `ImmutableMap`, use " +
                    "`com.google.common.collect.ImmutableMap#*(..)` to narrow down the results.";
    //language=markdown
    public static final String METHOD_PATTERN_INVOCATIONS_DESCRIPTION =
            "A [method pattern](https://docs.openrewrite.org/reference/method-patterns) is used to find matching method invocations. " +
                    "For example, to find all method invocations in the Guava library, use the pattern: " +
                    "`com.google.common..*#*(..)`.<br/><br/>" +
                    "The pattern format is `<PACKAGE>#<METHOD_NAME>(<ARGS>)`. <br/><br/>" +
                    "`..*` includes all subpackages of `com.google.common`. <br/>" +
                    "`*(..)` matches any method name with any number of arguments. <br/><br/>" +
                    "For more specific queries, like Guava's `ImmutableMap`, use " +
                    "`com.google.common.collect.ImmutableMap#*(..)` to narrow down the results.";
    /**
     * @deprecated Use {@link #METHOD_PATTERN_INVOCATIONS_DESCRIPTION} instead.
     */
    @Deprecated
    public static final String METHOD_PATTERN_DESCRIPTION = METHOD_PATTERN_INVOCATIONS_DESCRIPTION;

    private TypeMatcher typeMatcher;
    private MethodNameMatcher methodNameMatcher;
    private List<ArgumentMatcher> argumentMatchers;
    private int varArgsPosition = -1;

    /**
     * Whether to match overridden forms of the method on subclasses of {@link #typeMatcher}.
     */
    @Getter
    private final boolean matchOverrides;

    public MethodMatcher(String methodPattern, @Nullable Boolean matchOverrides) {
        this(methodPattern, Boolean.TRUE.equals(matchOverrides));
    }

    public MethodMatcher(String methodPattern, boolean matchOverrides) {
        this.matchOverrides = matchOverrides;

        String patternToUse = methodPattern;
        boolean retryWithPound = false;

        try {
            parsePattern(patternToUse);
        } catch (IllegalArgumentException e) {
            // Check if the error is due to missing separator between type and method
            // Try to find the last dot before the opening parenthesis
            int lastParen = methodPattern.lastIndexOf('(');
            if (lastParen > 0) {
                int lastDot = methodPattern.lastIndexOf('.', lastParen);
                if (lastDot > 0 && lastDot < lastParen - 1) {
                    // Replace the last dot with # and retry
                    patternToUse = methodPattern.substring(0, lastDot) + "#" +
                            methodPattern.substring(lastDot + 1);
                    retryWithPound = true;
                }
            }

            if (retryWithPound) {
                try {
                    parsePattern(patternToUse);
                    // Log or track that we used the fallback
                } catch (Exception retryException) {
                    // If retry also fails, throw the original exception
                    throw e;
                }
            } else {
                throw e;
            }
        }
    }

    private void parsePattern(String methodPattern) {
        Parser parser = new Parser(methodPattern);
        parser.parse();

        this.typeMatcher = parser.typeMatcher;
        this.methodNameMatcher = parser.methodNameMatcher;
        this.argumentMatchers = parser.argumentMatchers;
        this.varArgsPosition = parser.varArgsPosition;
    }

    public static Validated<String> validate(@Nullable String signature) {
        String property = "methodPattern";
        try {
            if (signature != null) {
                new MethodMatcher(signature, null);
            }
            return Validated.valid(property, signature);
        } catch (Throwable throwable) {
            return Validated.invalid(
                    property,
                    signature,
                    "Tried to construct a method matcher with an invalid method pattern. " +
                            "An example of a good method pattern is `java.util.List add(..)`. " +
                            throwable.getMessage(),
                    throwable
            );
        }
    }


    public MethodMatcher(J.MethodDeclaration method, boolean matchOverrides) {
        this(methodPattern(method), matchOverrides);
    }

    public MethodMatcher(String methodPattern) {
        this(methodPattern, false);
    }

    public MethodMatcher(J.MethodDeclaration method) {
        this(method, false);
    }

    public MethodMatcher(JavaType.Method method) {
        this(methodPattern(method), false);
    }

    private boolean matchesTargetTypeName(String fullyQualifiedTypeName) {
        return typeMatcher.matchesQualifiedName(fullyQualifiedTypeName);
    }

    boolean matchesTargetType(JavaType type) {
        if (typeMatcher.matches(type)) {
            return true;
        }

        if (matchOverrides && type instanceof JavaType.FullyQualified) {
            return TypeUtils.isOfTypeWithName(
                    (JavaType.FullyQualified) type,
                    matchOverrides,
                    this::matchesTargetTypeName
            );
        }

        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean matchesMethodName(String name) {
        return methodNameMatcher.matches(name);
    }

    private boolean matchesParameterTypes(List<JavaType> parameterTypes) {
        // Try the new ArgumentMatcher approach first if available
        return matchesParameterTypesWithMatchers(parameterTypes);
    }

    private boolean matchesParameterTypesWithMatchers(List<JavaType> types) {
        if (varArgsPosition == -1) {
            // No varargs - exact match required
            if (types.size() != argumentMatchers.size()) {
                return false;
            }
            for (int i = 0; i < types.size(); i++) {
                ArgumentMatcher matcher = argumentMatchers.get(i);
                if (!matcher.matches(types.get(i))) {
                    return false;
                }
            }
            return true;
        } else {
            // Has wildcard varargs (..) which can match any number of arguments
            int beforeCount = varArgsPosition;
            int afterCount = argumentMatchers.size() - varArgsPosition - 1;

            if (types.size() < beforeCount + afterCount) {
                return false;
            }

            // Match parameters before wildcard varargs
            for (int i = 0; i < beforeCount; i++) {
                if (!argumentMatchers.get(i).matches(types.get(i))) {
                    return false;
                }
            }

            // Match wildcard varargs themselves (from beforeCount to types.size() - afterCount)
            ArgumentMatcher varargsMatcher = argumentMatchers.get(varArgsPosition);
            for (int i = beforeCount; i < types.size() - afterCount; i++) {
                if (!varargsMatcher.matches(types.get(i))) {
                    return false;
                }
            }

            // Match parameters after wildcard varargs
            for (int i = 0; i < afterCount; i++) {
                int typeIndex = types.size() - afterCount + i;
                int matcherIndex = varArgsPosition + 1 + i;
                if (!argumentMatchers.get(matcherIndex).matches(types.get(typeIndex))) {
                    return false;
                }
            }

            return true;
        }
    }

    public boolean matches(JavaType.@Nullable Method type) {
        if (type == null) {
            return false;
        }
        if (!matchesMethodName(type.getName())) {
            return false;
        }

        // Early argument count check to avoid expensive type matching
        if (!matchesParameterCount(type.getParameterTypes().size())) {
            return false;
        }

        if (!matchesTargetType(type.getDeclaringType())) {
            return false;
        }

        return matchesParameterTypes(type.getParameterTypes());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean matchesParameterCount(int actualArgCount) {
        return varArgsPosition == -1 ? actualArgCount == argumentMatchers.size() : actualArgCount >= argumentMatchers.size() - 1;
    }

    public boolean matches(@Nullable MethodCall methodCall) {
        if (methodCall == null) {
            return false;
        }
        return matches(methodCall.getMethodType());
    }

    public boolean matches(@Nullable Expression maybeMethod) {
        return maybeMethod instanceof MethodCall && matches((MethodCall) maybeMethod);
    }

    public boolean matches(J.MethodDeclaration method, J.ClassDeclaration enclosing) {
        if (enclosing.getType() == null || !matchesTargetType(enclosing.getType())) {
            return false;
        }

        if (method.getMethodType() != null && !matchesMethodName(method.getMethodType().getName())) {
            return false;
        }

        List<JavaType> parameterTypes =
                method
                        .getParameters()
                        .stream()
                        .map(MethodMatcher::variableDeclarationsType)
                        .filter(Objects::nonNull)
                        .collect(toList());
        return matchesParameterTypes(parameterTypes);
    }

    public boolean matches(J.MethodDeclaration method, J.NewClass enclosing) {
        if (enclosing.getType() == null) {
            return false;
        }

        if (!TypeUtils.isAssignableTo(this::matchesTargetType, enclosing.getType())) {
            return false;
        }

        if (method.getMethodType() != null && !matchesMethodName(method.getMethodType().getName())) {
            return false;
        }

        List<JavaType> parameterTypes =
                method
                        .getParameters()
                        .stream()
                        .map(MethodMatcher::variableDeclarationsType)
                        .filter(Objects::nonNull)
                        .collect(toList());
        return matchesParameterTypes(parameterTypes);
    }

    private static @Nullable JavaType variableDeclarationsType(Statement v) {
        if (v instanceof J.VariableDeclarations) {
            J.VariableDeclarations vd = (J.VariableDeclarations) v;
            List<J.VariableDeclarations.NamedVariable> variables = vd.getVariables();
            if (!variables.isEmpty() && variables.get(0).getType() != null) {
                return variables.get(0).getType();
            } else if (vd.getTypeAsFullyQualified() != null) {
                return vd.getTypeAsFullyQualified();
            } else {
                return vd.getTypeExpression() != null ? vd.getTypeExpression().getType() : null;
            }
        } else {
            return null;
        }
    }

    /**
     * Prefer {@link #matches(MethodCall)}, which uses the default `false` behavior for matchUnknownTypes.
     * Using matchUnknownTypes can improve Visitor resiliency for an AST with missing type information, but
     * also increases the risk of false-positive matches on unrelated MethodInvocation instances.
     */
    public boolean matches(J.@Nullable MethodInvocation method, boolean matchUnknownTypes) {
        if (method == null) {
            return false;
        }

        if (method.getMethodType() == null) {
            return matchUnknownTypes && matchesAllowingUnknownTypes(method);
        }

        return matches(method.getMethodType());
    }

    private boolean matchesAllowingUnknownTypes(J.MethodInvocation method) {
        if (!matchesMethodName(method.getSimpleName())) {
            return false;
        }

        // Early argument count check to avoid expensive select matching
        int actualArgCount = method.getArguments().size();
        if (!matchesParameterCount(actualArgCount)) {
            return false;
        }

        // When checking receiver with unknown types, we still need to match the name pattern
        // but we're more lenient about it
        if (method.getSelect() instanceof J.Identifier) {
            J.Identifier select = (J.Identifier) method.getSelect();
            // Always check the pattern - but for unknown types we use the simple name alone
            if (!typeMatcher.matchesSimpleName(select.getSimpleName())) {
                return false;
            }
        }

        // For unknown types, we need to be more lenient with argument matching
        return matchesArgumentsAllowingUnknownTypes(method);
    }

    private boolean matchesArgumentsAllowingUnknownTypes(J.MethodInvocation method) {
        // Try the new ArgumentMatcher approach first if available
        List<Expression> arguments = method.getArguments();
        if (varArgsPosition == -1) {
            // No varargs - exact match required
            if (arguments.size() != argumentMatchers.size()) {
                return false;
            }
            for (int i = 0; i < arguments.size(); i++) {
                if (!argumentMatchers.get(i).matchesUnknown(arguments.get(i).getType())) {
                    return false;
                }
            }
            return true;
        } else {
            // Has wildcard varargs (..) which can match any number of arguments
            int beforeCount = varArgsPosition;
            int afterCount = argumentMatchers.size() - varArgsPosition - 1;

            if (arguments.size() < beforeCount + afterCount) {
                return false;
            }

            // Match parameters before wildcard varargs
            for (int i = 0; i < beforeCount; i++) {
                if (!argumentMatchers.get(i).matchesUnknown(arguments.get(i).getType())) {
                    return false;
                }
            }

            // Match wildcard varargs themselves (from beforeCount to arguments.size() - afterCount)
            ArgumentMatcher varargsMatcher = argumentMatchers.get(varArgsPosition);
            for (int i = beforeCount; i < arguments.size() - afterCount; i++) {
                if (!varargsMatcher.matchesUnknown(arguments.get(i).getType())) {
                    return false;
                }
            }

            // Match parameters after wildcard varargs
            for (int i = 0; i < afterCount; i++) {
                int argIndex = arguments.size() - afterCount + i;
                int matcherIndex = varArgsPosition + 1 + i;
                if (!argumentMatchers.get(matcherIndex).matchesUnknown(arguments.get(argIndex).getType())) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * Evaluate whether this MethodMatcher and the specified FieldAccess are describing the same type or not.
     * Known limitation/bug: MethodMatchers can have patterns/wildcards like "com.*.Bar" instead of something
     * concrete like "com.foo.Bar". This limitation is not desirable or intentional and should be fixed.
     * If a methodMatcher is passed that includes wildcards the result will always be "false"
     *
     * @param fieldAccess A J.FieldAccess that hopefully has the same fully qualified type as this matcher.
     */
    public boolean isFullyQualifiedClassReference(J.FieldAccess fieldAccess) {
        if (!methodNameMatcher.matches(fieldAccess.getName().getSimpleName())) {
            return false;
        }

        Expression target = fieldAccess.getTarget();
        if (target instanceof J.Identifier) {
            String simpleName = ((J.Identifier) target).getSimpleName();
            return typeMatcher.matchesSimpleName(simpleName);
        } else if (target instanceof J.FieldAccess) {
            StringBuilder builder = new StringBuilder();
            while (target instanceof J.FieldAccess) {
                builder.insert(0, ((J.FieldAccess) target).getSimpleName());
                builder.insert(0, '.');
                target = ((J.FieldAccess) target).getTarget();
            }
            if (target instanceof J.Identifier) {
                builder.insert(0, ((J.Identifier) target).getSimpleName());
            }
            return typeMatcher.matchesQualifiedName(builder.toString());
        }
        return false;
    }

    private static @Nullable String typePattern(JavaType type) {
        if (type instanceof JavaType.Primitive) {
            if (type.equals(JavaType.Primitive.String)) {
                return ((JavaType.Primitive) type).getClassName();
            }
            return ((JavaType.Primitive) type).getKeyword();
        } else if (type instanceof JavaType.Unknown) {
            return "*";
        } else if (type instanceof JavaType.FullyQualified) {
            return ((JavaType.FullyQualified) type).getFullyQualifiedName();
        } else if (type instanceof JavaType.Array) {
            JavaType elemType = ((JavaType.Array) type).getElemType();
            return typePattern(elemType) + "[]";
        }
        return null;
    }

    public static String methodPattern(J.MethodDeclaration method) {
        assert method.getMethodType() != null;
        return methodPattern(method.getMethodType());
    }

    public static String methodPattern(JavaType.Method method) {
        StringJoiner parameters = new StringJoiner(", ", "(", ")");
        for (JavaType javaType : method.getParameterTypes()) {
            String s = typePattern(javaType);
            if (s != null) {
                parameters.add(s);
            }
        }

        return typePattern(method.getDeclaringType()) + " " + method.getName() + parameters;
    }

    @Override
    public String toString() {
        StringJoiner arguments = new StringJoiner(", ", "(", ")");
        for (ArgumentMatcher argumentMatcher : argumentMatchers) {
            arguments.add(argumentMatcher.toString());
        }

        return typeMatcher + " " + methodNameMatcher + arguments;
    }

    /**
     * Simple AspectJ-style pattern matcher that avoids regex compilation.
     * Supports:
     * - * matches any sequence of characters except '.'
     * - .. matches any sequence of packages/subpackages
     * - Literal matching for everything else
     */
    // IMPORTANT: Don't add more subtypes so we can benefit from bimorphic optimizations
    interface TypeMatcher {
        boolean matches(JavaType type);

        boolean matchesQualifiedName(String qualifiedName);

        /**
         * Match against a simple name (used when type information is unknown).
         * For patterns like "com.foo.Bar", matches "Bar".
         * For patterns like "com.*.Bar", also matches "Bar".
         */
        boolean matchesSimpleName(String simpleName);
    }

    /**
     * Extract the type name from a JavaType, normalizing primitives (especially String).
     * Returns null if the type cannot be converted to a meaningful type name.
     */
    static @Nullable String extractTypeName(JavaType type) {
        if (type instanceof JavaType.Primitive) {
            JavaType.Primitive primitive = (JavaType.Primitive) type;
            if (primitive == JavaType.Primitive.String) {
                return primitive.getClassName();
            }
            return primitive.getKeyword();
        }

        if (type instanceof JavaType.FullyQualified) {
            return ((JavaType.FullyQualified) type).getFullyQualifiedName();
        }

        return null;
    }

    /**
     * Helper to unwrap array dimensions and varargs, allowing base type matchers to focus only on type matching.
     */
    static @Nullable JavaType unwrapArrays(JavaType type, int expectedArrayDepth, boolean expectVarargs) {
        // Unwrap expected array dimensions
        while (expectedArrayDepth > 0 && type instanceof JavaType.Array) {
            type = ((JavaType.Array) type).getElemType();
            expectedArrayDepth--;
        }

        // If we expected more arrays than we got, return null to signal mismatch
        if (expectedArrayDepth > 0) {
            return null;
        }

        // Handle varargs - expecting one more array layer
        if (expectVarargs) {
            if (type instanceof JavaType.Array) {
                type = ((JavaType.Array) type).getElemType();
            } else {
                return null;  // Expected varargs but didn't get array
            }
        }

        return type instanceof JavaType.Unknown ? null : type;
    }

    @RequiredArgsConstructor
    static class StandardTypeMatcher implements TypeMatcher {
        private final TypeNameMatcher nameMatcher;
        private final int arrayDimensions;

        @Override
        public boolean matches(JavaType type) {
            type = unwrapArrays(type, arrayDimensions, false);
            if (type == null) {
                return false;
            }

            String typeName = extractTypeName(type);
            return typeName != null && nameMatcher.matches(typeName);
        }

        @Override
        public boolean matchesQualifiedName(String qualifiedName) {
            return nameMatcher.matches(qualifiedName);
        }

        @Override
        public boolean matchesSimpleName(String simpleName) {
            return nameMatcher.matchesSimpleName(simpleName);
        }

        @Override
        public String toString() {
            return nameMatcher.toString();
        }
    }

    enum WildcardTypeMatcher implements TypeMatcher {
        INSTANCE;

        @Override
        public boolean matches(@Nullable JavaType type) {
            return true;
        }

        @Override
        public boolean matchesQualifiedName(String qualifiedName) {
            return true;
        }

        @Override
        public boolean matchesSimpleName(String simpleName) {
            return true;
        }

        @Override
        public String toString() {
            return "*";
        }
    }

    // IMPORTANT: Don't add more subtypes so we can benefit from bimorphic optimizations
    interface MethodNameMatcher {
        boolean matches(String methodName);
    }

    @RequiredArgsConstructor
    static class ExactMethodNameMatcher implements MethodNameMatcher {
        private final String methodName;

        @Override
        public boolean matches(String name) {
            return methodName.equals(name);
        }

        @Override
        public String toString() {
            return methodName;
        }
    }

    @RequiredArgsConstructor
    static class ConstructorMethodNameMatcher implements MethodNameMatcher {
        private final String originalName;

        @Override
        public boolean matches(String name) {
            return "<constructor>".equals(name);
        }

        @Override
        public String toString() {
            // Normalize <init> to <constructor> in toString
            return "<init>".equals(originalName) ? "<constructor>" : originalName;
        }
    }

    @RequiredArgsConstructor
    static class PatternMethodNameMatcher implements MethodNameMatcher {
        private final String pattern;
        private final boolean isFullWildcard;

        @Override
        public boolean matches(String methodName) {
            return isFullWildcard || matchesMethodPattern(pattern, methodName, 0, 0);
        }

        private boolean matchesMethodPattern(String pattern, String text, int pIdx, int tIdx) {
            int pLength = pattern.length();
            int tLength = text.length();

            while (pIdx < pLength) {
                if (tIdx >= tLength) {
                    // Consume any remaining wildcards in pattern
                    while (pIdx < pLength && pattern.charAt(pIdx) == '*') {
                        pIdx++;
                    }
                    return pIdx >= pLength;
                }

                char p = pattern.charAt(pIdx++);
                if (p == '*') {
                    // Wildcard at end matches rest of text
                    if (pIdx >= pLength) {
                        return true;
                    }

                    // Try matching with zero-length wildcard match first (greedy)
                    if (matchesMethodPattern(pattern, text, pIdx, tIdx)) {
                        return true;
                    }
                    // Then try consuming characters from text
                    while (tIdx < tLength) {
                        tIdx++;
                        if (matchesMethodPattern(pattern, text, pIdx, tIdx)) {
                            return true;
                        }
                    }
                    return false;
                } else {
                    // Literal character must match
                    if (text.charAt(tIdx) != p) {
                        return false;
                    }
                    tIdx++;
                }
            }

            return tIdx >= tLength;
        }

        @Override
        public String toString() {
            return pattern;
        }
    }

    interface ArgumentMatcher {
        boolean matches(JavaType type);
        boolean matchesUnknown(@Nullable JavaType type);
    }

    // exact or pattern case
    @RequiredArgsConstructor
    static class StandardArgumentMatcher implements ArgumentMatcher {
        final TypeNameMatcher nameMatcher;
        final int arrayDimensions;

        @Override
        public boolean matches(JavaType type) {
            type = unwrapArrays(type, arrayDimensions, false);
            if (type == null) {
                return false;
            }
            String typeName = extractTypeName(type);
            return typeName != null && nameMatcher.matches(typeName);
        }

        @Override
        public boolean matchesUnknown(@Nullable JavaType type) {
            if (type == null || type instanceof JavaType.Unknown) {
                return true;
            }
            return matches(type);
        }

        @Override
        public String toString() {
            return nameMatcher.toString();
        }
    }

    // `*` case
    enum WildcardMatcher implements ArgumentMatcher {
        INSTANCE;

        @Override
        public boolean matches(JavaType type) {
            return true;
        }

        @Override
        public boolean matchesUnknown(@Nullable JavaType type) {
            return true;
        }

        @Override
        public String toString() {
            return "*";
        }
    }

    // `type...` case
    @RequiredArgsConstructor
    static class VarArgsMatcher implements ArgumentMatcher {
        private final StandardArgumentMatcher elementMatcher;

        @Override
        public boolean matches(JavaType type) {
            // Varargs can be passed as:
            // 1. Array: acceptsProfiles(new String[]{"a", "b"})
            // 2. Individual args: acceptsProfiles("a", "b", "c")

            // Try matching as array first
            if (type instanceof JavaType.Array) {
                JavaType elemType = ((JavaType.Array) type).getElemType();
                if (elementMatcher.matches(elemType)) {
                    return true;
                }
            }

            // Try matching as individual element
            return elementMatcher.matches(type);
        }

        @Override
        public boolean matchesUnknown(@Nullable JavaType type) {
            if (type == null || type instanceof JavaType.Unknown) {
                return true;
            }
            return matches(type);
        }

        @Override
        public String toString() {
            return elementMatcher + "...";
        }
    }

    // `..` case
    enum WildcardVarArgsMatcher implements ArgumentMatcher {
        INSTANCE;

        @Override
        public boolean matches(JavaType type) {
            // Wildcard varargs (..) matches any type
            return true;
        }

        @Override
        public boolean matchesUnknown(@Nullable JavaType type) {
            // Wildcard varargs (..) matches any type
            return true;
        }

        @Override
        public String toString() {
            return "..";
        }
    }

    @RequiredArgsConstructor
    static class Parser {
        private final String pattern;
        private MethodMatcher.TypeMatcher typeMatcher;
        private MethodMatcher.MethodNameMatcher methodNameMatcher;
        private List<MethodMatcher.ArgumentMatcher> argumentMatchers;
        private int varArgsPosition = -1;
        private boolean hasWildcardVarArgs = false;  // Track if we've seen .. wildcard

        void parse() {
            int openParen = pattern.indexOf('(');
            if (openParen == -1) {
                throw new IllegalArgumentException("Invalid method pattern - missing '(': " + pattern);
            }

            int closeParen = pattern.lastIndexOf(')');
            if (closeParen == -1 || closeParen <= openParen) {
                throw new IllegalArgumentException("Invalid method pattern - missing or misplaced ')': " + pattern);
            }

            // Find the separator between type and method (# or last space before '(')
            int separator = pattern.lastIndexOf('#', openParen);
            if (separator == -1) {
                separator = pattern.lastIndexOf(' ', openParen);
                if (separator == -1) {
                    throw new IllegalArgumentException("Invalid method pattern - missing type/method separator: " + pattern);
                }
            }

            // Parse type pattern
            String typePattern = pattern.substring(0, separator).trim();
            if (typePattern.isEmpty()) {
                throw new IllegalArgumentException("Invalid method pattern - empty type pattern: " + pattern);
            }
            typeMatcher = parseTypeMatcher(typePattern);

            // Parse method name pattern
            String methodName = pattern.substring(separator + 1, openParen).trim();
            if (methodName.isEmpty()) {
                throw new IllegalArgumentException("Invalid method pattern - empty method name: " + pattern);
            }
            methodNameMatcher = parseMethodNameMatcher(methodName);

            // Parse arguments
            String argsString = pattern.substring(openParen + 1, closeParen).trim();
            parseArguments(argsString);
        }

        private MethodMatcher.TypeMatcher parseTypeMatcher(String typePattern) {
            org.openrewrite.java.TypeMatcher.ParsedType parsed = parseType(typePattern);

            // Check for full wildcard pattern
            if ("*".equals(parsed.getBaseType()) && parsed.getArrayDimensions() == 0) {
                return MethodMatcher.WildcardTypeMatcher.INSTANCE;
            }

            TypeNameMatcher nameMatcher = TypeNameMatcher.fromPattern(parsed.getBaseType());

            // Check if the name matcher is a full wildcard pattern with no arrays
            if (nameMatcher instanceof PatternTypeNameMatcher &&
                    ((PatternTypeNameMatcher) nameMatcher).isFullWildcard() &&
                    parsed.getArrayDimensions() == 0) {
                return MethodMatcher.WildcardTypeMatcher.INSTANCE;
            }

            return new MethodMatcher.StandardTypeMatcher(nameMatcher, parsed.getArrayDimensions());
        }

        private org.openrewrite.java.TypeMatcher.ParsedType parseType(String typePattern) {
            return org.openrewrite.java.TypeMatcher.parseTypePattern(typePattern);
        }

        private MethodMatcher.MethodNameMatcher parseMethodNameMatcher(String methodName) {
            // Handle special cases
            if ("<constructor>".equals(methodName) || "<init>".equals(methodName)) {
                return new MethodMatcher.ConstructorMethodNameMatcher(methodName);
            }
            if ("<default>".equals(methodName)) {
                return new MethodMatcher.ExactMethodNameMatcher("<default>");
            }

            // Check if it contains wildcards
            if (methodName.contains("*")) {
                boolean isFullWildcard = "*".equals(methodName);
                return new MethodMatcher.PatternMethodNameMatcher(methodName, isFullWildcard);
            }

            return new MethodMatcher.ExactMethodNameMatcher(methodName);
        }

        private void parseArguments(String argsString) {
            argumentMatchers = new ArrayList<>();
            varArgsPosition = -1;

            // Handle empty arguments
            if (argsString.isEmpty()) {
                return;
            }

            // Handle (..) - matches any arguments
            if ("..".equals(argsString)) {
                argumentMatchers.add(MethodMatcher.WildcardVarArgsMatcher.INSTANCE);
                varArgsPosition = 0;
                return;
            }

            // Parse comma-separated arguments
            int start = 0;
            int commaPos;

            while ((commaPos = argsString.indexOf(',', start)) != -1) {
                processArgument(argsString.substring(start, commaPos).trim());
                start = commaPos + 1;
            }

            // Process the last argument
            processArgument(argsString.substring(start).trim());
        }

        private void processArgument(String arg) {
            if (arg.isEmpty()) {
                return; // Skip empty arguments (e.g., trailing comma)
            }

            if ("..".equals(arg)) {
                // Wildcard varargs (..) - matches zero or more arguments of any type
                if (hasWildcardVarArgs) {
                    throw new IllegalArgumentException("Invalid method pattern - only one wildcard varargs (..) is allowed: " + pattern);
                }
                hasWildcardVarArgs = true;
                if (varArgsPosition == -1) {
                    varArgsPosition = argumentMatchers.size();
                }
                argumentMatchers.add(MethodMatcher.WildcardVarArgsMatcher.INSTANCE);
            } else if ("*".equals(arg)) {
                // Single wildcard argument - matches exactly one argument of any type
                argumentMatchers.add(MethodMatcher.WildcardMatcher.INSTANCE);
            } else if (arg.endsWith("...")) {
                // Java varargs notation (e.g., String...) - matches zero or more of specific type
                String baseType = arg.substring(0, arg.length() - 3).trim();
                // Java varargs ... can coexist with wildcard .., but we still need to track position
                if (varArgsPosition == -1) {
                    varArgsPosition = argumentMatchers.size();
                }
                MethodMatcher.ArgumentMatcher baseMatcher = parseArgumentMatcher(baseType);
                if (baseMatcher instanceof MethodMatcher.StandardArgumentMatcher) {
                    argumentMatchers.add(new MethodMatcher.VarArgsMatcher((MethodMatcher.StandardArgumentMatcher) baseMatcher));
                } else {
                    // This shouldn't happen with valid patterns, but handle it gracefully
                    argumentMatchers.add(baseMatcher);
                }
            } else {
                // Regular type argument
                argumentMatchers.add(parseArgumentMatcher(arg));
            }
        }

        private MethodMatcher.ArgumentMatcher parseArgumentMatcher(String typePattern) {
            if ("*".equals(typePattern)) {
                return MethodMatcher.WildcardMatcher.INSTANCE;
            }

            org.openrewrite.java.TypeMatcher.ParsedType parsed = parseType(typePattern);
            TypeNameMatcher nameMatcher = TypeNameMatcher.fromPattern(parsed.getBaseType());
            return new MethodMatcher.StandardArgumentMatcher(nameMatcher, parsed.getArrayDimensions());
        }


    }
}
