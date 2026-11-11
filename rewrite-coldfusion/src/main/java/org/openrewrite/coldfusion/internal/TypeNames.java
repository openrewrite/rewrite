/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.coldfusion.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/**
 * The names that name types, a dotted name such as {@code models.User} or a string such as {@code "models.User"}.
 */
final class TypeNames {
    private TypeNames() {
    }

    /**
     * The name a tree names a type with, or null when it is not a plain name.
     */
    static @Nullable String text(@Nullable J tree) {
        if (tree instanceof J.Identifier) {
            return ((J.Identifier) tree).getSimpleName();
        }
        if (tree instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) tree;
            String target = text(fieldAccess.getTarget());
            return target == null ? null : target + "." + fieldAccess.getSimpleName();
        }
        if (tree instanceof Cf.TypeLiteral && ((Cf.TypeLiteral) tree).getName() instanceof J.Literal) {
            Object value = ((J.Literal) ((Cf.TypeLiteral) tree).getName()).getValue();
            return value instanceof String ? (String) value : null;
        }
        return null;
    }

    /**
     * A tree naming a type, given that type, as a dotted name has it on itself and on its last name.
     */
    @SuppressWarnings("unchecked")
    static <T extends J> T withType(T tree, JavaType type) {
        if (tree instanceof J.Identifier) {
            return (T) ((J.Identifier) tree).withType(type);
        }
        if (tree instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) tree;
            return (T) fieldAccess.withName(fieldAccess.getName().withType(type)).withType(type);
        }
        if (tree instanceof Cf.TypeLiteral) {
            return (T) ((Cf.TypeLiteral) tree).withType(type);
        }
        return tree;
    }
}
