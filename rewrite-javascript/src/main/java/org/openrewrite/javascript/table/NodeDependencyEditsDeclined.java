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
package org.openrewrite.javascript.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.NonNull;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class NodeDependencyEditsDeclined extends DataTable<NodeDependencyEditsDeclined.@NonNull Row> {

    public NodeDependencyEditsDeclined(Recipe recipe) {
        super(recipe, "Node.js dependency edits declined",
                "Edits a recipe did not make, or that the package manager applies only in part, and why.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
                description = "The file the edit belongs in.")
        String sourcePath;

        @Column(displayName = "Package name",
                description = "The dependency the edit was for.")
        String packageName;

        @Column(displayName = "Reason",
                description = "Why the package manager would not read the edit as asked.")
        String reason;
    }
}
