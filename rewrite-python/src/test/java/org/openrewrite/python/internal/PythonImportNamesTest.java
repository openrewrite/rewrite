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
import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class PythonImportNamesTest {

    private static J.Identifier ident(String name) {
        return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, null, null);
    }

    private static J.FieldAccess qualid(String dotted, @Nullable JavaType type) {
        String[] parts = dotted.split("\\.");
        Expression target = parts.length == 1 ?
                new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY) : ident(parts[0]);
        for (int i = 1; i < parts.length - 1; i++) {
            target = new J.FieldAccess(randomId(), Space.EMPTY, Markers.EMPTY, target,
                    JLeftPadded.build(ident(parts[i])), null);
        }
        return new J.FieldAccess(randomId(), Space.EMPTY, Markers.EMPTY, target,
                JLeftPadded.build(ident(parts[parts.length - 1])), type);
    }

    private static J.Import imp(String dotted, @Nullable String alias, @Nullable JavaType type) {
        return new J.Import(randomId(), Space.EMPTY, Markers.EMPTY,
                JLeftPadded.build(false), qualid(dotted, type),
                alias == null ? null : JLeftPadded.build(ident(alias)));
    }

    @Test
    void nameStringOfIdentifier() {
        assertThat(PythonImportNames.nameString(ident("os"))).isEqualTo("os");
    }

    @Test
    void nameStringOfDottedFieldAccess() {
        assertThat(PythonImportNames.nameString(qualid("os.path", null))).isEqualTo("os.path");
    }

    @Test
    void nameStringSkipsEmptyTarget() {
        assertThat(PythonImportNames.nameString(qualid("join", null))).isEqualTo("join");
    }

    @Test
    void nameStringOfNull() {
        assertThat(PythonImportNames.nameString(null)).isEmpty();
    }

    @Test
    void aliasNameIsNullWithoutAlias() {
        assertThat(PythonImportNames.aliasName(imp("os", null, null))).isNull();
    }

    @Test
    void aliasNameReadsTheAlias() {
        assertThat(PythonImportNames.aliasName(imp("numpy", "np", null))).isEqualTo("np");
    }

    @Test
    void canonicalFqnOfMethodType() {
        JavaType.Method method = new JavaType.Method(null, 0, JavaType.ShallowClass.build("posixpath"),
                "join", null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList());
        assertThat(PythonImportNames.canonicalFqn(imp("join", null, method))).isEqualTo("posixpath.join");
    }

    @Test
    void canonicalFqnOfClassType() {
        assertThat(PythonImportNames.canonicalFqn(imp("Iterable", null, JavaType.ShallowClass.build("typing.Iterable"))))
                .isEqualTo("typing.Iterable");
    }

    @Test
    void canonicalFqnIsNullWhenUnattributed() {
        assertThat(PythonImportNames.canonicalFqn(imp("Iterable", null, null))).isNull();
    }

    @Test
    void canonicalFqnIsNullForUnknownType() {
        assertThat(PythonImportNames.canonicalFqn(imp("Iterable", null, JavaType.Unknown.getInstance()))).isNull();
    }
}
