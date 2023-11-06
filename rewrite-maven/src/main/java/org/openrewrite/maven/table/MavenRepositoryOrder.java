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

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class MavenRepositoryOrder extends DataTable<MavenRepositoryOrder.Row> {

    public MavenRepositoryOrder(Recipe recipe) {
        super(recipe, MavenRepositoryOrder.Row.class,
                MavenRepositoryOrder.class.getName(),
                "Maven repository order",
                "The order in which dependencies will be resolved for each `pom.xml` based on its defined repositories and effective `settings.xml`.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Repository ID",
                description = "The ID of the repository. Note that projects may define the same physical repository with different IDs.")
        String id;

        @Column(displayName = "Repository URI",
                description = "The URI of the repository.")
        String uri;

        @Column(displayName = "Known to exist",
                description = "If the repository is provably reachable. If false, does not guarantee that the repository does not exist.")
        boolean knownToExist;

        @Column(displayName = "Rank",
                description = "The index order of this repository in the repository list.")
        int rank;
    }
}
