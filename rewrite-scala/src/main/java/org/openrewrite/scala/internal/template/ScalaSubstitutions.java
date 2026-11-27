/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.internal.template;

import org.openrewrite.Cursor;
import org.openrewrite.java.internal.template.Substitutions;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.scala.ScalaParenthesizeVisitor;

import static java.util.Collections.emptySet;

public class ScalaSubstitutions extends Substitutions {
    public ScalaSubstitutions(String code, Object[] parameters) {
        super(code, emptySet(), parameters);
    }

    @Override
    protected String newObjectParameter(String fqn, int index) {
        StringBuilder scalaType = new StringBuilder();
        toScalaType(fqn, 0, scalaType);
        return "__P__./*__p" + index + "__*/p[" + scalaType + "]()";
    }

    @Override
    protected String newPrimitiveParameter(String fqn, int index) {
        return newObjectParameter(fqn, index);
    }

    /**
     * Writes the type at {@code pos} of a Java type name, as {@link Substitutions} denotes a parameter's type, in Scala
     * syntax and returns the position after it. The Java name already settles how a type without a denotable name is
     * written, such as an anonymous class or an intersection type.
     */
    private static int toScalaType(String javaType, int pos, StringBuilder out) {
        if (javaType.startsWith("?", pos)) {
            out.append('?');
            if (javaType.startsWith("? extends ", pos)) {
                out.append(" <: ");
                return toScalaType(javaType, pos + "? extends ".length(), out);
            } else if (javaType.startsWith("? super ", pos)) {
                out.append(" >: ");
                return toScalaType(javaType, pos + "? super ".length(), out);
            }
            return pos + 1;
        }

        int nameEnd = pos;
        while (nameEnd < javaType.length() && "<>,[ ".indexOf(javaType.charAt(nameEnd)) < 0) {
            nameEnd++;
        }
        StringBuilder type = new StringBuilder(toScalaName(javaType.substring(pos, nameEnd)));
        pos = nameEnd;
        if (javaType.startsWith("<", pos)) {
            type.append('[');
            pos = toScalaType(javaType, pos + 1, type);
            while (javaType.startsWith(", ", pos)) {
                type.append(", ");
                pos = toScalaType(javaType, pos + 2, type);
            }
            type.append(']');
            pos++;
        }
        for (; javaType.startsWith("[]", pos); pos += 2) {
            type.insert(0, "Array[").append(']');
        }
        out.append(type);
        if (javaType.startsWith(" | ", pos)) {
            out.append(" | ");
            return toScalaType(javaType, pos + " | ".length(), out);
        }
        return pos;
    }

    private static String toScalaName(String javaName) {
        JavaType.Primitive primitive = JavaType.Primitive.fromKeyword(javaName);
        if (primitive == JavaType.Primitive.Void) {
            // Scala's `Unit` maps to `void`, and `Void` would denote `java.lang.Void`
            return "Unit";
        }
        return primitive == null || primitive == JavaType.Primitive.String || javaName.isEmpty() ?
                javaName :
                Character.toUpperCase(javaName.charAt(0)) + javaName.substring(1);
    }

    @Override
    public Expression maybeParenthesize(Expression newTree, Cursor cursor) {
        return ScalaParenthesizeVisitor.maybeParenthesize(newTree, cursor);
    }
}
