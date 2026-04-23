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
 * Test fixture for {@code BailoutReasonsDataTable}. See {@link ReachabilityFixtureTable}
 * for the rationale — this duplicates the production FQN via a {@link #getName()}
 * override so the selector's string-name lookup finds our rows.
 */
class BailoutFixtureTable extends DataTable<BailoutFixtureTable.Row> {

    BailoutFixtureTable(Recipe recipe) {
        super(recipe, "Bailouts (fixture)", "Test fixture — pretends to be BailoutReasonsDataTable.");
    }

    @Override
    public String getName() {
        return "org.openrewrite.analysis.testselection.table.BailoutReasonsDataTable";
    }

    @Value
    static class Row {
        String reason;
        String detail;
    }
}
