/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.internal.template;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.internal.template.Substitutions;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypedTree;

import java.util.Collections;

import static java.util.Collections.emptySet;

public class KotlinSubstitutions extends Substitutions {
    private final Object[] parameters;

    public KotlinSubstitutions(String code, Object[] parameters) {
        super(code, emptySet(), parameters);
        this.parameters = parameters;
    }

    @Override
    protected String newObjectParameter(String fqn, int index) {
        return "__P__./*__p" + index + "__*/p<" + toKotlinTypeSyntax(fqn, index, true) + ">()";
    }

    // Kotlin has no `T[]` syntax and no raw types: a bare generic name resolves to an error type, which
    // suppresses attribution of members selected on the placeholder. Star-project raw types (top-level only)
    // and render arrays as kotlin.Array<T> / primitive array types.
    private String toKotlinTypeSyntax(String fqn, int index, boolean starProjectRaw) {
        if (fqn.endsWith("[]")) {
            return toKotlinArrayType(fqn.substring(0, fqn.length() - 2), index);
        }
        String kotlinType = fqn
                .replace("? extends ", "out ")
                .replace("? super ", "in ")
                .replace("?", "*");
        if (starProjectRaw && kotlinType.indexOf('<') < 0) {
            int arity = genericArity(index);
            if (arity > 0) {
                return kotlinType + "<" + String.join(", ", Collections.nCopies(arity, "*")) + ">";
            }
        }
        return kotlinType;
    }

    private String toKotlinArrayType(String elementFqn, int index) {
        switch (elementFqn) {
            case "boolean":
                return "kotlin.BooleanArray";
            case "byte":
                return "kotlin.ByteArray";
            case "char":
                return "kotlin.CharArray";
            case "double":
                return "kotlin.DoubleArray";
            case "float":
                return "kotlin.FloatArray";
            case "int":
                return "kotlin.IntArray";
            case "long":
                return "kotlin.LongArray";
            case "short":
                return "kotlin.ShortArray";
            case "java.lang.Object":
                return "kotlin.Array<*>";
            default:
                return "kotlin.Array<" + toKotlinTypeSyntax(elementFqn, index, false) + ">";
        }
    }

    // The template-declared type is a shallow class with no type parameters, so a raw type's arity is
    // recovered from the actual argument instead.
    private int genericArity(int index) {
        if (index < 0 || index >= parameters.length || !(parameters[index] instanceof TypedTree)) {
            return 0;
        }
        JavaType type = ((TypedTree) parameters[index]).getType();
        if (type instanceof JavaType.Parameterized) {
            return ((JavaType.Parameterized) type).getTypeParameters().size();
        }
        if (type instanceof JavaType.FullyQualified) {
            return ((JavaType.FullyQualified) type).getTypeParameters().size();
        }
        return 0;
    }

    @Override
    protected String newPrimitiveParameter(String fqn, int index) {
        return newObjectParameter(fqn, index);
    }
}
