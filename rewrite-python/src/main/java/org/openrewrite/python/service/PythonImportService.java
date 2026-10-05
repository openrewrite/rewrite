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
        String fqType = member == null ? packageName : packageName == null ? typeName : packageName + "." + typeName;
        String boundName = member == null ? typeName : member;
        // A name with no dot becomes a plain `import <name>` with a null name.
        String module = fqType == null || fqType.isEmpty() ? boundName : fqType;
        String name = fqType == null || fqType.isEmpty() ? null : boundName;
        return new PythonAddImportVisitor<>(module, name, alias, onlyIfReferenced);
    }

    @Override
    public <P> JavaVisitor<P> removeImportVisitor(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        String packageName = lastDot == -1 ? null : fullyQualifiedName.substring(0, lastDot);
        String typeName = lastDot == -1 ? fullyQualifiedName : fullyQualifiedName.substring(lastDot + 1);
        String module = packageName == null || packageName.isEmpty() ? typeName : packageName;
        String name = packageName == null || packageName.isEmpty() ? null : typeName;
        return new PythonRemoveImportVisitor<>(module, name);
    }

    /**
     * Python imports are {@code Py.MultiImport} statements, so {@code getImports()} is always empty.
     */
    @Override
    public boolean usesStatementBasedImports() {
        return true;
    }
}
