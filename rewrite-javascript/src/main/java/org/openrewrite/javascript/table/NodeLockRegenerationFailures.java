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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NodeLockRegenerationFailures extends DataTable<NodeLockRegenerationFailures.Row> {

    public NodeLockRegenerationFailures(Recipe recipe) {
        super(recipe,
                "Node lock file regeneration failures",
                "Records why a lock file could not be regenerated after a dependency recipe " +
                        "changed a `package.json`, so fleet-scale runs can aggregate causes. " +
                        "On any failure the manifest edit still applies and the old lock is left untouched.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
                description = "The lock file that was not regenerated.")
        String sourcePath;

        @Column(displayName = "Package name",
                description = "The package whose resolution caused the failure, when attributable.")
        @Nullable String packageName;

        @Column(displayName = "Reason",
                description = "The failure category, e.g. `RESOLUTION_REQUIRED` or `REGISTRY_UNREACHABLE`.")
        String reason;

        @Column(displayName = "Detail",
                description = "Human-readable detail of the failure.")
        String detail;
    }
}
