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
public class NodeDependencyProtocolsSkipped extends DataTable<NodeDependencyProtocolsSkipped.@NonNull Row> {

    public NodeDependencyProtocolsSkipped(Recipe recipe) {
        super(recipe, "Node.js dependencies skipped for their protocol",
                "Dependencies a recipe left unchanged because their value is not a version range.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
                description = "The package.json the recipe left unchanged.")
        String sourcePath;

        @Column(displayName = "Declared in",
                description = "The package.json declaring the value.")
        String declaredIn;

        @Column(displayName = "Package name",
                description = "The dependency left unchanged.")
        String packageName;

        @Column(displayName = "Dependency scope",
                description = "The manifest field declaring it.")
        String dependencyScope;

        @Column(displayName = "Protocol",
                description = "The protocol the value names or expands to.")
        String protocol;

        @Column(displayName = "Current value",
                description = "The declared value.")
        String currentValue;

        @Column(displayName = "Requested version",
                description = "The version the recipe was asked for.")
        String requestedVersion;
    }
}
