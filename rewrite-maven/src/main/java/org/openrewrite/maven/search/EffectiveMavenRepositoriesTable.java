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
package org.openrewrite.maven.search;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class EffectiveMavenRepositoriesTable extends DataTable<EffectiveMavenRepositoriesTable.Row> {

    public EffectiveMavenRepositoriesTable(Recipe recipe) {
        super(recipe,
                "Effective Maven repositories",
                "Table showing which Maven repositories were used in dependency resolution for this POM.");
    }

    @Value
    public static class Row {
        @Column(displayName = "POM path",
                description = "The path to the POM file.")
        String pomPath;

        @Column(displayName = "Repository URI",
                description = "The URI of the Maven repository.")
        String repositoryUri;
    }
}
