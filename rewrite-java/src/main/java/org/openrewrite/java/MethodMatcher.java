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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;
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
 * * *(..)                                 - All method invocations
 * java.util.* *(..)                       - All method invocations to classes belonging to java.util (including sub-packages)
 * java.util.Collections *(..)             - All method invocations on java.util.Collections class
 * java.util.Collections unmodifiable*(..) - All method invocations starting with "unmodifiable" on java.util.Collections
 * java.util.Collections min(..)           - All method invocations for all overloads of "min"
 * java.util.Collections emptyList()       - All method invocations on java.util.Collections.emptyList()
 * my.org.MyClass *(boolean, ..)           - All method invocations where the first arg is a boolean in my.org.MyClass
 * </PRE>
 */
@SuppressWarnings("NotNullFieldNotInitialized")
public class MethodMatcher implements InvocationMatcher {
    private static final JavaType.ShallowClass OBJECT_CLASS = JavaType.ShallowClass.build("java.lang.Object");
    private static final String ASPECTJ_DOT_PATTERN = StringUtils.aspectjNameToPattern(".");
    private static final String ASPECTJ_DOTDOT_PATTERN = StringUtils.aspectjNameToPattern("..");

    @Getter
    private Pattern targetTypePattern;
    @Getter
    private Pattern methodNamePattern;
    @Getter
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
                targetTypePattern = Pattern.compile(new TypeVisitor().visitTargetTypePattern(targetTypePatternContext));
                targetType = isPlainIdentifier(targetTypePatternContext)
                        ? pattern.replace(ASPECTJ_DOT_PATTERN, ".").replace("\\", "")
                        : null;
                pattern = ctx.simpleNamePattern().children.stream()
                        .map(c -> StringUtils.aspectjNameToPattern(c.toString()))
                        .collect(joining(""));
                methodNamePattern = Pattern.compile(pattern);
                methodName = isPlainIdentifier(ctx.simpleNamePattern()) ? pattern : null;
                argumentPattern = Pattern.compile(new FormalParameterVisitor().visitFormalParametersPattern(
                        ctx.formalParametersPattern()));
                return null;
            }
        }.visit(parser.methodPattern());
    }

    private boolean isPlainIdentifier(MethodSignatureParser.TargetTypePatternContext context) {
        return context.BANG() == null
                && context.AND() == null
                && context.OR() == null
                && context.classNameOrInterface().DOTDOT().isEmpty()
                && context.classNameOrInterface().WILDCARD().isEmpty();
    }

    private boolean isPlainIdentifier(MethodSignatureParser.SimpleNamePatternContext context) {
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

    public boolean matches(@Nullable JavaType.Method type) {
        if (type == null || !matchesTargetType(type.getDeclaringType())) {
            return false;
        }

        if (methodName != null && !methodName.equals(type.getName())) {
            return false;
        } else if (methodName == null && !methodNamePattern.matcher(type.getName()).matches()) {
            return false;
        }

        StringJoiner joiner = new StringJoiner(",");
        for (JavaType javaType : type.getParameterTypes()) {
            String s = typePattern(javaType);
            if (s != null) {
                joiner.add(s);
            }
        }

        return argumentPattern.matcher(joiner.toString()).matches();
    }

    public boolean matches(J.MethodDeclaration method, J.ClassDeclaration enclosing) {
        if (enclosing.getType() == null) {
            return false;
        }

        // aspectJUtils does not support matching classes separated by packages.
        // [^.]* is the product of a fully wild card match for a method. `* foo()`
        boolean matchesTargetType = (targetType == null && "[^.]*".equals(targetTypePattern.pattern()))
                || matchesTargetType(enclosing.getType());
        if (!matchesTargetType) {
            return false;
        }

        if (methodName != null && !(methodName.equals(method.getSimpleName())
                || method.getMethodType() != null && methodName.equals(method.getMethodType().getName()))) {
            return false;
        } else if (methodName == null && !(methodNamePattern.matcher(method.getSimpleName()).matches()
                || method.getMethodType() != null && methodNamePattern.matcher(method.getMethodType().getName()).matches())) {
            return false;
        }

        String arguments = method.getParameters().stream()
                .map(this::variableDeclarationsType)
                .filter(Objects::nonNull)
                .map(MethodMatcher::typePattern)
                .filter(Objects::nonNull)
                .collect(joining(","));
        return argumentPattern.matcher(arguments).matches();
    }

    @Nullable
    private JavaType variableDeclarationsType(Statement v) {
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

    @Override
    public boolean matches(@Nullable Expression maybeMethod) {
        return (maybeMethod instanceof J.MethodInvocation && matches((J.MethodInvocation) maybeMethod)) ||
                (maybeMethod instanceof J.NewClass && matches((J.NewClass) maybeMethod)) ||
                (maybeMethod instanceof J.MemberReference && matches((J.MemberReference) maybeMethod));
    }

    public boolean matches(@Nullable J.MethodInvocation method) {
        return matches(method, false);
    }

    /**
     * Prefer {@link #matches(J.MethodInvocation)}, which uses the default `false` behavior for matchUnknownTypes.
     * Using matchUnknownTypes can improve Visitor resiliency for an AST with missing type information, but
     * also increases the risk of false-positive matches on unrelated MethodInvocation instances.
     */
    public boolean matches(@Nullable J.MethodInvocation method, boolean matchUnknownTypes) {
        if(method == null) {
            return false;
        }

        if (method.getMethodType() == null) {
            return matchUnknownTypes && matchesAllowingUnknownTypes(method);
        }

        if (!matchesTargetType(method.getMethodType().getDeclaringType())) {
            return false;
        }

        if (methodName != null && !methodName.equals(method.getSimpleName())
                || methodName == null && !methodNamePattern.matcher(method.getSimpleName()).matches()) {
            return false;
        }

        StringJoiner joiner = new StringJoiner(",");
        for (JavaType javaType : method.getMethodType().getParameterTypes()) {
            String s = typePattern(javaType);
            if (s != null) {
                joiner.add(s);
            }
        }

        return argumentPattern.matcher(joiner.toString()).matches();
    }

    private boolean matchesAllowingUnknownTypes(J.MethodInvocation method) {
        if (methodName != null && !methodName.equals(method.getSimpleName())
                || methodName == null && !methodNamePattern.matcher(method.getSimpleName()).matches()) {
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
        return targetTypePattern.matcher(select.getSimpleName()).matches()
            || Pattern.compile(targetTypePattern.pattern()
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

    public boolean matches(J.NewClass constructor) {
        JavaType.FullyQualified type = TypeUtils.asFullyQualified(constructor.getType());
        if (type == null || constructor.getConstructorType() == null) {
            return false;
        }

        if (!matchesTargetType(type) || !("<constructor>".equals(methodName)
                || methodName == null && methodNamePattern.matcher("<constructor>").matches())) {
            return false;
        }

        StringJoiner joiner = new StringJoiner(",");
        for (JavaType javaType : constructor.getConstructorType().getParameterTypes()) {
            String s = typePattern(javaType);
            if (s != null) {
                joiner.add(s);
            }
        }

        return argumentPattern.matcher(joiner.toString()).matches();
    }

    public boolean matches(J.MemberReference memberReference) {
        return matches(memberReference.getMethodType());
    }

    boolean matchesTargetType(@Nullable JavaType.FullyQualified type) {
        if (type == null || type instanceof JavaType.Unknown) {
            return false;
        }

        if (targetType != null && fullyQualifiedNamesAreEqual(targetType, type.getFullyQualifiedName())) {
            return true;
        } else if (targetType == null && targetTypePattern.matcher(type.getFullyQualifiedName()).matches()) {
            return true;
        }

        if (matchOverrides) {
            if (!"java.lang.Object".equals(type.getFullyQualifiedName()) && matchesTargetType(OBJECT_CLASS)) {
                return true;
            }

            if (matchesTargetType(type.getSupertype())) {
                return true;
            }

            for (JavaType.FullyQualified anInterface : type.getInterfaces()) {
                if (matchesTargetType(anInterface)) {
                    return true;
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
        String hopefullyFullyQualifiedMethod;
        if (targetType != null) {
            hopefullyFullyQualifiedMethod = targetType + "." + methodNamePattern.pattern();
        } else {
            hopefullyFullyQualifiedMethod = targetTypePattern.pattern()
                    .replace(ASPECTJ_DOT_PATTERN, ".")
                    + "." + methodNamePattern.pattern();
        }
        return fieldAccess.isFullyQualifiedClassReference(hopefullyFullyQualifiedMethod);
    }

    @Nullable
    private static String typePattern(JavaType type) {
        if (type instanceof JavaType.Primitive) {
            if (type.equals(JavaType.Primitive.String)) {
                return ((JavaType.Primitive) type).getClassName();
            }
            return ((JavaType.Primitive) type).getKeyword();
        } else if(type instanceof JavaType.Unknown) {
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
}

class TypeVisitor extends MethodSignatureParserBaseVisitor<String> {
    @Override
    public String visitClassNameOrInterface(MethodSignatureParser.ClassNameOrInterfaceContext ctx) {
        StringBuilder classNameBuilder = new StringBuilder();
        for (ParseTree c : ctx.children) {
            classNameBuilder.append(StringUtils.aspectjNameToPattern(c.getText()));
        }
        String className = classNameBuilder.toString();

        if (!className.contains(".")) {
            try {
                int arrInit = className.lastIndexOf("\\[");
                String beforeArr = arrInit == -1 ? className : className.substring(0, arrInit);
                if (JavaType.Primitive.fromKeyword(beforeArr) != null) {
                    if ("String".equals(beforeArr)) {
                        return "java.lang." + className;
                    }
                    return className;
                }
                Class.forName("java.lang." + beforeArr, false, TypeVisitor.class.getClassLoader());
                return "java.lang." + className;
            } catch (ClassNotFoundException ignored) {
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
                return baseType + (variableArgs ? "\\[\\]" : "");
            }
        }
    }
}
