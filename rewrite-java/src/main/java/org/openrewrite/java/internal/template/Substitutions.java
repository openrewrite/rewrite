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
package org.openrewrite.java.internal.template;

import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.internal.grammar.TemplateParameterLexer;
import org.openrewrite.java.internal.grammar.TemplateParameterParser;
import org.openrewrite.java.internal.grammar.TemplateParameterParser.TypeContext;
import org.openrewrite.java.tree.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class Substitutions {
    private static final Pattern PATTERN_COMMENT = Pattern.compile("__p(\\d+)__");

    private final String code;
    private final Object[] parameters;
    private final PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper(
            "#{", "}", null);

    public String substitute() {
        AtomicInteger requiredParameters = new AtomicInteger(0);
        AtomicInteger index = new AtomicInteger(0);
        String substituted = code;
        while (true) {
            Map<String, String> typedPatternByName = new HashMap<>();
            String previous = substituted;
            substituted = propertyPlaceholderHelper.replacePlaceholders(substituted, key -> {
                String s;
                if (!key.isEmpty()) {
                    TemplateParameterParser parser = new TemplateParameterParser(new CommonTokenStream(new TemplateParameterLexer(
                            CharStreams.fromString(key))));

                    parser.removeErrorListeners();
                    parser.addErrorListener(new ThrowingErrorListener());

                    TemplateParameterParser.MatcherPatternContext ctx = parser.matcherPattern();
                    TemplateParameterParser.TypedPatternContext typedPattern = ctx.typedPattern();
                    if (typedPattern == null) {
                        String paramName = ctx.parameterName().Identifier().getText();
                        s = typedPatternByName.get(paramName);
                        if (s == null) {
                            throw new IllegalArgumentException("The parameter " + paramName + " must be defined before it is referenced.");
                        }
                    } else {
                        int i = index.getAndIncrement();
                        s = substituteTypedPattern(key, i, typedPattern);
                        if (ctx.typedPattern().parameterName() != null) {
                            String paramName = ctx.typedPattern().parameterName().Identifier().getText();
                            typedPatternByName.put(paramName, s);
                        }
                        requiredParameters.incrementAndGet();
                    }
                } else {
                    int i = index.getAndIncrement();
                    s = substituteUntyped(parameters[i], i);
                    requiredParameters.incrementAndGet();
                }

                return s;
            });

            if (previous.equals(substituted)) {
                break;
            }
        }

        if (parameters.length != requiredParameters.get()) {
            throw new IllegalArgumentException("This template requires " + requiredParameters.get() + " parameters.");
        }

        return substituted;
    }

    private String substituteTypedPattern(String key, int index, TemplateParameterParser.TypedPatternContext typedPattern) {
        Object parameter = parameters[index];
        String s;
        String matcherName = typedPattern.patternType().matcherName().Identifier().getText();
        TypeContext param = typedPattern.patternType().type();

        if ("anyArray".equals(matcherName)) {
            if (!(parameter instanceof TypedTree)) {
                throw new IllegalArgumentException("anyArray can only be used on TypedTree parameters");
            }

            JavaType type = ((TypedTree) parameter).getType();
            if (type == null && parameter instanceof J.Empty && ((J.Empty) parameter).getMarkers().findFirst(TemplateParameter.class).isPresent()) {
                // this is a hack, but since we currently represent template parameters as `J.Empty`, this is the only way to get the type now
                type = ((J.Empty) parameter).getMarkers().findFirst(TemplateParameter.class).get().getType();
            }
            JavaType.Array arrayType = TypeUtils.asArray(type);
            if (arrayType == null) {
                arrayType = TypeUtils.asArray(type);
                if (arrayType == null) {
                    throw new IllegalArgumentException("anyArray can only be used on parameters containing JavaType.Array type attribution");
                }
            }

            int dimensions = 1;
            for (; arrayType.getElemType() instanceof JavaType.Array; arrayType = (JavaType.Array) arrayType.getElemType()) {
                dimensions++;
            }

            s = "(" + newArrayParameter(arrayType.getElemType(), dimensions, index) + ")";
        } else if ("any".equals(matcherName)) {
            JavaType type;
            if (param != null) {
                type = TypeParameter.toFullyQualifiedName(param);
            } else {
                if (parameter instanceof J.NewClass && ((J.NewClass) parameter).getBody() != null
                    && ((J.NewClass) parameter).getClazz() != null) {
                    // for anonymous classes get the type from the supertype
                    type = ((J.NewClass) parameter).getClazz().getType();
                } else if (parameter instanceof TypedTree) {
                    type = ((TypedTree) parameter).getType();
                } else {
                    type = null;
                }
            }

            String fqn = getTypeName(type);
            JavaType.Primitive primitive = JavaType.Primitive.fromKeyword(fqn);
            s = primitive == null || primitive.equals(JavaType.Primitive.String) ?
                    newObjectParameter(fqn, index) :
                    newPrimitiveParameter(fqn, index);

            parameters[index] = ((J) parameter).withPrefix(Space.EMPTY);
        } else {
            throw new IllegalArgumentException("Invalid template matcher '" + key + "'");
        }
        return s;
    }

    protected String newObjectParameter(String fqn, int index) {
        return "__P__." + "<" + fqn + ">/*__p" + index + "__*/p()";
    }

    protected String newPrimitiveParameter(String fqn, int index) {
        return "__P__./*__p" + index + "__*/" + fqn + "p()";
    }

    protected String newArrayParameter(JavaType elemType, int dimensions, int index) {
        StringBuilder builder = new StringBuilder("/*__p" + index + "__*/" + "new ");
        if (elemType instanceof JavaType.Primitive) {
            builder.append(((JavaType.Primitive) elemType).getKeyword());
        } else if (elemType instanceof JavaType.FullyQualified) {
            builder.append(((JavaType.FullyQualified) elemType).getFullyQualifiedName().replace("$", "."));
        }
        for (int i = 0; i < dimensions; i++) {
            builder.append("[0]");
        }
        return builder.toString();
    }

    private String getTypeName(@Nullable JavaType type) {
        if (type == null) {
            return "java.lang.Object";
        } else if (type instanceof JavaType.GenericTypeVariable) {
            JavaType.GenericTypeVariable genericTypeVariable = (JavaType.GenericTypeVariable) type;
            if (genericTypeVariable.getName().equals("?")) {
                // wildcards cannot be used as type parameters on method invocations
                return "java.lang.Object";
            }
            return TypeUtils.toString(type);
        }
        return TypeUtils.toString(type).replace("$", ".");
    }

    private String substituteUntyped(Object parameter, int index) {
        if (parameter instanceof J) {
            if (parameter instanceof J.Annotation) {
                return "@SubAnnotation(" + index + ")";
            } else if (parameter instanceof J.Block) {
                return "/*__p" + index + "__*/{}";
            } else if (parameter instanceof J.Literal || parameter instanceof J.VariableDeclarations) {
                //noinspection deprecation
                return ((J) parameter).printTrimmed();
            } else {
                throw new IllegalArgumentException("Template parameter " + index + " cannot be used in an untyped template substitution. " +
                                                   "Instead of \"#{}\", indicate the template parameter's type with \"#{any(" + typeHintFor(parameter) + ")}\".");
            }
        } else if (parameter instanceof JRightPadded) {
            return substituteUntyped(((JRightPadded<?>) parameter).getElement(), index);
        } else if (parameter instanceof JLeftPadded) {
            return substituteUntyped(((JLeftPadded<?>) parameter).getElement(), index);
        }
        return parameter.toString();
    }

    private static String typeHintFor(Object j) {
        if (j instanceof TypedTree) {
            return typeHintFor(((TypedTree) j).getType());
        }
        return "";
    }

    private static String typeHintFor(@Nullable JavaType t) {
        if (t instanceof JavaType.Primitive) {
            return ((JavaType.Primitive) t).getKeyword();
        } else if (t instanceof JavaType.FullyQualified) {
            return ((JavaType.FullyQualified) t).getFullyQualifiedName();
        }
        return "";
    }

    @SuppressWarnings("SpellCheckingInspection")
    public <J2 extends J> List<J2> unsubstitute(List<J2> js) {
        return ListUtils.map(js, this::unsubstitute);
    }

    @SuppressWarnings("SpellCheckingInspection")
    public <J2 extends J> J2 unsubstitute(J2 j) {
        if (parameters.length == 0) {
            return j;
        }

        //noinspection unchecked
        J2 unsub = (J2) new JavaVisitor<Integer>() {
            @SuppressWarnings("ConstantConditions")
            @Override
            public J visitAnnotation(J.Annotation annotation, Integer integer) {
                if (TypeUtils.isOfClassType(annotation.getType(), "SubAnnotation")) {
                    J.Literal index = (J.Literal) annotation.getArguments().get(0);
                    J a2 = (J) parameters[(Integer) index.getValue()];
                    return a2.withPrefix(a2.getPrefix().withWhitespace(annotation.getPrefix().getWhitespace()));
                }
                return super.visitAnnotation(annotation, integer);
            }

            @Override
            public J visitBlock(J.Block block, Integer integer) {
                J param = maybeParameter(block);
                if (param != null) {
                    return param;
                }
                return super.visitBlock(block, integer);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                J param = maybeParameter(method.getName());
                if (param != null) {
                    return param;
                }
                return super.visitMethodInvocation(method, integer);
            }

            @Override
            public <T extends J> J visitParentheses(J.Parentheses<T> parens, Integer integer) {
                J param = maybeParameter(parens.getTree());
                if (param != null) {
                    return param;
                }
                return super.visitParentheses(parens, integer);
            }

            @Override
            public J visitLiteral(J.Literal literal, Integer integer) {
                J param = maybeParameter(literal);
                if (param != null) {
                    return param;
                }
                return super.visitLiteral(literal, integer);
            }

            @Nullable
            private J maybeParameter(J j) {
                Integer param = parameterIndex(j.getPrefix());
                if (param != null) {
                    J j2 = (J) parameters[param];
                    return j2.withPrefix(j2.getPrefix().withWhitespace(j.getPrefix().getWhitespace()));
                }
                return null;
            }

            @Nullable
            private Integer parameterIndex(Space space) {
                for (Comment comment : space.getComments()) {
                    if (comment instanceof TextComment) {
                        Matcher matcher = PATTERN_COMMENT.matcher(((TextComment) comment).getText());
                        if (matcher.matches()) {
                            return Integer.valueOf(matcher.group(1));
                        }
                    }
                }
                return null;
            }
        }.visit(j, 0);

        assert unsub != null;
        return unsub;
    }

    private static class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new IllegalArgumentException(
                    String.format("Syntax error at line %d:%d %s.", line, charPositionInLine, msg), e);
        }
    }
}
