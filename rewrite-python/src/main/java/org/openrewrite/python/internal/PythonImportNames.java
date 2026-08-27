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
package org.openrewrite.python.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/**
 * Reads the names and types an import binds, mirroring {@code rewrite.python.import_utils}.
 */
public final class PythonImportNames {

    private PythonImportNames() {
    }

    /**
     * The dotted name a name tree spells, e.g. {@code os.path}. A qualid whose target is
     * {@link J.Empty} spells just its own name, which is how a single-part import is modelled.
     */
    public static String nameString(@Nullable J name) {
        if (name instanceof J.Identifier) {
            return ((J.Identifier) name).getSimpleName();
        }
        if (name instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) name;
            String target = nameString(fieldAccess.getTarget());
            String simpleName = fieldAccess.getName().getSimpleName();
            return target.isEmpty() ? simpleName : target + "." + simpleName;
        }
        return "";
    }

    public static @Nullable String aliasName(J.Import imp) {
        return imp.getAlias() == null ? null : imp.getAlias().getSimpleName();
    }

    /**
     * The canonical fully qualified name of the symbol the import binds, which differs from the
     * written path for re-exports: {@code from os.path import join} binds {@code posixpath.join}.
     */
    public static @Nullable String canonicalFqn(J.Import imp) {
        JavaType type = imp.getQualid().getType();
        if (type instanceof JavaType.Method) {
            JavaType.Method method = (JavaType.Method) type;
            JavaType.FullyQualified declaring = method.getDeclaringType();
            if (declaring instanceof JavaType.Unknown || method.getName().isEmpty()) {
                return null;
            }
            return declaring.getFullyQualifiedName() + "." + method.getName();
        }
        if (type instanceof JavaType.Parameterized) {
            type = ((JavaType.Parameterized) type).getType();
        }
        if (type instanceof JavaType.FullyQualified && !(type instanceof JavaType.Unknown)) {
            return ((JavaType.FullyQualified) type).getFullyQualifiedName();
        }
        return null;
    }
}
