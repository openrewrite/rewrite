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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.service.ImportService;

/**
 * Dispatches {@link org.openrewrite.java.JavaVisitor#maybeAddImport}/{@code maybeRemoveImport} to
 * the Python side over RPC, since Java's {@link ImportService} emits a {@code J.Import} that prints
 * as invalid {@code import a.b.C} and works off an empty {@code getImports()} for Python.
 */
public class PythonImportService extends ImportService {

    @Override
    public <P> JavaVisitor<P> addImportVisitor(@Nullable String packageName,
                                               String typeName,
                                               @Nullable String member,
                                               @Nullable String alias,
                                               boolean onlyIfReferenced) {
        // A `member` is Java's static import, whose Python equivalent imports the member from
        // the type's own module: `from <package>.<type> import <member>`.
        String[] moduleAndName = member == null ?
                moduleAndName(packageName, typeName) :
                moduleAndName(packageName == null ? typeName : packageName + "." + typeName, member);
        return new PythonAddImportVisitor<>(moduleAndName[0], moduleAndName[1], alias, onlyIfReferenced);
    }

    @Override
    public <P> JavaVisitor<P> removeImportVisitor(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        String[] moduleAndName = lastDot == -1 ?
                moduleAndName(null, fullyQualifiedName) :
                moduleAndName(fullyQualifiedName.substring(0, lastDot), fullyQualifiedName.substring(lastDot + 1));
        return new PythonRemoveImportVisitor<>(moduleAndName[0], moduleAndName[1]);
    }

    /**
     * Python imports are {@code Py.MultiImport} statements, so {@code getImports()} is always empty.
     */
    @Override
    public boolean usesStatementBasedImports() {
        return true;
    }

    /**
     * Splits a fully-qualified name into Python's (module, name) pair, e.g. {@code collections.abc}
     * + {@code Iterable}; a name with no dot becomes a plain {@code import <name>} with a null name.
     */
    private static String[] moduleAndName(@Nullable String packageName, String typeName) {
        return packageName == null || packageName.isEmpty() ?
                new String[]{typeName, null} :
                new String[]{packageName, typeName};
    }
}
