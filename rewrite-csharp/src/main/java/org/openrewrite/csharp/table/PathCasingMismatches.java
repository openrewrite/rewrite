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
package org.openrewrite.csharp.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class PathCasingMismatches extends DataTable<PathCasingMismatches.Row> {

    public PathCasingMismatches(Recipe recipe) {
        super(recipe, "MSBuild path casing mismatches",
                "Path references in solution and MSBuild project files whose casing differs from the file " +
                "or directory that is actually in the repository.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
                description = "The solution or MSBuild project file containing the reference.")
        String sourcePath;

        @Column(displayName = "Location",
                description = "Where in the file the reference was found, e.g. `ProjectReference/@Include`.")
        String location;

        @Column(displayName = "Reference",
                description = "The path reference as it was written.")
        String reference;

        @Column(displayName = "Aligned reference",
                description = "The path reference with each segment cased as it is in the repository.")
        String alignedReference;
    }
}
