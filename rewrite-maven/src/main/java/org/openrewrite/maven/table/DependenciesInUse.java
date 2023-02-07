/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.maven.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;

@JsonIgnoreType
public class DependenciesInUse extends DataTable<DependenciesInUse.Row> {

    public DependenciesInUse(Recipe recipe) {
        super(recipe, Row.class,
                DependenciesInUse.class.getName(),
                "Dependencies in use", "Direct and transitive dependencies in use.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Project name",
                description = "The name of the project that contains the dependency.")
        String projectName;

        @Column(displayName = "Source set",
                description = "The source set that contains the dependency.")
        String sourceSet;

        @Column(displayName = "Group",
                description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.")
        String groupId;

        @Column(displayName = "Artifact",
                description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`.")
        String artifactId;

        @Column(displayName = "Version",
                description = "The resolved version.")
        String version;

        @Column(displayName = "Dated snapshot version",
                description = "The resolved dated snapshot version or null if this dependency is not a snapshot.")
        @Nullable
        String datedSnapshotVersion;

        @Column(displayName = "Scope",
                description = "Dependency scope. This will be `compile` if the dependency is direct and a scope is not explicitly " +
                              "specified in the POM.")
        String scope;

        @Column(displayName = "Depth",
                description = "How many levels removed from a direct dependency. This will be 0 for direct dependencies.")
        Integer depth;
    }
}
