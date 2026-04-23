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
 * The output of the {@code mod test} test-selection pipeline: the set of tests
 * that a downstream runner should execute for a given change set. Each row
 * identifies a single test target (a class, or optionally a single method)
 * along with enough metadata for a runner to invoke it.
 * <p>
 * Schema v1 — consumed by the {@code mod test} runner adapter.
 */
public class TestPlanDataTable extends DataTable<TestPlanDataTable.Row> {

    public TestPlanDataTable(Recipe recipe) {
        super(recipe,
                "Test plan",
                "Tests selected for execution based on a change set, with the module, " +
                        "test identifier, reason for selection, language, and target runner.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Module",
                description = "Module path relative to the repository root (e.g. `core/api`). " +
                        "Matches the `module` column of `AffectedModulesDataTable`.")
        String module;

        @Column(displayName = "Test class",
                description = "Fully-qualified name of the test class to execute.")
        String testClass;

        @Column(displayName = "Test method",
                description = "Name of the test method, or blank if the entire class is selected. " +
                        "In schema v1 this is typically blank — tests are selected at class granularity.")
        String testMethod;

        @Column(displayName = "Reason",
                description = "Free-text reason for selection. Conventional values include " +
                        "`module-dep-changed`, `build-file-changed`, or `bailout:<code>` when " +
                        "the selector bails out and falls back to running everything.")
        String reason;

        @Column(displayName = "Language",
                description = "Source language of the test file: `java`, `kotlin`, or `groovy`.")
        String language;

        @Column(displayName = "Runner",
                description = "Build tool that should execute the test: `gradle` or `mvn`.")
        String runner;
    }
}
