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
        //TODO: Question for Jonathan: should we not (similar to SourcesFileErrors) have a afterSourcePath and represent both?
        // What if a recipe changes the path and adds markers? Do we need both?
        // If a later recipe removed the sourcefile, we will not have the markers anymore. Not foreseeing that as an issue as typically search and edit/delete will not go together.
        @Column(displayName = "Source path of search result",
                description = "The source path of the file with the search result markers present.")
        @Nullable
        String sourcePath;

        //TODO: Question for Jonathan: what should the truncation length be?
        @Column(displayName = "Result",
                description = "The trimmed printed tree of the LST element that the marker is attached to. Truncated after 1000 chars if longer.")
        String result;

        //TODO: Question for Jonathan: Should we not report both instance name and recipe name? Searching for the recipe id from the String is not always "easy" (see also the unit tests)
        @Column(displayName = "Parent of the recipe that had the search marker added",
                description = "In a hierarchical recipe, the parent of the recipe that made a change. Empty if " +
                        "this is the root of a hierarchy or if the recipe is not hierarchical at all.")
        String parentRecipe;

        @Column(displayName = "Recipe that added the search marker",
                description = "The specific recipe that added the Search marker.")
        String recipe;

    }
    //TODO: Question for Jonathan: in `SourcesFileResults`, I see a `build` method which is not used in the OR codebase. Perhaps by our other system? Do we need to have this build method or not? `SourcesFileErrors` does not have it.
}
