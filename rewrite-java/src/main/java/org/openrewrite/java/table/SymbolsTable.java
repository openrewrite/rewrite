/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.table;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class SymbolsTable extends DataTable<SymbolsTable.Row> {

    public SymbolsTable(Recipe recipe) {
        super(recipe, "Symbols overview",
                "All symbols (classes, methods, fields) declared in the codebase.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
                description = "The path to the source file containing this symbol.")
        String sourcePath;

        @Column(displayName = "Symbol kind",
                description = "The kind of symbol: CLASS, INTERFACE, ENUM, RECORD, ANNOTATION, METHOD, CONSTRUCTOR, or FIELD.")
        String kind;

        @Column(displayName = "Name",
                description = "The simple name of the symbol.")
        String name;

        @Nullable
        @Column(displayName = "Parent type",
                description = "The fully qualified name of the enclosing type, if any.")
        String parentFqn;

        @Nullable
        @Column(displayName = "Signature",
                description = "For methods: the method signature. For fields: the field type.")
        String signature;

        @Nullable
        @Column(displayName = "Visibility",
                description = "The access modifier: PUBLIC, PROTECTED, PRIVATE, or PACKAGE_PRIVATE.")
        String visibility;
    }
}
