/*
 * Copyright 2023 the original author or authors.
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
public class ParentPomsInUse extends DataTable<ParentPomsInUse.Row> {

    public ParentPomsInUse(Recipe recipe) {
        super(recipe, Row.class,
                ParentPomsInUse.class.getName(),
                "Maven parent POMs in use", "Projects, GAVs and relativePaths for Maven parent POMs in use.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Project artifactId",
                description = "The artifactId of the project that contains the parent.")
        String projectArtifactId;

        @Column(displayName = "Group",
                description = "The first part of a parent coordinate `org.springframework.boot`.")
        String groupId;

        @Column(displayName = "Artifact",
                description = "The second part of a parent coordinate `spring-boot-starter-*`.")
        String artifactId;

        @Column(displayName = "Version",
                description = "The resolved version.")
        @Nullable
        String version;

        @Column(displayName = "Relative path",
                description = "The relative path to the parent.")
        @Nullable
        String relativePath;
    }
}
