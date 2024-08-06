/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class ClasspathTypeCount extends DataTable<ClasspathTypeCount.Row> {

    public ClasspathTypeCount(Recipe recipe) {
        super(recipe,
                "Classpath type count",
                "The number of types in each source set in a project's classpath.");
    }

    @Value
    public static class Row {

        @Column(displayName = "Project name",
                description = "The name of the (sub)project.")
        String project;

        @Column(displayName = "Source set",
                description = "The source set name.")
        String sourceSet;

        @Column(displayName = "Types",
                description = "The number of types in the source set.")
        int types;
    }
}

