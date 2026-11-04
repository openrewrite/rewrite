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
package org.openrewrite.golang.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.NonNull;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class GoDependenciesInUse extends DataTable<GoDependenciesInUse.@NonNull Row> {

    public GoDependenciesInUse(Recipe recipe) {
        super(recipe, "Go dependencies in use", "Direct and transitive dependencies in use in Go modules.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Module path",
                description = "The module path of the project that contains the dependency (from the `module` directive in go.mod).")
        String modulePath;

        @Column(displayName = "Project path",
                description = "The path to the go.mod file.")
        String projectPath;

        @Column(displayName = "Dependency module",
                description = "The module path of the Go dependency.")
        String dependencyModule;

        @Column(displayName = "Version",
                description = "The resolved version of the dependency.")
        @Nullable
        String version;

        @Column(displayName = "Version constraint",
                description = "The version as declared in the `require` directive in go.mod.")
        @Nullable
        String versionConstraint;

        @Column(displayName = "Direct",
                description = "Whether this is a direct dependency (true) or a transitive dependency (false).")
        Boolean direct;

        @Column(displayName = "Count",
                description = "How many times this dependency appears in the dependency graph.")
        Integer count;
    }
}
