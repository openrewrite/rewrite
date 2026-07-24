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

    /**
     * Render a Java type name emitted by {@link Substitutions} as a Kotlin type so that the Kotlin
     * parser can resolve members accessed on the substituted placeholder.
     * <ul>
     *     <li>Wildcards ({@code ? extends}, {@code ? super}, {@code ?}) become Kotlin variance/projection syntax.</li>
     *     <li>Arrays ({@code T[]}) become {@code kotlin.Array<T>} or a primitive array type, since Kotlin has no
     *     {@code T[]} syntax.</li>
     *     <li>A top-level generic type named without type arguments (a raw type) gets star projections, since Kotlin
     *     has no raw types and a bare generic name resolves to an error type — which suppresses attribution of any
     *     member selected on it. Star projection only applies to the substituted type itself, whose arity is recovered
     *     from the actual argument; nested type arguments (e.g. array elements) are rendered structurally only.</li>
     * </ul>
     */
    private String toKotlinTypeSyntax(String fqn, int index, boolean starProjectRaw) {
        if (fqn.endsWith("[]")) {
            return toKotlinArrayType(fqn.substring(0, fqn.length() - "[]".length()), index);
        }
        String kotlinType = fqn
                .replace("? extends ", "out ")
                .replace("? super ", "in ")
                .replace("?", "*");
        if (starProjectRaw && kotlinType.indexOf('<') < 0) {
            int arity = genericArity(index);
            if (arity > 0) {
                StringBuilder sb = new StringBuilder(kotlinType).append('<');
                for (int i = 0; i < arity; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append('*');
                }
                return sb.append('>').toString();
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

    /**
     * The number of type parameters the substituted parameter's own type carries, used to star-project a
     * template-declared raw generic type. The declared type (from the template text) is a shallow class with no
     * type parameters, so the arity is recovered from the actual argument instead.
     */
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
