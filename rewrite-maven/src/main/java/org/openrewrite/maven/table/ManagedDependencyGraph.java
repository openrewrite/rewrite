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

public class ManagedDependencyGraph extends DataTable<ManagedDependencyGraph.Row> {

    public ManagedDependencyGraph(Recipe recipe) {
        super(recipe, ManagedDependencyGraph.Row.class,
                DependenciesInUse.class.getName(),
                "Managed dependency graph",
                "Relationships between POMs and their ancestors that define managed dependencies.");
    }

    @Value
    public static class Row {
        @Column(displayName = "From dependency",
                description = "What depends on the 'to' dependency.")
        String from;

        @Column(displayName = "From dependency",
                description = "A dependency.")
        String to;
    }

    public enum Relationship {
        Parent,
        ManagedDependency
    }
}
