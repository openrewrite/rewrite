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
import lombok.Setter;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.grammar.MethodSignatureLexer;
import org.openrewrite.java.internal.grammar.MethodSignatureParser;
import org.openrewrite.java.internal.grammar.MethodSignatureParserBaseVisitor;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.java.tree.TypeUtils.fullyQualifiedNamesAreEqual;

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
    private static final String ASPECTJ_DOT_PATTERN = StringUtils.aspectjNameToPattern(".");
    private static final String ASPECTJ_DOTDOT_PATTERN = StringUtils.aspectjNameToPattern("..");
    private static final Pattern EMPTY_ARGUMENTS_PATTERN = Pattern.compile("");
    private static final Pattern ANY_ARGUMENTS_PATTERN = Pattern.compile(".*");

    @Nullable
    private Pattern targetTypePattern;

    @Nullable
    private Pattern methodNamePattern;

    private Pattern argumentPattern;

    @Nullable
    private String targetType;

    @Nullable
    private String methodName;

    /**
     * Whether to match overridden forms of the method on subclasses of {@link #targetTypePattern}.
     */
    @Getter
    private final boolean matchOverrides;

    public MethodMatcher(String signature, @Nullable Boolean matchOverrides) {
        this(signature, Boolean.TRUE.equals(matchOverrides));
    }

    public MethodMatcher(String signature, boolean matchOverrides) {
        this.matchOverrides = matchOverrides;

        MethodSignatureParser parser = new MethodSignatureParser(new CommonTokenStream(new MethodSignatureLexer(
                CharStreams.fromString(signature))));

        new MethodSignatureParserBaseVisitor<Void>() {
            @Override
            public Void visitMethodPattern(MethodSignatureParser.MethodPatternContext ctx) {
                MethodSignatureParser.TargetTypePatternContext targetTypePatternContext = ctx.targetTypePattern();
                String pattern = new TypeVisitor().visitTargetTypePattern(targetTypePatternContext);
                if (isPlainIdentifier(targetTypePatternContext)) {
                    targetType = pattern;
                } else {
                    targetTypePattern = Pattern.compile(StringUtils.aspectjNameToPattern(pattern));
                }

                if (isPlainIdentifier(ctx.simpleNamePattern())) {
                    StringBuilder builder = new StringBuilder();
                    for (ParseTree child : ctx.simpleNamePattern().children) {
                        builder.append(child.getText());
                    }
                    methodName = builder.toString();
                } else {
                    StringBuilder builder = new StringBuilder();
                    for (ParseTree child : ctx.simpleNamePattern().children) {
                        builder.append(StringUtils.aspectjNameToPattern(child.getText()));
                    }
                    methodNamePattern = Pattern.compile(builder.toString());
                }

                if (ctx.formalParametersPattern().formalsPattern() == null) {
                    argumentPattern = EMPTY_ARGUMENTS_PATTERN;
                } else if (matchAllArguments(ctx.formalParametersPattern().formalsPattern())) {
                    argumentPattern = ANY_ARGUMENTS_PATTERN;
                } else {
                    argumentPattern = Pattern.compile(new FormalParameterVisitor().visitFormalParametersPattern(
                            ctx.formalParametersPattern()));
                }
                return null;
            }
        }.visit(parser.methodPattern());
    }

    private static boolean matchAllArguments(MethodSignatureParser.FormalsPatternContext context) {
        return context.dotDot() != null && context.formalsPatternAfterDotDot() == null;
    }

    private static boolean isPlainIdentifier(MethodSignatureParser.TargetTypePatternContext context) {
        return context.BANG() == null &&
               context.AND() == null &&
               context.OR() == null &&
               context.classNameOrInterface().DOTDOT().isEmpty() &&
               context.classNameOrInterface().WILDCARD().isEmpty();
    }

    private static boolean isPlainIdentifier(MethodSignatureParser.SimpleNamePatternContext context) {
        return context.WILDCARD().isEmpty();
    }

    public MethodMatcher(J.MethodDeclaration method, boolean matchOverrides) {
        this(methodPattern(method), matchOverrides);
    }

    public MethodMatcher(String signature) {
        this(signature, false);
    }

    public MethodMatcher(J.MethodDeclaration method) {
        this(method, false);
    }

    public MethodMatcher(JavaType.Method method) {
        this(methodPattern(method), false);
    }

    @Deprecated
    public Pattern getTargetTypePattern() {
        return targetTypePattern != null ? targetTypePattern : Pattern.compile(requireNonNull(targetType));
    }

    @Deprecated
    public Pattern getMethodNamePattern() {
        return methodNamePattern != null ? methodNamePattern : Pattern.compile(requireNonNull(methodName));
    }

    @Deprecated
    public Pattern getArgumentPattern() {
        return argumentPattern;
    }

    private boolean matchesTargetTypeName(String fullyQualifiedTypeName) {
        return this.targetType != null && fullyQualifiedNamesAreEqual(this.targetType, fullyQualifiedTypeName) ||
               this.targetTypePattern != null && this.targetTypePattern.matcher(fullyQualifiedTypeName).matches();
    }

    boolean matchesTargetType(@Nullable JavaType.FullyQualified type) {
        return TypeUtils.isOfTypeWithName(
                type,
                matchOverrides,
                this::matchesTargetTypeName
        );
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean matchesMethodName(String methodName) {
        return this.methodName != null && this.methodName.equals(methodName) ||
               this.methodNamePattern != null && methodNamePattern.matcher(methodName).matches();
    }

    private boolean matchesParameterTypes(List<JavaType> parameterTypes) {
        if (argumentPattern == ANY_ARGUMENTS_PATTERN) {
            return true;
        } else if (argumentPattern == EMPTY_ARGUMENTS_PATTERN) {
            return parameterTypes.isEmpty();
        }

        StringJoiner joiner = new StringJoiner(",");
        for (JavaType javaType : parameterTypes) {
            String s = typePattern(javaType);
            if (s != null) {
                joiner.add(s);
            }
        }
        return argumentPattern.matcher(joiner.toString()).matches();
    }

    public boolean matches(@Nullable JavaType.Method type) {
        if (type == null) {
            return false;
        }
        if (!matchesTargetType(type.getDeclaringType())) {
            return false;
        }

        if (!matchesMethodName(type.getName())) {
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
        // [^.]* is the product of a fully wild card match for a method. `* foo()`
        boolean matchesTargetType = (targetTypePattern != null && "[^.]*".equals(targetTypePattern.pattern()))
                                    || matchesTargetType(enclosing.getType());
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
                        .collect(Collectors.toList());
        return matchesParameterTypes(parameterTypes);
    }

    @Nullable
    private static JavaType variableDeclarationsType(Statement v) {
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
    public boolean matches(@Nullable J.MethodInvocation method, boolean matchUnknownTypes) {
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

        if (method.getSelect() != null
            && method.getSelect() instanceof J.Identifier
            && !matchesSelectBySimpleNameAlone(((J.Identifier) method.getSelect()))) {
            return false;
        }

        final String argumentSignature = argumentsFromExpressionTypes(method);
        final Pattern relaxedArgumentPattern = Pattern.compile(
                argumentPattern.pattern().replaceAll("((?:[a-zA-Z0-9]+\\.?)+)",
                        "($1|" + JavaType.Unknown.getInstance().getFullyQualifiedName() + ")"));
        return relaxedArgumentPattern.matcher(argumentSignature).matches();
    }

    private boolean matchesSelectBySimpleNameAlone(J.Identifier select) {
        if (targetType != null) {
            return targetType.equals(select.getSimpleName()) || targetType.endsWith('.' + select.getSimpleName());
        }
        //noinspection DataFlowIssue
        return targetTypePattern.matcher(select.getSimpleName()).matches() ||
               Pattern.compile(targetTypePattern.pattern()
                               .replaceAll(".*" + Pattern.quote(ASPECTJ_DOT_PATTERN), "")
                               .replaceAll(".*" + Pattern.quote(ASPECTJ_DOTDOT_PATTERN), ""))
                       .matcher(select.getSimpleName()).matches();
    }

    private String argumentsFromExpressionTypes(J.MethodInvocation method) {
        StringJoiner joiner = new StringJoiner(",");
        for (Expression expr : method.getArguments()) {
            final JavaType exprType = expr.getType();
            String s = exprType == null
                    ? JavaType.Unknown.getInstance().getFullyQualifiedName()
                    : typePattern(exprType);
            joiner.add(s);
        }
        return joiner.toString();
    }

    /**
     * Evaluate whether this MethodMatcher and the specified FieldAccess are describing the same type or not.
     * Known limitation/bug: MethodMatchers can have patterns/wildcards like "com.*.Bar" instead of something
     * concrete like "com.foo.Bar". This limitation is not desirable or intentional and should be fixed.
     * If a methodMatcher is passed that includes wildcards the result will always be "false"
     *
     * @param fieldAccess A J.FieldAccess that hopefully has the same fully qualified type as this matcher.
     */
    @SuppressWarnings("DataFlowIssue")
    public boolean isFullyQualifiedClassReference(J.FieldAccess fieldAccess) {
        if (methodName != null && !methodName.equals(fieldAccess.getName().getSimpleName())) {
            return false;
        } else if (methodNamePattern != null && !methodNamePattern.matcher(fieldAccess.getName().getSimpleName()).matches()) {
            return false;
        }

        Expression target = fieldAccess.getTarget();
        if (target instanceof J.Identifier) {
            return targetType != null && targetType.equals(((J.Identifier) target).getSimpleName()) ||
                    targetTypePattern != null && targetTypePattern.matcher(((J.Identifier) target).getSimpleName()).matches();
        } else if (target instanceof J.FieldAccess) {
            return ((J.FieldAccess) target).isFullyQualifiedClassReference(targetType != null ? targetType : targetTypePattern.pattern());
        }
        return false;
    }

    @Nullable
    private static String typePattern(JavaType type) {
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
        //noinspection DataFlowIssue
        return (targetType != null ? targetType : targetTypePattern.pattern()) +
               ' ' +
               (methodName != null ? methodName : methodNamePattern.pattern()) +
               '(' + argumentPattern.pattern() + ')';
    }
}

class TypeVisitor extends MethodSignatureParserBaseVisitor<String> {
    private static final Set<String> COMMON_JAVA_LANG_TYPES =
            new HashSet<>(Arrays.asList(
                    "Appendable",
                    "AutoCloseable",
                    "Boolean",
                    "Byte",
                    "Character",
                    "CharSequence",
                    "Class",
                    "ClassLoader",
                    "Cloneable",
                    "Comparable",
                    "Double",
                    "Enum",
                    "Error",
                    "Exception",
                    "Float",
                    "FunctionalInterface",
                    "Integer",
                    "Iterable",
                    "Long",
                    "Math",
                    "Number",
                    "Object",
                    "Readable",
                    "Record",
                    "Runnable",
                    "Short",
                    "String",
                    "StringBuffer",
                    "StringBuilder",
                    "System",
                    "Thread",
                    "Throwable",
                    "Void"
            ));

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
                if (COMMON_JAVA_LANG_TYPES.contains(beforeArr)) {
                    return "java.lang." + className;
                }
            }
        }

        return className;
    }
}

