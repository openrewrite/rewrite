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

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class TypeParameter {

    private static final JavaType.Class TYPE_OBJECT = JavaType.ShallowClass.build("java.lang.Object");

    public static JavaType toFullyQualifiedName(@Nullable TemplateParameterParser.TypeContext type) {
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
                return type1;
            }

            @Override
            public JavaType visitTypeName(TemplateParameterParser.TypeNameContext ctx) {
                String fqn = ctx.FullyQualifiedName() != null ? ctx.FullyQualifiedName().getText() : ctx.Identifier().getText();
                JavaType type;
                if (fqn.contains(".")) {
                    type = JavaType.ShallowClass.build(fqn);
                } else if (fqn.equals("String")) {
                    type = JavaType.ShallowClass.build("java.lang.String");
                } else if (fqn.equals("Object")) {
                    type = TYPE_OBJECT;
                } else if ((type = JavaType.Primitive.fromKeyword(fqn)) != null) {
                    // empty
                } else {
                    type = JavaType.Unknown.getInstance();
                }
                return type;
            }

            @Override
            public JavaType visitTypeParameter(TemplateParameterParser.TypeParameterContext ctx) {
                JavaType type1 = super.visitTypeParameter(ctx);
                if (ctx.variance() != null) {
                    JavaType.GenericTypeVariable.Variance variance = ctx.variance().Variance().getSymbol().getText().equals("extends") ?
                            JavaType.GenericTypeVariable.Variance.COVARIANT : JavaType.GenericTypeVariable.Variance.CONTRAVARIANT;
                    type1 = new JavaType.GenericTypeVariable(null, ctx.variance().WILDCARD().getText(), variance, singletonList(type1));
                } else if (ctx.WILDCARD() != null) {
                    type1 = new JavaType.GenericTypeVariable(null, ctx.WILDCARD().getText(), JavaType.GenericTypeVariable.Variance.INVARIANT, emptyList());
                }
                return type1;
            }
        });
    }
}
