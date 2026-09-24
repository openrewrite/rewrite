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
        super(recipe, "Node.js dependencies skipped for a specifier protocol",
                "Dependencies whose version position holds a specifier protocol rather than a version " +
                        "constraint, so the recipe left that value alone. The constraint lives behind the " +
                        "protocol (in a pnpm catalog, a workspace member, a patch), and overwriting the " +
                        "reference would discard it.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
                description = "The path of the package.json declaring the dependency.")
        String sourcePath;

        @Column(displayName = "Package name",
                description = "The dependency that was left alone.")
        String packageName;

        @Column(displayName = "Dependency scope",
                description = "The manifest field declaring it, e.g. dependencies or devDependencies.")
        String dependencyScope;

        @Column(displayName = "Protocol",
                description = "The specifier protocol found in the version position, e.g. `catalog:` or `workspace:`.")
        String protocol;

        @Column(displayName = "Current value",
                description = "The declared value, left unchanged.")
        String currentValue;

        @Column(displayName = "Requested version",
                description = "The version constraint the recipe would have set had this been a plain constraint.")
        String requestedVersion;
    }
}
