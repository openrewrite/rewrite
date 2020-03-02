/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.NonFinal;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.grammar.AspectJLexer;
import org.openrewrite.java.internal.grammar.RefactorMethodSignatureParser;
import org.openrewrite.java.internal.grammar.RefactorMethodSignatureParserBaseVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;

@Getter
public class MethodMatcher {
    private Pattern targetTypePattern;
    private Pattern methodNamePattern;
    private Pattern argumentPattern;

    public MethodMatcher(String signature) {
        var parser = new RefactorMethodSignatureParser(new CommonTokenStream(new AspectJLexer(
                CharStreams.fromString(signature))));

        new RefactorMethodSignatureParserBaseVisitor<Void>() {
            @Override
            public Void visitMethodPattern(RefactorMethodSignatureParser.MethodPatternContext ctx) {
                targetTypePattern = Pattern.compile(new TypeVisitor().visitTargetTypePattern(ctx.targetTypePattern()));
                methodNamePattern = Pattern.compile(ctx.simpleNamePattern().children.stream()
                        .map(c -> AspectjUtils.aspectjNameToPattern(c.toString()))
                        .collect(joining("")));
                argumentPattern = Pattern.compile(new FormalParameterVisitor().visitFormalParametersPattern(
                        ctx.formalParametersPattern()));
                return null;
            }
        }.visit(parser.methodPattern());
    }

    public boolean matches(J.MethodInvocation method) {
        if (method.getType() == null || method.getType().getDeclaringType() == null) {
            return false;
        }

        if (method.getType().getResolvedSignature() == null) {
            // no way to verify the parameter list
            return false;
        }

        String resolvedSignaturePattern = method.getType().getResolvedSignature().getParamTypes().stream()
                .map(this::typePattern)
                .filter(Objects::nonNull)
                .collect(joining(","));

        return matchesTargetType(method.getType().getDeclaringType()) &&
                methodNamePattern.matcher(method.getSimpleName()).matches() &&
                argumentPattern.matcher(resolvedSignaturePattern).matches();
    }

    boolean matchesTargetType(@Nullable JavaType.Class type) {
        return type != null && (targetTypePattern.matcher(type.getFullyQualifiedName()).matches() ||
                type != JavaType.Class.OBJECT && matchesTargetType(type.getSupertype() == null ? JavaType.Class.OBJECT : type.getSupertype()));
    }

    @Nullable
    private String typePattern(JavaType type) {
        if (type instanceof JavaType.Primitive) {
            return ((JavaType.Primitive) type).getKeyword();
        } else if (type instanceof JavaType.Class) {
            return ((JavaType.Class) type).getFullyQualifiedName();
        } else if (type instanceof JavaType.Array) {
            return typePattern(((JavaType.Array) type).getElemType()) + "[]";
        }
        return null;
    }
}

class TypeVisitor extends RefactorMethodSignatureParserBaseVisitor<String> {
    @Override
    public String visitClassNameOrInterface(RefactorMethodSignatureParser.ClassNameOrInterfaceContext ctx) {
        String className = ctx.children.stream()
                .map(c -> AspectjUtils.aspectjNameToPattern(c.getText()))
                .collect(joining(""));

        if (!className.contains(".")) {
            try {
                int arrInit = className.lastIndexOf("\\[");
                Class.forName("java.lang." + (arrInit == -1 ? className : className.substring(0, arrInit)), false, TypeVisitor.class.getClassLoader());
                return "java.lang." + className;
            } catch (ClassNotFoundException ignored) {
            }
        }

        return className;
    }

    @Override
    public String visitPrimitiveType(RefactorMethodSignatureParser.PrimitiveTypeContext ctx) {
        return ctx.getText();
    }
}

/**
 * The wildcard .. indicates zero or more parameters, so:
 *
 * <code>execution(void m(..))</code>
 * picks out execution join points for void methods named m, of any number of arguments, while
 *
 * <code>execution(void m(.., int))</code>
 * picks out execution join points for void methods named m whose last parameter is of type int.
 */
class FormalParameterVisitor extends RefactorMethodSignatureParserBaseVisitor<String> {
    private final List<Argument> arguments = new ArrayList<>();

    @Override
    public String visitTerminal(TerminalNode node) {
        if ("...".equals(node.getText())) {
            ((Argument.FormalType) arguments.get(arguments.size() - 1)).setVariableArgs(true);
        }
        return super.visitTerminal(node);
    }

    @Override
    public String visitDotDot(RefactorMethodSignatureParser.DotDotContext ctx) {
        arguments.add(Argument.DOT_DOT);
        return super.visitDotDot(ctx);
    }

    @Override
    public String visitFormalTypePattern(RefactorMethodSignatureParser.FormalTypePatternContext ctx) {
        arguments.add(new Argument.FormalType(ctx));
        return super.visitFormalTypePattern(ctx);
    }

    @Override
    public String visitFormalParametersPattern(RefactorMethodSignatureParser.FormalParametersPatternContext ctx) {
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

    private static abstract class Argument {
        abstract String getRegex();

        private static final Argument DOT_DOT = new Argument() {
            @Override
            String getRegex() {
                return "([^,]+,)*([^,]+)";
            }
        };

        static class FormalType extends Argument {
            RefactorMethodSignatureParser.FormalTypePatternContext ctx;

            @Setter
            @NonFinal
            boolean variableArgs = false;

            public FormalType(RefactorMethodSignatureParser.FormalTypePatternContext ctx) {
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

class AspectjUtils {
    private AspectjUtils() {
    }

    /**
     * See https://eclipse.org/aspectj/doc/next/progguide/semantics-pointcuts.html#type-patterns
     * <p>
     * An embedded * in an identifier matches any sequence of characters, but
     * does not match the package (or inner-type) separator ".".
     * <p>
     * The ".." wildcard matches any sequence of characters that start and end with a ".", so it can be used to pick out all
     * types in any subpackage, or all inner types. e.g. <code>within(com.xerox..*)</code> picks out all join points where
     * the code is in any declaration of a type whose name begins with "com.xerox.".
     */
    public static String aspectjNameToPattern(String name) {
        return name
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replaceAll("([^.])*\\.([^.])*", "$1\\.$2")
                .replace("*", "[^.]*")
                .replace("..", "\\.(.+\\.)?");
    }
}