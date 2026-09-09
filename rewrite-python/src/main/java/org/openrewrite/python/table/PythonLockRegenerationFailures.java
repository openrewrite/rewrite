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

@JsonIgnoreType
public class PythonLockRegenerationFailures extends DataTable<PythonLockRegenerationFailures.@NonNull Row> {

    public PythonLockRegenerationFailures(Recipe recipe) {
        super(recipe, "Python lock regeneration failures",
                "Lock files that could not be regenerated after a dependency edit, and why.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
                description = "The path of the dependencies file whose lock could not be regenerated.")
        String sourcePath;

        @Column(displayName = "Package name",
                description = "The package that caused the failure, when attributable to one.")
        @Nullable
        String packageName;

        @Column(displayName = "Reason",
                description = "The structured failure reason, e.g. INDEX_UNREACHABLE or RESOLUTION_REQUIRED. " +
                        "Absent for unstructured failures such as a missing package manager executable.")
        @Nullable
        String reason;

        @Column(displayName = "Detail",
                description = "A human-readable description of the failure.")
        String detail;
    }
}
