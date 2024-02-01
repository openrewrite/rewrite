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

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.grammar.TemplateParameterParser;
import org.openrewrite.java.internal.grammar.TemplateParameterParserBaseVisitor;
import org.openrewrite.java.tree.JavaType;

import java.util.*;

public class TypeParameter {

    private static final JavaType.Class TYPE_OBJECT = JavaType.ShallowClass.build("java.lang.Object");

    public static JavaType toFullyQualifiedName(@Nullable TemplateParameterParser.TypeContext type) {
        if (type == null) {
            return TYPE_OBJECT;
        }

        JavaType result = type.accept(new TemplateParameterParserBaseVisitor<JavaType>() {
            final Deque<List<JavaType>> typeParameterStack = new ArrayDeque<>();

            @Override
            public JavaType visitType(TemplateParameterParser.TypeContext ctx) {
                JavaType type = ctx.typeName().accept(this);
                if (!ctx.typeParameter().isEmpty()) {
                    List<JavaType> typeParameters = new ArrayList<>();
                    for (TemplateParameterParser.TypeParameterContext param : ctx.typeParameter()) {
                        typeParameters.add(param.accept(this));
                    }
                    type = new JavaType.Parameterized(null, (JavaType.FullyQualified) type, typeParameters);
                }
                return type;
            }

            @Override
            public JavaType visitTypeName(TemplateParameterParser.TypeNameContext ctx) {
                String fqn = ctx.FullyQualifiedName().getText();
                if (fqn.contains(".")) {
                    return JavaType.ShallowClass.build(fqn);
                } else if (fqn.equals("String")) {
                    return JavaType.ShallowClass.build("java.lang.String");
                } else if (fqn.equals("Object")) {
                    return TYPE_OBJECT;
                } else {
                    return JavaType.Primitive.fromKeyword(fqn);
                }
            }

            @Override
            public JavaType visitTypeParameter(TemplateParameterParser.TypeParameterContext ctx) {
                JavaType type = super.visitTypeParameter(ctx);
                if (ctx.variance() != null) {
                    JavaType.GenericTypeVariable.Variance variance = ctx.variance().Variance().getSymbol().getText().equals("extends") ?
                            JavaType.GenericTypeVariable.Variance.COVARIANT : JavaType.GenericTypeVariable.Variance.CONTRAVARIANT;
                    type = new JavaType.GenericTypeVariable(null, ctx.variance().WILDCARD().getText(), variance, Collections.singletonList(type));
                }
                return type;
            }

            //            @Override
//            public JavaType visitTerminal(TerminalNode node) {
//                if (node.getSymbol().getType() == TemplateParameterLexer.LBRACK) {
//                    return JavaType.ShallowClass.build("java.lang.String");
//                }
//                return super.visitTerminal(node);
//            }
        });
        if (result != null) {
            return result;
        }
        String fqn = "";
        TemplateParameterParser.TypeNameContext typeName = type.typeName();

        if (typeName.Identifier() != null) {
            fqn = typeName.Identifier().getText();
        } else {
            fqn = typeName.FullyQualifiedName().getText();
        }

        return TYPE_OBJECT;
    }
}
