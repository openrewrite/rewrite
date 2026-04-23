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
 * Modules in the current repository that need to be re-tested in response to a
 * set of changed files. Produced by the build-file classifier and consumed by
 * downstream test selection recipes (see {@link TestPlanDataTable}).
 * <p>
 * A given module may appear in multiple rows when multiple changed files
 * affect it; a downstream consumer typically collapses by module path.
 * <p>
 * Common reason values include:
 * <ul>
 *     <li>{@code build-file-changed} — a build descriptor inside the module changed</li>
 *     <li>{@code source-changed} — a source file inside the module changed</li>
 *     <li>{@code module-dep-of-affected} — a downstream module that depends on an affected module</li>
 *     <li>{@code repo-root-bailout:<path>} — a repo-root file changed that we could not localize,
 *         so every module is flagged conservatively</li>
 * </ul>
 */
public class AffectedModulesDataTable extends DataTable<AffectedModulesDataTable.Row> {

    public AffectedModulesDataTable(Recipe recipe) {
        super(recipe,
                "Affected modules",
                "Modules in the current repository that are considered affected by a change set. " +
                        "Consumed by `mod test` as the input to test selection.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Module",
                description = "Module path relative to the repository root. The root module itself " +
                        "is represented as the empty string.")
        String module;

        @Column(displayName = "Reason",
                description = "Free-text explanation of why this module was flagged: e.g. " +
                        "`build-file-changed`, `source-changed`, `module-dep-of-affected`, " +
                        "or `repo-root-bailout:<path>`.")
        String reason;
    }
}
