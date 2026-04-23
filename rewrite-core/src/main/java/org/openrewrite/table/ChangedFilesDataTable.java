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
 * Input data table for test impact analysis. Contains the set of files
 * changed since some baseline reference (typically a merge-base commit).
 * <p>
 * Producers populate this table before running a test selection recipe —
 * for example, a JGit-based scanner that diffs the working tree against
 * {@code origin/main}. Consumers (test selection recipes) read rows to
 * decide which modules need to be re-tested.
 * <p>
 * Paths are repository-relative using forward-slash separators, matching
 * the convention of {@code org.openrewrite.SourceFile#getSourcePath()}.
 */
public class ChangedFilesDataTable extends DataTable<ChangedFilesDataTable.Row> {

    public ChangedFilesDataTable(Recipe recipe) {
        super(recipe,
                "Changed files",
                "The set of files that have changed relative to a baseline reference. " +
                        "Used as input to test impact analysis recipes.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Path",
                description = "Repository-relative path to the changed file, using forward-slash separators.")
        String path;

        @Column(displayName = "Change type",
                description = "The kind of change: one of ADDED, MODIFIED, DELETED, or RENAMED.")
        String changeType;
    }
}