/**
 * The wildcard {@code ..} indicates zero or more parameters, so:
 * <ul>
 * <li>{@code execution(void m(..))}
 * picks out execution join points for void methods named m, of any number of arguments, while
 * </li>
 * <li>
 * {@code execution(void m(.., int))}
 * picks out execution join points for void methods named m whose last parameter is of type int.
 * </li>
 * </ul>
 */
class FormalParameterVisitor extends MethodSignatureParserBaseVisitor<String> {
    private final List<Argument> arguments = new ArrayList<>();

    @Override
    public String visitTerminal(TerminalNode node) {
        if ("...".equals(node.getText())) {
            ((Argument.FormalType) arguments.get(arguments.size() - 1)).setVariableArgs(true);
        }
        return super.visitTerminal(node);
    }

    @Override
    public String visitDotDot(MethodSignatureParser.DotDotContext ctx) {
        arguments.add(Argument.DOT_DOT);
        return super.visitDotDot(ctx);
    }

    @Override
    public String visitFormalTypePattern(MethodSignatureParser.FormalTypePatternContext ctx) {
        arguments.add(new Argument.FormalType(ctx));
        return super.visitFormalTypePattern(ctx);
    }

    @Override
    public String visitFormalParametersPattern(MethodSignatureParser.FormalParametersPatternContext ctx) {
        super.visitFormalParametersPattern(ctx);

        List<String> argumentPatterns = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            Argument argument = arguments.get(i);

            // Note: the AspectJ grammar doesn't allow for multiple ..'s in one formal parameter pattern
            if (argument == Argument.DOT_DOT) {
                if (arguments.size() == 1) {
                    argumentPatterns.add("(" + argument.getRegex() + ")?");
                } else if (i > 0) {
                    argumentPatterns.add("(," + argument.getRegex() + ")?");
                } else {
                    argumentPatterns.add("(" + argument.getRegex() + ",)?");
                }
            } else { // FormalType
                if (i > 0 && arguments.get(i - 1) != Argument.DOT_DOT) {
                    argumentPatterns.add("," + argument.getRegex());
                } else {
                    argumentPatterns.add(argument.getRegex());
                }
            }
        }

        return String.join("", argumentPatterns).replace("...", "\\[\\]");
    }

    private abstract static class Argument {
        abstract String getRegex();

        private static final Argument DOT_DOT = new Argument() {
            @Override
            String getRegex() {
                return "([^,]+,)*([^,]+)";
            }
        };

        private static class FormalType extends Argument {
            private final MethodSignatureParser.FormalTypePatternContext ctx;

            @Setter
            private boolean variableArgs = false;

            public FormalType(MethodSignatureParser.FormalTypePatternContext ctx) {
                this.ctx = ctx;
            }

            @Override
            String getRegex() {
                String baseType = new TypeVisitor().visitFormalTypePattern(ctx);
                return StringUtils.aspectjNameToPattern(baseType) + (variableArgs ? "\\[\\]" : "");
            }
        }
    }
}
