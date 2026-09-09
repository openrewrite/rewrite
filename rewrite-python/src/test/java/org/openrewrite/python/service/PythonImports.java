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
package org.openrewrite.python.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.python.tree.Py;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

/**
 * Builds the import shapes the dispatch predicates read, without a Python runtime to parse them.
 */
final class PythonImports {

    private PythonImports() {
    }

    static Py.CompilationUnit cu(Statement... statements) {
        List<JRightPadded<Statement>> padded = new ArrayList<>();
        for (Statement statement : statements) {
            padded.add(JRightPadded.build(statement));
        }
        return new Py.CompilationUnit(randomId(), Space.EMPTY, Markers.EMPTY, Paths.get("test.py"),
                null, null, false, null, emptyList(), padded, Space.EMPTY);
    }

    static Py.MultiImport fromImport(String from, J.Import... names) {
        return multiImport(JRightPadded.build((NameTree) name(from)), names);
    }

    static Py.MultiImport directImport(J.Import... names) {
        return multiImport(null, names);
    }

    static J.Import member(String dotted) {
        return member(dotted, null, null);
    }

    /** The type an attributed function import carries, e.g. {@code posixpath.join}. */
    static JavaType.Method methodType(String declaringType, String name) {
        // Casts pin the List-based overload; bare nulls are ambiguous with the array-based one.
        return new JavaType.Method(null, 0, JavaType.ShallowClass.build(declaringType), name,
                null, (List<String>) null, (List<JavaType>) null, (List<JavaType>) null,
                (List<JavaType.FullyQualified>) null, null, (List<String>) null);
    }

    static J.Import member(String dotted, @Nullable String alias, @Nullable JavaType type) {
        return new J.Import(randomId(), Space.EMPTY, Markers.EMPTY, JLeftPadded.build(false),
                qualid(dotted, type), alias == null ? null : JLeftPadded.build(identifier(alias)));
    }

    private static Py.MultiImport multiImport(@Nullable JRightPadded<NameTree> from, J.Import... names) {
        List<JRightPadded<J.Import>> padded = new ArrayList<>();
        for (J.Import name : Arrays.asList(names)) {
            padded.add(JRightPadded.build(name));
        }
        return new Py.MultiImport(randomId(), Space.EMPTY, Markers.EMPTY, from, false,
                JContainer.build(Space.SINGLE_SPACE, padded, Markers.EMPTY));
    }

    /** A dotted name as the parser models it outside an import qualid: no empty target. */
    private static Expression name(String dotted) {
        String[] parts = dotted.split("\\.");
        Expression result = identifier(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result = new J.FieldAccess(randomId(), Space.EMPTY, Markers.EMPTY, result,
                    JLeftPadded.build(identifier(parts[i])), null);
        }
        return result;
    }

    /** A qualid: a single-part name has a {@link J.Empty} target, as the parser emits it. */
    private static J.FieldAccess qualid(String dotted, @Nullable JavaType type) {
        String[] parts = dotted.split("\\.");
        Expression target = parts.length == 1 ?
                new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY) : identifier(parts[0]);
        for (int i = 1; i < parts.length - 1; i++) {
            target = new J.FieldAccess(randomId(), Space.EMPTY, Markers.EMPTY, target,
                    JLeftPadded.build(identifier(parts[i])), null);
        }
        return new J.FieldAccess(randomId(), Space.EMPTY, Markers.EMPTY, target,
                JLeftPadded.build(identifier(parts[parts.length - 1])), type);
    }

    private static J.Identifier identifier(String name) {
        return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, null, null);
    }
}
