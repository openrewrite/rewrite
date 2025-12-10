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
import org.jspecify.annotations.Nullable;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class SearchResults extends DataTable<SearchResults.Row> {

    public SearchResults(Recipe recipe) {
        super(recipe, "Source files that had search results",
                "Search results that were found during the recipe run.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path of search result before the run",
                description = "The source path of the file with the search result markers present.")
        @Nullable
        String sourcePath;

        @Column(displayName = "Source path of search result after run the run",
                description = "A recipe may modify the source path. This is the path after the run. `null` when a source file was deleted during the run.")
        @Nullable
        String afterSourcePath;

        @Column(displayName = "Result",
                description = "The trimmed printed tree of the LST element that the marker is attached to.")
        String result;

        @Column(displayName = "Recipe that added the search marker",
                description = "The specific recipe that added the Search marker.")
        String recipe;
    }
}
