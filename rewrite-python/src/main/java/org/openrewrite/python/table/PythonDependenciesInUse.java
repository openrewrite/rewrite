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
public class PythonDependenciesInUse extends DataTable<PythonDependenciesInUse.@NonNull Row> {

    public PythonDependenciesInUse(Recipe recipe) {
        super(recipe, "Python dependencies in use", "Direct and transitive dependencies in use in Python projects.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Project name",
                description = "The name of the project that contains the dependency (from pyproject.toml).")
        @Nullable
        String projectName;

        @Column(displayName = "Project path",
                description = "The path to the project.")
        String projectPath;

        @Column(displayName = "Package name",
                description = "The name of the Python package.")
        String packageName;

        @Column(displayName = "Version",
                description = "The resolved version of the package.")
        @Nullable
        String version;

        @Column(displayName = "Version constraint",
                description = "The version constraint as declared in pyproject.toml.")
        @Nullable
        String versionConstraint;

        @Column(displayName = "Scope",
                description = "Dependency scope: dependencies, buildRequires, optionalDependencies, or dependencyGroups.")
        String scope;

        @Column(displayName = "Direct",
                description = "Whether this is a direct dependency (true) or transitive dependency (false).")
        Boolean direct;

        @Column(displayName = "Count",
                description = "How many times this dependency appears in the dependency tree.")
        Integer count;

        @Column(displayName = "License",
                description = "The SPDX license identifier of the project, if available.")
        @Nullable
        String license;
    }
}
