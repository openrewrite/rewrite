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
package org.openrewrite.python.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.NonNull;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/**
 * Records the outcome of regenerating a Python lock file after a dependency recipe edited its
 * manifest. One {@link Row} is emitted per project the recipe touched, so that on a large org
 * run the results can be aggregated to answer "which repos failed to regenerate their lock, and
 * why" — distinguishing <em>couldn't try</em> from <em>tried, nothing changed</em> from
 * <em>regenerated</em>.
 */
@JsonIgnoreType
public class PythonLockFileRegenerationResults extends DataTable<PythonLockFileRegenerationResults.@NonNull Row> {

    public PythonLockFileRegenerationResults(Recipe recipe) {
        super(recipe,
                "Python lock file regeneration results",
                "The outcome of regenerating a Python lock file (`uv.lock` / `Pipfile.lock`) after a " +
                        "dependency recipe edited its manifest.");
    }

    /**
     * The regeneration outcome for a single project.
     */
    public enum Status {
        /** The lock file was regenerated and its content differs from the original. */
        REGENERATED,
        /** Regeneration succeeded but produced content identical to the original lock. */
        UNCHANGED,
        /** The manifest changed but no lock file was present to regenerate. */
        NO_LOCK_PRESENT,
        /** The package manager (`uv` / `pipenv`) was not installed, so regeneration could not be attempted. */
        TOOL_NOT_INSTALLED,
        /** The package manager ran but failed to produce a new lock (see {@code detail}). */
        FAILED
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
                description = "The path to the manifest that was edited (e.g., `Pipfile` or `pyproject.toml`).")
        String sourcePath;

        @Column(displayName = "Lock file",
                description = "The name of the lock file corresponding to the manifest (e.g., `Pipfile.lock` or `uv.lock`).")
        @Nullable
        String lockFile;

        @Column(displayName = "Package manager",
                description = "The package manager responsible for the lock file (`pipenv` or `uv`).")
        @Nullable
        String packageManager;

        @Column(displayName = "Status",
                description = "The regeneration outcome: REGENERATED, UNCHANGED, NO_LOCK_PRESENT, TOOL_NOT_INSTALLED, or FAILED.")
        Status status;

        @Column(displayName = "Detail",
                description = "Additional detail, such as the error message when regeneration failed. Empty on success.")
        @Nullable
        String detail;
    }
}
