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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.java.JavaTypeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.internal.grammar.TemplateParameterParser;
import org.openrewrite.java.internal.grammar.TemplateParameterParser.TypeContext;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.newSetFromMap;
import static org.openrewrite.java.ParenthesizeVisitor.maybeParenthesize;

@RequiredArgsConstructor
@ToString
public class Substitutions {
    private static final Pattern PATTERN_COMMENT = Pattern.compile("__p(\\d+)__");
    private static final List<String> VALID_MATCHERS = Arrays.asList("any", "anyArray");

    private final String code;
    private final Set<String> genericTypes;
    private final Object[] parameters;
    private final PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper(
            "#{", "}", null);
    @Getter
    private final Set<JavaType.GenericTypeVariable> typeVariables = newSetFromMap(new IdentityHashMap<>());

    public String substitute() {
        Map<String, JavaType.GenericTypeVariable> generics = TypeParameter.parseGenericTypes(genericTypes);
        typeVariables.addAll(generics.values());
        AtomicInteger requiredParameters = new AtomicInteger(0);
        AtomicInteger index = new AtomicInteger(0);
        String substituted = code;
        while (true) {
            Map<String, String> typedPatternByName = new HashMap<>();
            String previous = substituted;
            substituted = propertyPlaceholderHelper.replacePlaceholders(substituted, key -> {
                String s;
                if (!key.isEmpty()) {
                    TemplateParameterParser.MatcherPatternContext ctx = TypeParameter.parser(key).matcherPattern();
                    TemplateParameterParser.TypedPatternContext typedPattern = ctx.typedPattern();
                    if (typedPattern == null) {
                        String paramName = ctx.parameterName().Identifier().getText();
                        s = typedPatternByName.get(paramName);
                        if (s == null) {
                            throw new IllegalArgumentException("The parameter " + paramName + " must be defined before it is referenced.");
                        }
                    } else {
                        int i = index.getAndIncrement();
                        s = substituteTypedPattern(key, i, typedPattern, generics);
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

    private String substituteTypedPattern(String key, int index, TemplateParameterParser.TypedPatternContext typedPattern, Map<String, JavaType.GenericTypeVariable> generics) {
        if (index >= parameters.length) {
            throw new IllegalArgumentException("This template requires more parameters.");
        }
        Object parameter = parameters[index];
        String s;
        TypeContext param = typedPattern.patternType().type();
        String matcherName = typedPattern.patternType().matcherName().Identifier().getText();
        if (!VALID_MATCHERS.contains(matcherName)) {
            throw new IllegalArgumentException("Invalid template matcher '" + key + "'");
        }

        JavaType type;
        if (param != null) {
            type = TypeParameter.toJavaType(param, generics);
            if ("anyArray".equals(matcherName)) {
                type = new JavaType.Array(null, type, null);
            }
        } else {
            if (parameter instanceof J.NewClass && ((J.NewClass) parameter).getBody() != null &&
                    ((J.NewClass) parameter).getClazz() != null) {
                // for anonymous classes get the type from the supertype
                type = ((J.NewClass) parameter).getClazz().getType();
            } else if (parameter instanceof J.Empty && ((J.Empty) parameter).getMarkers().findFirst(TemplateParameter.class).isPresent()) {
                type = ((J.Empty) parameter).getMarkers().findFirst(TemplateParameter.class).get().getType();
            } else if (parameter instanceof TypedTree) {
                type = ((TypedTree) parameter).getType();
            } else if ("anyArray".equals(matcherName)) {
                type = new JavaType.Array(null, JavaType.ShallowClass.build("java.lang.Object"), null);
            } else {
                type = null;
            }
            extractTypeVariables(type);
        }

        String fqn = getTypeName(type);
        JavaType.Primitive primitive = JavaType.Primitive.fromKeyword(fqn);
        s = primitive == null || primitive == JavaType.Primitive.String ?
                newObjectParameter(fqn, index) :
                newPrimitiveParameter(fqn, index);

        parameters[index] = ((J) parameter).withPrefix(Space.EMPTY);
        return s;
    }

    protected String newObjectParameter(String fqn, int index) {
        return "__P__." + "<" + fqn + ">/*__p" + index + "__*/p()";
    }

    protected String newPrimitiveParameter(String fqn, int index) {
        return "__P__./*__p" + index + "__*/" + fqn + "p()";
    }

    private String getTypeName(@Nullable JavaType type) {
        if (type == null) {
            return "java.lang.Object";
        } else if (type instanceof JavaType.GenericTypeVariable) {
            JavaType.GenericTypeVariable genericType = (JavaType.GenericTypeVariable) type;
            if (!"?".equals(genericType.getName())) {
                return genericType.getName();
            } else if (genericType.getVariance() != JavaType.GenericTypeVariable.Variance.COVARIANT || genericType.getBounds().size() != 1) {
                // wildcards cannot be used as type parameters on method invocations as in `foo.<?> bar()`
                return "java.lang.Object";
            } else {
                return TypeUtils.toString(genericType.getBounds().get(0));
            }
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
        if (t == null || t instanceof JavaType.GenericTypeVariable) {
            return "";
        }
        return TypeUtils.toString(t);
    }

    private void extractTypeVariables(@Nullable JavaType type) {
        if (type == null) {
            return;
        }
        Set<JavaType> visited = newSetFromMap(new IdentityHashMap<>());
        new JavaTypeVisitor<Integer>() {
            @Override
            public JavaType visitAnnotation(JavaType.Annotation annotation, Integer p) {
                return annotation.getType();
            }

            @Override
            public JavaType visitArray(JavaType.Array array, Integer p) {
                visit(array.getElemType(), p);
                return array;
            }

            @Override
            public JavaType visitClass(JavaType.Class aClass, Integer p) {
                return aClass;
            }

            @Override
            public JavaType visitGenericTypeVariable(JavaType.GenericTypeVariable generic, Integer p) {
                if (!visited.add(generic)) {
                    return generic;
                }
                if (!"?".equals(generic.getName())) {
                    typeVariables.add(generic);
                }
                return super.visitGenericTypeVariable(generic, p);
            }

            @Override
            public JavaType visitMethod(JavaType.Method method, Integer p) {
                return method;
            }

            @Override
            public JavaType visitParameterized(JavaType.Parameterized parameterized, Integer p) {
                for (JavaType typeParameter : parameterized.getTypeParameters()) {
                    visit(typeParameter, p);
                }
                return super.visitParameterized(parameterized, p);
            }

            @Override
            public JavaType visitVariable(JavaType.Variable variable, Integer p) {
                return variable;
            }
        }.visit(type, 0);
    }

    @SuppressWarnings("SpellCheckingInspection")
    public <J2 extends J> List<J2> unsubstitute(List<J2> js) {
        return ListUtils.map(js, this::unsubstitute);
    }

    @SuppressWarnings("SpellCheckingInspection")
    public <J2 extends J> @Nullable J2 unsubstitute(J2 j) {
        if (parameters.length == 0) {
            return j;
        }

        //noinspection unchecked
        return (J2) new JavaVisitor<Integer>() {
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
                if (param instanceof Expression) {
                    return maybeParenthesize((Expression) param, getCursor());
                } else if (param != null) {
                    return param;
                }
                return super.visitMethodInvocation(method, integer);
            }

            @Override
            public <T extends J> J visitParentheses(J.Parentheses<T> parens, Integer integer) {
                J param = maybeParameter(parens.getTree());
                if (param instanceof Expression) {
                    return maybeParenthesize((Expression) param, getCursor());
                } else if (param != null) {
                    return param;
                }
                return super.visitParentheses(parens, integer);
            }

            @Override
            public J visitLiteral(J.Literal literal, Integer integer) {
                J param = maybeParameter(literal);
                if (param instanceof Expression) {
                    return maybeParenthesize((Expression) param, getCursor());
                } else if (param != null) {
                    return param;
                }
                return super.visitLiteral(literal, integer);
            }

            private @Nullable J maybeParameter(J j1) {
                Integer param = parameterIndex(j1.getPrefix());
                if (param != null) {
                    J j2 = (J) parameters[param];
                    return j2.withPrefix(j2.getPrefix().withWhitespace(j1.getPrefix().getWhitespace()));
                }
                return null;
            }

            private @Nullable Integer parameterIndex(Space space) {
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
        }.visitNonNull(j, 0);
    }
}
