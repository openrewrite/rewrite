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

        @Column(displayName = "Trigger path",
                description = "Repository-relative path of the changed file that caused this row. " +
                        "For cascade rows (`module-dep-of-affected`) this is the file in the " +
                        "upstream affected module. For `repo-root-bailout` rows this is the " +
                        "root file that triggered the bailout. May be empty when no specific " +
                        "trigger applies (e.g. synthetic root fallback).")
        String triggerPath;

        @Column(displayName = "Via",
                description = "Immediate upstream module that propagated the impact to this row. " +
                        "Empty for direct rows (`source-changed`, `build-file-changed`, " +
                        "`repo-root-bailout`); for `module-dep-of-affected` rows this is the " +
                        "module whose change cascaded here. Lets a consumer reconstruct the " +
                        "module-DAG path from a changed file to a reached module.")
        String via;
    }
}
