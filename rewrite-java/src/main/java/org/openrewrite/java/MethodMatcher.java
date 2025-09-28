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
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Validated;
import org.openrewrite.java.internal.ThrowingErrorListener;
import org.openrewrite.java.internal.grammar.MethodSignatureLexer;
import org.openrewrite.java.internal.grammar.MethodSignatureParser;
import org.openrewrite.java.internal.grammar.MethodSignatureParserBaseVisitor;
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
        ANTLRErrorListener errorListener = new ThrowingErrorListener(methodPattern);
        MethodSignatureLexer lexer = new MethodSignatureLexer(CharStreams.fromString(methodPattern));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        MethodSignatureParser parser = new MethodSignatureParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        new MethodSignatureParserBaseVisitor<Void>() {

            @SuppressWarnings("ConstantValue")
            @Override
            public @Nullable Void visitMethodPattern(MethodSignatureParser.MethodPatternContext ctx) {
                MethodSignatureParser.TargetTypePatternContext targetTypePatternContext = ctx.targetTypePattern();
                typeMatcher = new TargetTypeMatcherVisitor().visitTargetTypePattern(targetTypePatternContext);
                boolean isPattern = false;

                // Build method name pattern
                String methodNamePattern;
                if (ctx.simpleNamePattern().CONSTRUCTOR() != null) {
                    methodNamePattern = "<constructor>";
                    methodNameMatcher = new ExactMethodNameMatcher(methodNamePattern);
                } else if (ctx.simpleNamePattern().JAVASCRIPT_DEFAULT_METHOD() != null) {
                    methodNamePattern = "<default>";
                    methodNameMatcher = new ExactMethodNameMatcher(methodNamePattern);
                } else {
                    StringBuilder builder = new StringBuilder();
                    for (MethodSignatureParser.SimpleNamePartContext child : ctx.simpleNamePattern().simpleNamePart()) {
                        if (child.WILDCARD() != null) {
                            isPattern = true;
                        }
                        builder.append(child.getText());
                    }
                    methodNamePattern = builder.toString();
                }

                // Create appropriate MethodNameMatcher (if not already set for constructor/default)
                if (methodNameMatcher == null) {
                    if (!isPattern) {
                        // Exact match
                        methodNameMatcher = new ExactMethodNameMatcher(methodNamePattern);
                    } else {
                        // Pattern match (including "*" wildcard) - we already know it has wildcards from isPattern flag
                        AspectJMatcher.PatternType patternType = "*".equals(methodNamePattern) ?
                                AspectJMatcher.PatternType.FullWildcard :
                                AspectJMatcher.PatternType.Wildcard;
                        methodNameMatcher = new PatternMethodNameMatcher(
                                new AspectJMatcher(methodNamePattern, patternType));
                    }
                }

                if (ctx.formalParametersPattern().formalsPattern() == null) {
                    argumentMatchers = new ArrayList<>();  // Empty list for no arguments
                } else if (matchAllArguments(ctx.formalParametersPattern().formalsPattern())) {
                    // For (..), use a single WildcardVarArgsMatcher
                    argumentMatchers = new ArrayList<>();
                    argumentMatchers.add(WildcardVarArgsMatcher.INSTANCE);
                    varArgsPosition = 0;
                } else {
                    FormalParameterVisitor visitor = new FormalParameterVisitor();
                    visitor.visitFormalParametersPattern(ctx.formalParametersPattern());
                    // Capture the ArgumentMatchers and varargs position
                    argumentMatchers = visitor.getMatchers();
                    varArgsPosition = visitor.getVarArgsPosition();
                }
                return null;
            }
        }.visit(parser.methodPattern());
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

    private static boolean matchAllArguments(MethodSignatureParser.FormalsPatternContext context) {
        return context.DOTDOT() != null && context.formalsPatternAfterDotDot() == null;
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


    private AspectJMatcher getTargetTypeMatcher() {
        if (typeMatcher instanceof StandardTypeMatcher) {
            return ((StandardTypeMatcher) typeMatcher).getDelegate();
        }
        throw new UnsupportedOperationException("getTargetTypeMatcher() only supported for StandardTypeMatcher with PatternTypeNameMatcher");
    }

    private boolean matchesTargetTypeName(String fullyQualifiedTypeName) {
        // Delegate to the TypeNameMatcher which handles all the matching logic
        return typeMatcher.matches(fullyQualifiedTypeName);
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
        int actualArgCount = type.getParameterTypes().size();
        if (varArgsPosition == -1) {
            // No varargs - exact count match required
            if (actualArgCount != argumentMatchers.size()) {
                return false;
            }
        } else {
            // With varargs - need at least (matchers - 1) arguments
            // because varargs can match 0 or more arguments
            if (actualArgCount < argumentMatchers.size() - 1) {
                return false;
            }
        }

        if (!matchesTargetType(type.getDeclaringType())) {
            return false;
        }

        return matchesParameterTypes(type.getParameterTypes());
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
        if (enclosing.getType() == null) {
            return false;
        }

        // aspectJUtils does not support matching classes separated by packages.
        // * is a fully wild card match for a method. `* foo()`
        if (!matchesTargetType(enclosing.getType())) {
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
        if (varArgsPosition == -1) {
            // No varargs - exact count match required
            if (actualArgCount != argumentMatchers.size()) {
                return false;
            }
        } else {
            // With varargs - need at least (matchers - 1) arguments
            // because varargs can match 0 or more arguments
            if (actualArgCount < argumentMatchers.size() - 1) {
                return false;
            }
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
            return getTargetTypeMatcher().matchesType(simpleName);
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
            return typeMatcher.matches(builder.toString());
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
        StringJoiner parameters = new StringJoiner(",");
        for (JavaType javaType : method.getParameterTypes()) {
            String s = typePattern(javaType);
            if (s != null) {
                parameters.add(s);
            }
        }

        return typePattern(method.getDeclaringType()) + " " +
                method.getName() + "(" + parameters + ")";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Target type pattern
        sb.append(typeMatcher.toString());
        sb.append(' ');

        // Method name - handle special cases
        // Use the actual pattern from the matcher
        sb.append(methodNameMatcher);

        // Arguments pattern
        sb.append('(');
        if (argumentMatchers.isEmpty()) {
            // No arguments
        } else if (argumentMatchers.size() == 1 && argumentMatchers.get(0) instanceof VarArgsMatcher) {
            sb.append("..");
        } else {
            for (int i = 0; i < argumentMatchers.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                    if (!(argumentMatchers.get(i) instanceof VarArgsMatcher)) {
                        sb.append(' ');
                    }
                }

                ArgumentMatcher matcher = argumentMatchers.get(i);
                sb.append(matcher);
            }
        }
        sb.append(')');

        return sb.toString();
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
        boolean matches(String qualifiedName);

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
    static @Nullable String extractTypeName(@Nullable JavaType type) {
        if (type == null) {
            return null;
        }

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

        return type;
    }

    static class StandardTypeMatcher implements TypeMatcher {
        private final TypeNameMatcher nameMatcher;
        private final int arrayDimensions;
        private final boolean isVarargs;

        StandardTypeMatcher(TypeNameMatcher nameMatcher, int arrayDimensions, boolean isVarargs) {
            this.nameMatcher = nameMatcher;
            this.arrayDimensions = arrayDimensions;
            this.isVarargs = isVarargs;
        }

        @Override
        public boolean matches(JavaType type) {
            type = unwrapArrays(type, arrayDimensions, isVarargs);
            if (type == null || type instanceof JavaType.Unknown) {
                // For null/Unknown types, only match if we have a full wildcard pattern
                return nameMatcher instanceof PatternTypeNameMatcher &&
                        ((PatternTypeNameMatcher) nameMatcher).isFullWildcard();
            }

            String typeName = extractTypeName(type);
            return typeName != null && nameMatcher.matches(typeName);
        }

        @Override
        public boolean matches(String qualifiedName) {
            return nameMatcher.matches(qualifiedName);
        }

        @Override
        public boolean matchesSimpleName(String simpleName) {
            if (nameMatcher instanceof ExactTypeNameMatcher) {
                // Exact match
                String pattern = nameMatcher.getPattern();
                return pattern.equals(simpleName) || pattern.endsWith('.' + simpleName);
            } else {
                String pattern = nameMatcher.getPattern();
                // Also try matching with stripped package wildcards
                // For patterns like com.*.Bar or com..Bar, we want to match just "Bar"
                int lastDot = pattern.lastIndexOf('.');
                if (lastDot > 0 && lastDot < pattern.length() - 1) {
                    int lastPartStart = lastDot + 1;
                    int lastPartLength = pattern.length() - lastPartStart;

                    // Check if last part is just "*"
                    if (lastPartLength == 1 && pattern.charAt(lastPartStart) == '*') {
                        return true;
                    }

                    // Check if last part contains wildcards
                    boolean hasWildcard = false;
                    for (int i = lastPartStart; i < pattern.length(); i++) {
                        if (pattern.charAt(i) == '*') {
                            hasWildcard = true;
                            break;
                        }
                    }

                    if (!hasWildcard) {
                        // Simple exact match
                        return simpleName.length() == lastPartLength &&
                                pattern.regionMatches(lastPartStart, simpleName, 0, lastPartLength);
                    } else {
                        // Has wildcards - use pattern matching on the substring
                        // This is not in hot path (only for unknown types) so substring is acceptable here
                        return simpleName.matches(pattern.substring(lastPartStart).replace("*", ".*"));
                    }
                }
            }
            return false;
        }

        AspectJMatcher getDelegate() {
            if (nameMatcher instanceof PatternTypeNameMatcher) {
                return ((PatternTypeNameMatcher) nameMatcher).getDelegate();
            }
            throw new UnsupportedOperationException("getDelegate() only supported for PatternTypeNameMatcher");
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
        public boolean matches(String qualifiedName) {
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

    static class ExactMethodNameMatcher implements MethodNameMatcher {
        private final String methodName;

        ExactMethodNameMatcher(String methodName) {
            this.methodName = methodName;
        }

        @Override
        public boolean matches(String name) {
            return methodName.equals(name);
        }

        @Override
        public String toString() {
            return methodName;
        }
    }

    static class PatternMethodNameMatcher implements MethodNameMatcher {
        private final AspectJMatcher delegate;

        PatternMethodNameMatcher(AspectJMatcher delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean matches(String methodName) {
            return delegate.matchesMethod(methodName);
        }

        @Override
        public String toString() {
            return delegate.getPattern();
        }
    }

    interface ArgumentMatcher {
        boolean matches(JavaType type);
        boolean matchesUnknown(@Nullable JavaType type);
        int getArrayDimensions();
        boolean isVarargs();

        @Override
        String toString();
    }

    // exact or pattern case
    static class StandardArgumentMatcher implements ArgumentMatcher {
        final TypeNameMatcher nameMatcher;
        private final int arrayDimensions;

        StandardArgumentMatcher(TypeNameMatcher nameMatcher, int arrayDimensions) {
            this.nameMatcher = nameMatcher;
            this.arrayDimensions = arrayDimensions;
        }

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
        public int getArrayDimensions() {
            return arrayDimensions;
        }

        @Override
        public boolean isVarargs() {
            return false;
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
        public int getArrayDimensions() {
            return 0;
        }

        @Override
        public boolean isVarargs() {
            return false;
        }

        @Override
        public String toString() {
            return "*";
        }
    }

    // `type...` case
    static class VarArgsMatcher implements ArgumentMatcher {
        private final StandardArgumentMatcher elementMatcher;

        VarArgsMatcher(StandardArgumentMatcher elementMatcher) {
            this.elementMatcher = elementMatcher;
        }

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
        public int getArrayDimensions() {
            return elementMatcher.getArrayDimensions();
        }

        @Override
        public boolean isVarargs() {
            return true;
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
        public int getArrayDimensions() {
            return 0;
        }

        @Override
        public boolean isVarargs() {
            return true;
        }

        @Override
        public String toString() {
            return "..";
        }
    }
}

/**
 * Simple data class to carry pattern metadata during parsing.
 * Separates pattern string from array/varargs metadata.
 */
class TypePatternInfo {
    final String pattern;
    final int arrayDimensions;
    final boolean isVarargs;
    final AspectJMatcher.@Nullable PatternType patternType;

    TypePatternInfo(String pattern, int arrayDimensions, boolean isVarargs, AspectJMatcher.@Nullable PatternType patternType) {
        this.pattern = pattern;
        this.arrayDimensions = arrayDimensions;
        this.isVarargs = isVarargs;
        this.patternType = patternType;
    }
}

/**
 * Visitor for building AspectJMatcher with metadata from target type patterns.
 * Extracts information like array dimensions directly from the parse tree.
 */
class TargetTypeMatcherVisitor extends MethodSignatureParserBaseVisitor<MethodMatcher.TypeMatcher> {

    @Override
    public MethodMatcher.TypeMatcher visitTargetTypePattern(MethodSignatureParser.TargetTypePatternContext ctx) {
        MethodSignatureParser.ClassNameOrInterfaceContext classNameCtx = ctx.classNameOrInterface();
        if (classNameCtx == null) {
            // Wildcard type - matches everything
            return MethodMatcher.WildcardTypeMatcher.INSTANCE;
        }
        return buildTypeMatcher(classNameCtx);
    }

    static TypePatternInfo buildTypePatternInfo(MethodSignatureParser.ClassNameOrInterfaceContext classNameCtx) {
        StringBuilder patternBuilder = new StringBuilder();
        AspectJMatcher.PatternType patternType = null;
        ParseTree prevChild = null;
        boolean hasWildcards = false;

        int childCount = classNameCtx.getChildCount();
        int arrayDimensions = 0;
        int lastNonArrayIndex = childCount - 1;

        ParseTree lastChild = childCount > 0 ? classNameCtx.getChild(lastNonArrayIndex) : null;
        if (lastChild instanceof MethodSignatureParser.ArrayDimensionsContext) {
            arrayDimensions = countArrayDimensions((MethodSignatureParser.ArrayDimensionsContext) lastChild);
            lastNonArrayIndex--;
        }

        for (int i = 0; i <= lastNonArrayIndex; i++) {
            ParseTree child = classNameCtx.getChild(i);

            boolean isWildcard = child instanceof TerminalNode &&
                    ((TerminalNode) child).getSymbol().getType() == MethodSignatureLexer.WILDCARD;

            boolean isDotDotWildcard = isWildcard &&
                    prevChild instanceof TerminalNode &&
                    ((TerminalNode) prevChild).getSymbol().getType() == MethodSignatureLexer.DOTDOT;

            if (isDotDotWildcard && i == lastNonArrayIndex) {
                patternBuilder.setLength(patternBuilder.length() - 2);
                String pattern = patternBuilder.toString();
                if (pattern.isEmpty() || "*".equals(pattern)) {
                    pattern = "*";
                    patternType = AspectJMatcher.PatternType.FullWildcard;
                } else {
                    patternType = AspectJMatcher.PatternType.PackagePrefix;
                }
                patternBuilder.setLength(0);
                patternBuilder.append(pattern);
            } else {
                String childText = child.getText();
                patternBuilder.append(childText);
                if (!hasWildcards && (childText.contains("*") || childText.contains(".."))) {
                    hasWildcards = true;
                }
            }
            prevChild = child;
        }

        String pattern = patternBuilder.toString();

        if (!pattern.contains(".")) {
            if (Character.isLowerCase(pattern.charAt(0)) && JavaType.Primitive.fromKeyword(pattern) != null) {
                // It's a primitive, keep as-is
            } else {
                if (TypeUtils.findQualifiedJavaLangTypeName(pattern) != null) {
                    pattern = "java.lang." + pattern;
                }
            }
        }

        // If patternType wasn't set (i.e., not a PackagePrefix), determine it now
        if (patternType == null) {
            if ("*".equals(pattern) || "*..*".equals(pattern)) {
                patternType = AspectJMatcher.PatternType.FullWildcard;
            } else if (hasWildcards) {
                patternType = AspectJMatcher.PatternType.Wildcard;
            } else {
                patternType = AspectJMatcher.PatternType.Exact;
            }
        }

        return new TypePatternInfo(pattern, arrayDimensions, false, patternType);
    }

    static MethodMatcher.TypeMatcher buildTypeMatcher(MethodSignatureParser.ClassNameOrInterfaceContext classNameCtx) {
        TypePatternInfo info = buildTypePatternInfo(classNameCtx);

        // Check for full wildcard pattern with no arrays - return WildcardTypeMatcher
        if (info.patternType == AspectJMatcher.PatternType.FullWildcard && info.arrayDimensions == 0 && !info.isVarargs) {
            return MethodMatcher.WildcardTypeMatcher.INSTANCE;
        }

        // Create matcher based on pattern type (already computed during parsing)
        TypeNameMatcher nameMatcher;
        if (info.patternType == AspectJMatcher.PatternType.Exact) {
            // Exact pattern - no wildcards
            nameMatcher = new ExactTypeNameMatcher(info.pattern);
        } else {
            // Pattern with wildcards - use AspectJMatcher
            AspectJMatcher aspectJMatcher = new AspectJMatcher(info.pattern, info.patternType);
            nameMatcher = new PatternTypeNameMatcher(aspectJMatcher);
        }
        return new MethodMatcher.StandardTypeMatcher(nameMatcher, info.arrayDimensions, info.isVarargs);
    }

    private static int countArrayDimensions(MethodSignatureParser.@Nullable ArrayDimensionsContext ctx) {
        int count = 0;
        while (ctx != null) {
            count++;
            ctx = ctx.arrayDimensions();
        }
        return count;
    }
}

/**
 * Visitor for building AspectJMatcher with metadata from formal type patterns.
 * Extracts array dimensions and varargs information directly from the parse tree.
 */
class FormalTypeMatcherVisitor extends MethodSignatureParserBaseVisitor<TypePatternInfo> {

    @Override
    public TypePatternInfo visitFormalTypePattern(MethodSignatureParser.FormalTypePatternContext ctx) {
        MethodSignatureParser.ClassNameOrInterfaceContext classNameCtx = ctx.classNameOrInterface();
        if (classNameCtx == null) {
            return new TypePatternInfo("*", 0, false, AspectJMatcher.PatternType.FullWildcard);
        }
        return TargetTypeMatcherVisitor.buildTypePatternInfo(classNameCtx);
    }
}

/**
 * Visitor for building ArgumentMatchers from the formal parameters pattern.
 * This is used during MethodMatcher construction to set up efficient matching.
 */
class FormalParameterVisitor extends MethodSignatureParserBaseVisitor<Void> {
    private final List<MethodMatcher.ArgumentMatcher> matchers = new ArrayList<>();
    private int varArgsPos = -1;
    private final FormalTypeMatcherVisitor typeMatcherVisitor = new FormalTypeMatcherVisitor();
    private boolean nextTypeIsVarargs = false;

    public List<MethodMatcher.ArgumentMatcher> getMatchers() {
        return matchers;
    }

    public int getVarArgsPosition() {
        return varArgsPos;
    }

    @Override
    public Void visitFormalTypePattern(MethodSignatureParser.FormalTypePatternContext ctx) {
        if (ctx.classNameOrInterface() != null && isWildcardOnly(ctx.classNameOrInterface())) {
            matchers.add(MethodMatcher.WildcardMatcher.INSTANCE);
        } else {
            TypePatternInfo info = typeMatcherVisitor.visitFormalTypePattern(ctx);

            // Update info if this is a varargs parameter
            if (nextTypeIsVarargs) {
                info = new TypePatternInfo(info.pattern, info.arrayDimensions, true, info.patternType);
            }

            // Create matcher based on pattern type (already computed during parsing)
            TypeNameMatcher nameMatcher;
            if (info.patternType == AspectJMatcher.PatternType.Exact) {
                // Exact pattern - no wildcards
                nameMatcher = new ExactTypeNameMatcher(info.pattern);
            } else {
                // Pattern with wildcards - use AspectJMatcher
                AspectJMatcher aspectJMatcher = new AspectJMatcher(info.pattern, info.patternType);
                nameMatcher = new PatternTypeNameMatcher(aspectJMatcher);
            }

            MethodMatcher.StandardArgumentMatcher baseMatcher = new MethodMatcher.StandardArgumentMatcher(
                nameMatcher,
                info.arrayDimensions
            );

            if (info.isVarargs) {
                matchers.add(new MethodMatcher.VarArgsMatcher(baseMatcher));
            } else {
                matchers.add(baseMatcher);
            }
        }
        nextTypeIsVarargs = false;
        return super.visitFormalTypePattern(ctx);
    }

    @Override
    public Void visitFormalsPattern(MethodSignatureParser.FormalsPatternContext ctx) {
        if (ctx.DOTDOT() != null) {
            if (varArgsPos == -1) {
                varArgsPos = matchers.size();
                matchers.add(MethodMatcher.WildcardVarArgsMatcher.INSTANCE);
            }
        }

        if (ctx.ELLIPSIS() != null) {
            nextTypeIsVarargs = true;
            if (varArgsPos == -1) {
                varArgsPos = matchers.size();
            }
        }

        super.visitFormalsPattern(ctx);
        return null;
    }

    private static boolean isWildcardOnly(MethodSignatureParser.ClassNameOrInterfaceContext ctx) {
        // Check if it's exactly one child and it's a wildcard token
        if (ctx.getChildCount() == 1) {
            ParseTree child = ctx.getChild(0);
            if (child instanceof TerminalNode) {
                TerminalNode node = (TerminalNode) child;
                return node.getSymbol().getType() == MethodSignatureLexer.WILDCARD;
            }
        }
        return false;
    }
}
