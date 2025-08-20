/*
 * Copyright 2024 the original author or authors.
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

import org.antlr.v4.runtime.*;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.internal.grammar.TemplateParameterLexer;
import org.openrewrite.java.internal.grammar.TemplateParameterParser;
import org.openrewrite.java.internal.grammar.TemplateParameterParserBaseVisitor;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.*;

public class TypeParameter {

    private static final JavaType.Class TYPE_OBJECT = JavaType.ShallowClass.build("java.lang.Object");

    public static TemplateParameterParser parser(String value) {
        TemplateParameterLexer lexer = new TemplateParameterLexer(CharStreams.fromString(value));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowingErrorListener());

        TemplateParameterParser parser = new TemplateParameterParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(new ThrowingErrorListener());
        return parser;
    }

    public static JavaType toJavaType(TemplateParameterParser.@Nullable TypeContext type, Map<String, JavaType.GenericTypeVariable> genericTypes) {
        if (type == null) {
            return TYPE_OBJECT;
        }

        // FIXME handle `$` separator
        return type.accept(new TemplateParameterParserBaseVisitor<JavaType>() {
            @Override
            public JavaType visitType(TemplateParameterParser.TypeContext ctx) {
                JavaType type1 = ctx.typeName().accept(this);
                if (!ctx.typeParameter().isEmpty()) {
                    List<JavaType> typeParameters = new ArrayList<>();
                    for (TemplateParameterParser.TypeParameterContext param : ctx.typeParameter()) {
                        typeParameters.add(param.accept(this));
                    }
                    type1 = new JavaType.Parameterized(null, (JavaType.FullyQualified) type1, typeParameters);
                }
                for (TemplateParameterParser.TypeArrayContext unused : ctx.typeArray()) {
                    type1 = new JavaType.Array(null, type1, null);
                }
                return type1;
            }

            @Override
            public JavaType visitTypeName(TemplateParameterParser.TypeNameContext ctx) {
                String fqn = ctx.FullyQualifiedName() != null ? ctx.FullyQualifiedName().getText() : ctx.Identifier().getText();
                JavaType type;
                if (fqn.contains(".")) {
                    type = JavaType.ShallowClass.build(fqn);
                } else if (genericTypes.containsKey(fqn)) {
                    type = genericTypes.get(fqn);
                } else if ("String".equals(fqn)) {
                    type = JavaType.ShallowClass.build("java.lang.String");
                } else if ("Object".equals(fqn)) {
                    type = TYPE_OBJECT;
                } else if ((type = JavaType.Primitive.fromKeyword(fqn)) != null) {
                    // empty
                } else {
                    throw new IllegalArgumentException("Unknown type " + fqn + ". Make sure all types are fully qualified.");
                }
                return type;
            }

            @Override
            public JavaType visitTypeParameter(TemplateParameterParser.TypeParameterContext ctx) {
                JavaType type1 = super.visitTypeParameter(ctx);
                if (ctx.variance() != null) {
                    JavaType.GenericTypeVariable.Variance variance = ctx.variance().Extends() != null ?
                            JavaType.GenericTypeVariable.Variance.COVARIANT : JavaType.GenericTypeVariable.Variance.CONTRAVARIANT;
                    type1 = new JavaType.GenericTypeVariable(null, ctx.variance().WILDCARD().getText(), variance, singletonList(type1));
                } else if (ctx.WILDCARD() != null) {
                    type1 = new JavaType.GenericTypeVariable(null, ctx.WILDCARD().getText(), JavaType.GenericTypeVariable.Variance.INVARIANT, emptyList());
                }
                return type1;
            }
        });
    }

    public static Map<String, JavaType.GenericTypeVariable> parseGenericTypes(Set<String> genericTypes) {
        Map<String, List<TemplateParameterParser.GenericPatternContext>> contexts = genericTypes.stream()
                .map(e -> parser(e).genericPattern())
                .collect(groupingBy(e -> e.genericName().getText()));
        if (contexts.values().stream().anyMatch(e -> e.size() > 1)) {
            throw new IllegalArgumentException("Found duplicated generic type.");
        }

        Map<String, JavaType.GenericTypeVariable> genericTypesMap = contexts.keySet().stream()
                .collect(toMap(e -> e,
                        e -> new JavaType.GenericTypeVariable(null, e, JavaType.GenericTypeVariable.Variance.INVARIANT, emptyList())));

        for (String name : genericTypesMap.keySet()) {
            JavaType.GenericTypeVariable genericType = genericTypesMap.get(name);
            TemplateParameterParser.GenericPatternContext context = contexts.get(name).get(0);
            List<JavaType> bounds = context.type().stream()
                    .map(e -> toJavaType(e, genericTypesMap))
                    .collect(toList());
            if (!bounds.isEmpty()) {
                genericType.unsafeSet(name, JavaType.GenericTypeVariable.Variance.COVARIANT, bounds);
            }
        }
        return genericTypesMap;
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
