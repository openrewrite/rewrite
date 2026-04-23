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
package org.openrewrite.java.search;

import lombok.Value;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/**
 * Test fixture that masquerades as
 * {@code org.openrewrite.analysis.testselection.table.ReachabilityDataTable} by
 * overriding {@link DataTable#getName()}. The production selector (in
 * {@code SelectTestsInAffectedModules}) looks up this table by FQN via the
 * generic {@code DataTableStore#getRows(String, String)} API, so we don't need
 * to link against rewrite-program-analysis here.
 */
class ReachabilityFixtureTable extends DataTable<ReachabilityFixtureTable.Row> {

    ReachabilityFixtureTable(Recipe recipe) {
        super(recipe, "Reachability (fixture)", "Test fixture — pretends to be ReachabilityDataTable.");
    }

    @Override
    public String getName() {
        return "org.openrewrite.analysis.testselection.table.ReachabilityDataTable";
    }

    @Value
    static class Row {
        String sourceClass;
        String sourceMethod;
        String targetClass;
        String targetMethod;
        int pathLength;
    }
}
