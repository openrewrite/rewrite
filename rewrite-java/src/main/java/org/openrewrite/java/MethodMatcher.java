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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.internal.ThrowingErrorListener;
import org.openrewrite.java.internal.grammar.MethodSignatureLexer;
import org.openrewrite.java.internal.grammar.MethodSignatureParser;
import org.openrewrite.java.internal.grammar.MethodSignatureParserBaseVisitor;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

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

    private String targetType;

    @Nullable
    private volatile AspectJMatcher targetTypeMatcher;

    @Nullable
    private String methodName;

    @Nullable
    private AspectJMatcher methodNameMatcher;

    private List<ArgumentMatcher> argumentMatchers;

    private int varArgsPosition = -1;

    /**
     * Whether to match overridden forms of the method on subclasses of {@link #targetType}.
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

            @Override
            public @Nullable Void visitMethodPattern(MethodSignatureParser.MethodPatternContext ctx) {
                MethodSignatureParser.TargetTypePatternContext targetTypePatternContext = ctx.targetTypePattern();
                targetType = new TypeVisitor().visitTargetTypePattern(targetTypePatternContext);

                if (ctx.simpleNamePattern().CONSTRUCTOR() != null) {
                    methodName = "<constructor>";
                } else if (ctx.simpleNamePattern().JAVASCRIPT_DEFAULT_METHOD() != null) {
                    methodName = "<default>";
                } else if (isPlainIdentifier(ctx.simpleNamePattern())) {
                    StringBuilder builder = new StringBuilder();
                    for (ParseTree child : ctx.simpleNamePattern().children) {
                        builder.append(child.getText());
                    }
                    methodName = builder.toString();
                } else {
                    StringBuilder builder = new StringBuilder();
                    for (ParseTree child : ctx.simpleNamePattern().children) {
                        builder.append(child.getText());
                    }
                    String pattern = builder.toString();
                    // Create AspectJMatcher for wildcard method names
                    methodNameMatcher = new AspectJMatcher(pattern);
                }

                if (ctx.formalParametersPattern().formalsPattern() == null) {
                    argumentMatchers = new ArrayList<>();  // Empty list for no arguments
                } else if (matchAllArguments(ctx.formalParametersPattern().formalsPattern())) {
                    // For (..), use a single VarArgsMatcher
                    argumentMatchers = new ArrayList<>();
                    argumentMatchers.add(new VarArgsMatcher());
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

    private static boolean isPlainIdentifier(MethodSignatureParser.SimpleNamePatternContext context) {
        // Check if it's JAVASCRIPT_DEFAULT_METHOD or CONSTRUCTOR
        if (context.JAVASCRIPT_DEFAULT_METHOD() != null || context.CONSTRUCTOR() != null) {
            return true;
        }
        // Check if it has simpleNamePart children (which could contain wildcards)
        List<MethodSignatureParser.SimpleNamePartContext> parts = context.simpleNamePart();
        if (parts.isEmpty()) {
            return false;
        }
        // It's plain if all parts are Identifiers (no wildcards)
        for (MethodSignatureParser.SimpleNamePartContext part : parts) {
            if (part.WILDCARD() != null) {
                return false;
            }
        }
        return true;
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

    @Deprecated
    public Pattern getTargetTypePattern() {
        // For backward compatibility, convert to Pattern
        return Pattern.compile(StringUtils.aspectjNameToPattern(targetType));
    }

    @Deprecated
    public Pattern getMethodNamePattern() {
        // For backward compatibility, convert to Pattern
        if (methodName != null) {
            return Pattern.compile(methodName);
        } else if (methodNameMatcher != null) {
            // Reconstruct pattern from matcher
            return Pattern.compile("[^.]*");  // Simple wildcard pattern
        }
        return Pattern.compile(".*");
    }

    @Deprecated
    public Pattern getArgumentPattern() {
        // Generate pattern string from argumentMatchers
        return Pattern.compile(generatePatternFromMatchers());
    }

    private AspectJMatcher getTargetTypeMatcher() {
        AspectJMatcher matcher = targetTypeMatcher;
        if (matcher == null) {
            synchronized (this) {
                matcher = targetTypeMatcher;
                if (matcher == null) {
                    targetTypeMatcher = matcher = new AspectJMatcher(targetType);
                }
            }
        }
        return matcher;
    }

    private String generatePatternFromMatchers() {
        if (argumentMatchers.isEmpty()) {
            return "";
        }

        // Special case: single VarArgsMatcher means (..) which matches anything
        if (argumentMatchers.size() == 1 && argumentMatchers.get(0) instanceof VarArgsMatcher) {
            return "(([^,]+,)*([^,]+))?";
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < argumentMatchers.size(); i++) {
            ArgumentMatcher matcher = argumentMatchers.get(i);

            if (matcher instanceof VarArgsMatcher) {
                // VarArgs (..) can match zero or more arguments
                // DOT_DOT regex is ([^,]+,)*([^,]+) which matches one or more
                // We need to make it optional to match zero
                String dotDotRegex = "([^,]+,)*([^,]+)";
                if (argumentMatchers.size() == 1) {
                    // Just (..) - matches zero or more
                    builder.append('(').append(dotDotRegex).append(")?");
                } else if (i == 0) {
                    // (.., other args) - need special handling for first argument after ..
                    builder.append('(').append(dotDotRegex).append(",)?");
                } else {
                    // (other args, ..) - varargs at end
                    builder.append("(,").append(dotDotRegex).append(")?");
                }
            } else if (matcher instanceof WildcardMatcher) {
                // Handle comma placement for arguments after initial ..
                if (i == 1 && argumentMatchers.get(0) instanceof VarArgsMatcher) {
                    builder.append("([^,]+)");
                } else if (i > 0) {
                    builder.append(",([^,]+)");
                } else {
                    builder.append("([^,]+)");
                }
            } else if (matcher instanceof ExactTypeMatcher) {
                String pattern = ((ExactTypeMatcher) matcher).getPatternString();
                // Handle comma placement
                if (i == 1 && argumentMatchers.get(0) instanceof VarArgsMatcher) {
                    // First arg after .., no comma prefix
                    builder.append(pattern);
                } else if (i > 0) {
                    builder.append(",").append(pattern);
                } else {
                    builder.append(pattern);
                }
            }
        }

        return builder.toString();
    }

    private boolean matchesTargetTypeName(String fullyQualifiedTypeName) {
        return getTargetTypeMatcher().matches(fullyQualifiedTypeName);
    }

    boolean matchesTargetType(JavaType.@Nullable FullyQualified type) {
        // For unknown types, allow if pattern contains wildcards
        if (type == null || type instanceof JavaType.Unknown) {
            return targetType.contains("*") || targetType.contains("..");
        }
        return TypeUtils.isOfTypeWithName(
                type,
                matchOverrides,
                this::matchesTargetTypeName
        );
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean matchesMethodName(String name) {
        if (this.methodName != null) {
            return this.methodName.equals(name);
        }
        return methodNameMatcher != null && methodNameMatcher.matches(name);
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
                if (!argumentMatchers.get(i).matches(types.get(i))) {
                    return false;
                }
            }
            return true;
        } else {
            // Has varargs - match before and after
            int beforeCount = varArgsPosition;
            int afterCount = argumentMatchers.size() - varArgsPosition - 1;

            if (types.size() < beforeCount + afterCount) {
                return false;
            }

            // Match before varargs
            for (int i = 0; i < beforeCount; i++) {
                if (!argumentMatchers.get(i).matches(types.get(i))) {
                    return false;
                }
            }

            // Match after varargs
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
        boolean matchesTargetType = "*".equals(targetType) || matchesTargetType(enclosing.getType());
        if (!matchesTargetType) {
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

        // aspectJUtils does not support matching classes separated by packages.
        // * is a fully wild card match for a method. `* foo()`
        boolean matchesTargetType = "*".equals(targetType) || TypeUtils.isAssignableTo(targetType, enclosing.getType());
        if (!matchesTargetType) {
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

        // When checking receiver with unknown types, we still need to match the name pattern
        // but we're more lenient about it
        if (method.getSelect() instanceof J.Identifier) {
            J.Identifier select = (J.Identifier) method.getSelect();
            // Always check the pattern - but for unknown types we use the simple name alone
            if (!matchesSelectBySimpleNameAlone(select)) {
                return false;
            }
        }

        // For unknown types, we need to be more lenient with argument matching
        return matchesArgumentsAllowingUnknownTypes(method);
    }

    private boolean matchesArgumentsAllowingUnknownTypes(J.MethodInvocation method) {
        // Try the new ArgumentMatcher approach first if available
        return matchesArgumentsWithMatchers(method.getArguments(), true);
    }

    private boolean matchesArgumentsWithMatchers(List<Expression> arguments, boolean allowUnknownTypes) {
        if (varArgsPosition == -1) {
            // No varargs - exact match required
            if (arguments.size() != argumentMatchers.size()) {
                return false;
            }
            for (int i = 0; i < arguments.size(); i++) {
                if (!argumentMatchers.get(i).matchesExpression(arguments.get(i), allowUnknownTypes)) {
                    return false;
                }
            }
            return true;
        } else {
            // Has varargs - match before and after
            int beforeCount = varArgsPosition;
            int afterCount = argumentMatchers.size() - varArgsPosition - 1;

            if (arguments.size() < beforeCount + afterCount) {
                return false;
            }

            // Match before varargs
            for (int i = 0; i < beforeCount; i++) {
                if (!argumentMatchers.get(i).matchesExpression(arguments.get(i), allowUnknownTypes)) {
                    return false;
                }
            }

            // Match after varargs
            for (int i = 0; i < afterCount; i++) {
                int argIndex = arguments.size() - afterCount + i;
                int matcherIndex = varArgsPosition + 1 + i;
                if (!argumentMatchers.get(matcherIndex).matchesExpression(arguments.get(argIndex), allowUnknownTypes)) {
                    return false;
                }
            }

            return true;
        }
    }

    private boolean matchesSelectBySimpleNameAlone(J.Identifier select) {
        String simpleName = select.getSimpleName();
        if (!targetType.contains("*") && !targetType.contains("..")) {
            // Exact match
            return targetType.equals(simpleName) || targetType.endsWith('.' + simpleName);
        } else {
            AspectJMatcher matcher = getTargetTypeMatcher();
            // Try matching just the simple name
            if (matcher.matches(simpleName)) {
                return true;
            }
            // Also try matching with stripped package wildcards
            // For patterns like com.*.Bar or com..Bar, we want to match just "Bar"
            int lastDot = targetType.lastIndexOf('.');
            if (lastDot > 0 && lastDot < targetType.length() - 1) {
                int lastPartStart = lastDot + 1;
                int lastPartLength = targetType.length() - lastPartStart;

                // Check if last part is just "*"
                if (lastPartLength == 1 && targetType.charAt(lastPartStart) == '*') {
                    return true;
                }

                // Check if last part contains wildcards
                boolean hasWildcard = false;
                for (int i = lastPartStart; i < targetType.length(); i++) {
                    if (targetType.charAt(i) == '*') {
                        hasWildcard = true;
                        break;
                    }
                }

                if (!hasWildcard) {
                    // Simple exact match
                    return simpleName.length() == lastPartLength &&
                            targetType.regionMatches(lastPartStart, simpleName, 0, lastPartLength);
                } else {
                    // Has wildcards - use pattern matching on the substring
                    // This is not in hot path (only for unknown types) so substring is acceptable here
                    return simpleName.matches(targetType.substring(lastPartStart).replace("*", ".*"));
                }
            }
        }
        return false;
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
        if (methodName != null && !methodName.equals(fieldAccess.getName().getSimpleName())) {
            return false;
        } else if (methodNameMatcher != null && !methodNameMatcher.matches(fieldAccess.getName().getSimpleName())) {
            return false;
        }

        Expression target = fieldAccess.getTarget();
        if (target instanceof J.Identifier) {
            String simpleName = ((J.Identifier) target).getSimpleName();
            return getTargetTypeMatcher().matches(simpleName);
        } else if (target instanceof J.FieldAccess) {
            return ((J.FieldAccess) target).isFullyQualifiedClassReference(targetType);
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
        sb.append(targetType);
        sb.append(' ');

        // Method name - handle special cases
        if (methodName != null) {
            sb.append(methodName);
        } else if (methodNameMatcher != null) {
            // Use the actual pattern from the matcher
            sb.append(methodNameMatcher.getPattern());
        } else {
            sb.append('*');
        }

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
                if (matcher instanceof VarArgsMatcher) {
                    sb.append("..");
                } else if (matcher instanceof ExactTypeMatcher) {
                    sb.append(((ExactTypeMatcher) matcher).typeName);
                } else if (matcher instanceof WildcardMatcher) {
                    sb.append('*');
                } else {
                    // Fallback - shouldn't happen
                    sb.append("?");
                }
            }
        }
        sb.append(')');

        return sb.toString();
    }

    // ============ Simple AspectJ pattern matcher ============

    /**
     * Simple AspectJ-style pattern matcher that avoids regex compilation.
     * Supports:
     * - * matches any sequence of characters except '.'
     * - .. matches any sequence of packages/subpackages
     * - Literal matching for everything else
     */
    static class AspectJMatcher {
        private final String pattern;
        private final boolean isFullWildcard;   // Just "*"
        private final boolean containsDotDot;
        private final boolean isSimplePattern;  // No wildcards

        AspectJMatcher(String pattern) {
            this.pattern = pattern;
            if ("*".equals(pattern) || "*..*".equals(pattern)) {
                this.isFullWildcard = true;
                this.containsDotDot = false;
                this.isSimplePattern = false;
            } else {
                this.isFullWildcard = false;
                this.containsDotDot = pattern.contains("..");
                this.isSimplePattern = !pattern.contains("*") && !containsDotDot;
            }
        }

        String getPattern() {
            return pattern;
        }

        boolean matches(String text) {
            if (isFullWildcard) {
                return true;
            }
            if (isSimplePattern) {
                // For simple patterns, handle $ vs . equivalence for inner classes
                if (pattern.equals(text)) {
                    return true;
                }
                // Check if they're equal when normalized ($ replaced with .)
                if (pattern.length() == text.length()) {
                    return pattern.replace('$', '.').equals(text.replace('$', '.'));
                }
                return false;
            }

            // Optimize pattern *..*Something (any packages + class name pattern)
            if (pattern.length() > 3 && pattern.charAt(0) == '*' && pattern.charAt(1) == '.' && pattern.charAt(2) == '.') {
                // Find the simple class name (part after last dot or dollar sign for inner classes)
                int lastDot = text.lastIndexOf('.');
                int lastDollar = text.lastIndexOf('$');
                int lastSeparator = Math.max(lastDot, lastDollar);

                if (lastSeparator >= 0) {
                    // Match pattern after *.. against simple name only
                    return matchesPattern(pattern, text, 3, lastSeparator + 1);
                } else {
                    // No package, match pattern after *.. against entire text
                    return matchesPattern(pattern, text, 3, 0);
                }
            }

            // Optimize common patterns like *Suffix or Prefix*
            if (pattern.charAt(0) == '*' && !containsDotDot) {
                int nextStar = pattern.indexOf('*', 1);
                if (nextStar == -1) {
                    // Pattern like *Suffix - just check if text ends with Suffix
                    int suffixLength = pattern.length() - 1;
                    if (text.length() >= suffixLength &&
                        text.regionMatches(text.length() - suffixLength, pattern, 1, suffixLength)) {
                        // Also verify no dots (or dollars for inner classes) in the prefix part
                        for (int i = 0; i < text.length() - suffixLength; i++) {
                            char c = text.charAt(i);
                            if (c == '.' || c == '$') {
                                return false;
                            }
                        }
                        return true;
                    }
                    return false;
                } else if (nextStar == pattern.length() - 1) {
                    // Pattern like *Middle* - check if text contains Middle
                    int middleStart = 1;
                    int middleLength = nextStar - 1;

                    // Search for the middle part
                    for (int i = 0; i <= text.length() - middleLength; i++) {
                        if (text.regionMatches(i, pattern, middleStart, middleLength)) {
                            // Found it - check no dots or dollars before or after
                            for (int j = 0; j < i; j++) {
                                char c = text.charAt(j);
                                if (c == '.' || c == '$') {
                                    return false;
                                }
                            }
                            for (int j = i + middleLength; j < text.length(); j++) {
                                char c = text.charAt(j);
                                if (c == '.' || c == '$') {
                                    return false;
                                }
                            }
                            return true;
                        }
                    }
                    return false;
                }
            } else if (pattern.charAt(pattern.length() - 1) == '*' && !containsDotDot) {
                // Pattern like Prefix* - just check if text starts with Prefix
                int prefixLength = pattern.length() - 1;
                if (text.length() >= prefixLength &&
                    text.regionMatches(0, pattern, 0, prefixLength)) {
                    // Also verify no dots or dollars in the suffix part
                    for (int i = prefixLength; i < text.length(); i++) {
                        char c = text.charAt(i);
                        if (c == '.' || c == '$') {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }

            return matchesPattern(pattern, text, 0, 0);
        }

        private boolean matchesPattern(String pattern, String text, int pIdx, int tIdx) {
            // Match character by character
            while (pIdx < pattern.length()) {
                // Check if we've run out of text
                if (tIdx >= text.length()) {
                    // If remaining pattern is all wildcards, it's still a match
                    while (pIdx < pattern.length()) {
                        char p = pattern.charAt(pIdx);
                        if (p == '*') {
                            pIdx    ++;
                        } else if (p == '.' && pIdx + 1 < pattern.length() && pattern.charAt(pIdx + 1) == '.') {
                            pIdx += 2;  // .. at end is OK
                        } else {
                            return false;  // Non-wildcard characters left
                        }
                    }
                    return true;
                }

                char p = pattern.charAt(pIdx);

                if (p == '*') {
                    // * matches any characters except '.'
                    pIdx++;
                    if (pIdx >= pattern.length()) {
                        // * at end matches rest of text (no dots)
                        while (tIdx < text.length()) {
                            if (text.charAt(tIdx) == '.') return false;
                            tIdx++;
                        }
                        return true;
                    }

                    // Look ahead for the next literal segment
                    int segmentStart = pIdx;
                    int segmentEnd = segmentStart;
                    while (segmentEnd < pattern.length() && pattern.charAt(segmentEnd) != '*' &&
                           !(pattern.charAt(segmentEnd) == '.' && segmentEnd + 1 < pattern.length() &&
                             pattern.charAt(segmentEnd + 1) == '.')) {
                        segmentEnd++;
                    }

                    if (segmentStart < segmentEnd) {
                        // We have a literal segment to find
                        int segmentLength = segmentEnd - segmentStart;

                        // Try to find this segment in the remaining text
                        while (tIdx <= text.length() - segmentLength) {
                            if (text.charAt(tIdx) == '.') {
                                return false;  // * can't match dots
                            }

                            if (text.regionMatches(tIdx, pattern, segmentStart, segmentLength)) {
                                // Found the segment, continue matching after it
                                if (matchesPattern(pattern, text, segmentEnd, tIdx + segmentLength)) {
                                    return true;
                                }
                            }
                            tIdx++;
                        }
                        return false;
                    }

                    // Fall back to original algorithm for edge cases
                    // Try to match rest of pattern at each position
                    if (matchesPattern(pattern, text, pIdx, tIdx)) {
                        return true;
                    }
                    while (tIdx < text.length()) {
                        if (text.charAt(tIdx) == '.') {
                            return false;  // * doesn't match '.'
                        }
                        tIdx++;
                        if (matchesPattern(pattern, text, pIdx, tIdx)) {
                            return true;
                        }
                    }
                    return false;

                } else if (p == '.' && pIdx + 1 < pattern.length() && pattern.charAt(pIdx + 1) == '.') {
                    // .. matches any sequence of packages
                    pIdx += 2;
                    if (pIdx >= pattern.length()) {
                        return true;  // .. at end matches everything
                    }

                    // Try to match rest of pattern at each position
                    while (tIdx <= text.length()) {
                        if (matchesPattern(pattern, text, pIdx, tIdx)) {
                            return true;
                        }
                        tIdx++;
                    }
                    return false;

                } else if (p == '.') {
                    // Single dot should match '.' or '$' (inner class)
                    if (text.charAt(tIdx) != '.' && text.charAt(tIdx) != '$') {
                        return false;
                    }
                    pIdx++;
                    tIdx++;

                } else {
                    // Literal character match
                    if (p != text.charAt(tIdx)) {
                        return false;
                    }
                    pIdx++;
                    tIdx++;
                }
            }

            // Check if we've consumed both pattern and text
            return tIdx >= text.length();
        }
    }

    // ============ ArgumentMatcher hierarchy for efficient matching ============

    interface ArgumentMatcher {
        boolean matches(@Nullable JavaType type);

        boolean matchesExpression(Expression expr, boolean allowUnknownTypes);
    }

    static class ExactTypeMatcher implements ArgumentMatcher {
        final String typeName;
        @Nullable
        private AspectJMatcher matcher;

        ExactTypeMatcher(String typeName) {
            this.typeName = typeName;
        }

        String getPatternString() {
            // Handle varargs: replace ... with [] before pattern conversion
            String adjustedTypeName = typeName.replace("...", "[]");

            // If the type name doesn't contain a dot (unqualified), it should match
            // both simple names and fully qualified names ending with that simple name
            if (!adjustedTypeName.contains(".")) {
                // Match either the simple name or any.package.SimpleName
                return "(.*[.$])?" + StringUtils.aspectjNameToPattern(adjustedTypeName);
            }
            return StringUtils.aspectjNameToPattern(adjustedTypeName);
        }

        private AspectJMatcher getMatcher() {
            if (matcher == null) {
                // Handle varargs: replace ... with []
                String adjustedTypeName = typeName.replace("...", "[]");

                // If unqualified, prepend .. to match any package
                if (!adjustedTypeName.contains(".")) {
                    matcher = new AspectJMatcher(".." + adjustedTypeName);
                } else {
                    matcher = new AspectJMatcher(adjustedTypeName);
                }
            }
            return matcher;
        }

        @Override
        public boolean matches(@Nullable JavaType type) {
            if (type == null) {
                return false;
            }
            String typeStr = typePattern(type);
            if (typeStr == null) {
                return false;
            }

            // For unqualified types, try both with and without package
            if (!typeName.contains(".")) {
                String adjustedTypeName = typeName.replace("...", "[]");
                // Direct match on simple name
                int lastDot = typeStr.lastIndexOf('.');
                if (lastDot >= 0) {
                    String simpleName = typeStr.substring(lastDot + 1);
                    if (adjustedTypeName.equals(simpleName)) {
                        return true;
                    }
                }
            }

            return getMatcher().matches(typeStr);
        }

        @Override
        public boolean matchesExpression(Expression expr, boolean allowUnknownTypes) {
            JavaType type = expr.getType();
            if (type == null || type instanceof JavaType.Unknown) {
                return allowUnknownTypes;
            }
            return matches(type);
        }
    }

    static class WildcardMatcher implements ArgumentMatcher {
        @Override
        public boolean matches(@Nullable JavaType type) {
            return type != null;  // * matches any single type
        }

        @Override
        public boolean matchesExpression(Expression expr, boolean allowUnknownTypes) {
            JavaType type = expr.getType();
            return type != null || allowUnknownTypes;
        }
    }

    static class VarArgsMatcher implements ArgumentMatcher {
        // Special marker - actual varargs matching is handled by MethodMatcher
        @Override
        public boolean matches(@Nullable JavaType type) {
            throw new UnsupportedOperationException("VarArgs matching should be handled by MethodMatcher");
        }

        @Override
        public boolean matchesExpression(Expression expr, boolean allowUnknownTypes) {
            throw new UnsupportedOperationException("VarArgs matching should be handled by MethodMatcher");
        }
    }
}

class TypeVisitor extends MethodSignatureParserBaseVisitor<String> {
    @Override
    public String visitClassNameOrInterface(MethodSignatureParser.ClassNameOrInterfaceContext ctx) {
        StringBuilder classNameBuilder = new StringBuilder();
        for (ParseTree c : ctx.children) {
            classNameBuilder.append(c.getText());
        }
        String className = classNameBuilder.toString();

        if (!className.contains(".")) {
            int arrInit = className.lastIndexOf('[');
            String beforeArr = arrInit == -1 ? className : className.substring(0, arrInit);
            if (Character.isLowerCase(beforeArr.charAt(0)) && JavaType.Primitive.fromKeyword(beforeArr) != null) {
                return className;
            } else {
                if (TypeUtils.findQualifiedJavaLangTypeName(beforeArr) != null) {
                    return "java.lang." + className;
                }
            }
        }

        return className;
    }
}

/**
 * Visitor for building ArgumentMatchers from the formal parameters pattern.
 * This is used during MethodMatcher construction to set up efficient matching.
 */
class FormalParameterVisitor extends MethodSignatureParserBaseVisitor<Void> {
    private final List<MethodMatcher.ArgumentMatcher> matchers = new ArrayList<>();
    private int varArgsPos = -1;

    public List<MethodMatcher.ArgumentMatcher> getMatchers() {
        return matchers;
    }

    public int getVarArgsPosition() {
        return varArgsPos;
    }

    @Override
    public Void visitFormalTypePattern(MethodSignatureParser.FormalTypePatternContext ctx) {
        if (ctx.classNameOrInterface() != null && isWildcardOnly(ctx.classNameOrInterface())) {
            matchers.add(new MethodMatcher.WildcardMatcher());
        } else {
            // Build the type name from the context
            String typeName = new TypeVisitor().visitFormalTypePattern(ctx);
            matchers.add(new MethodMatcher.ExactTypeMatcher(typeName));
        }
        return super.visitFormalTypePattern(ctx);
    }

    @Override
    public Void visitFormalsPattern(MethodSignatureParser.FormalsPatternContext ctx) {
        if (ctx.DOTDOT() != null) {
            if (varArgsPos == -1) {  // Only track the first varargs position
                varArgsPos = matchers.size();
                matchers.add(new MethodMatcher.VarArgsMatcher());
            }
        }
        // Visit children to process formalTypePattern
        super.visitFormalsPattern(ctx);

        // After visiting formalTypePattern, check if ELLIPSIS was present
        if (ctx.ELLIPSIS() != null && !matchers.isEmpty()) {
            // Mark the last added matcher as varargs by replacing it
            MethodMatcher.ArgumentMatcher lastMatcher = matchers.get(matchers.size() - 1);
            if (lastMatcher instanceof MethodMatcher.ExactTypeMatcher) {
                String typeName = ((MethodMatcher.ExactTypeMatcher) lastMatcher).typeName;
                // Replace with varargs version
                matchers.set(matchers.size() - 1, new MethodMatcher.ExactTypeMatcher(typeName + "..."));
            }
        }
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
