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
package org.openrewrite.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class SourcesFileResults extends DataTable<SourcesFileResults.Row> {

    public SourcesFileResults(Recipe recipe) {
        super(recipe, "Source files that had results",
                "Source files that were modified by the recipe run.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path before the run",
                description = "The source path of the file before the run.")
        String sourcePath;

        @Column(displayName = "Source path after the run",
                description = "A recipe may modify the source path. This is the path after the run.")
        String afterSourcePath;

        @Column(displayName = "Recipe that made changes",
                description = "The specific recipe that made a change.")
        String recipe;
    }
}
