/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/**
 * Input data table for Phase 2 call-graph reachability analysis. Contains the
 * set of declared symbols (classes, methods, fields, constructors) that belong
 * to changed source files — in other words, "what are the targets whose callers
 * we want to find?".
 * <p>
 * Producers typically read {@link ChangedFilesDataTable} and walk the
 * corresponding LSTs, emitting one row per declared symbol per changed file
 * (see {@code ClassifyChangedSymbols}). Consumers (e.g. {@code ComputeReachability})
 * seed a backward call-graph walk from these rows.
 * <p>
 * Coarseness in v1: every class/method/field declared in a changed file is
 * considered "changed". This is a conservative approximation — it avoids the
 * need for pre/post LST diffing while still dramatically reducing the set of
 * test classes selected compared to module-level selection.
 */
public class ChangedSymbolsDataTable extends DataTable<ChangedSymbolsDataTable.Row> {

    public ChangedSymbolsDataTable(Recipe recipe) {
        super(recipe,
                "Changed symbols",
                "The set of declared symbols (classes, methods, fields, constructors) " +
                        "that belong to changed source files. Used as seeds for backward " +
                        "call-graph reachability analysis.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Class name",
                description = "Fully-qualified name of the class the symbol belongs to " +
                        "(the class itself for `CLASS` rows).")
        String className;

        @Column(displayName = "Member name",
                description = "Simple name of the method or field. Empty for `CLASS` rows. " +
                        "For constructors, `<constructor>`.")
        String memberName;

        @Column(displayName = "Member kind",
                description = "One of: `CLASS`, `METHOD`, `FIELD`, `CONSTRUCTOR`.")
        String memberKind;

        @Column(displayName = "Change type",
                description = "One of: `ADDED`, `MODIFIED`, `DELETED`, matching the row in " +
                        "`ChangedFilesDataTable` that produced this symbol.")
        String changeType;
    }
}
