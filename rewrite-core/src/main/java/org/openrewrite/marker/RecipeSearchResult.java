/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.marker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.Incubating;
import org.openrewrite.Recipe;
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.lang.Nullable;

import java.util.UUID;

/**
 * Used by search visitors to mark AST elements that match the search criteria. By marking AST elements in a tree,
 * search results can be contextualized in the tree that they are found in.
 */
@Incubating(since = "7.0.0")
@Data
@AllArgsConstructor
@EqualsAndHashCode
public class RecipeSearchResult implements SearchResult {
    @EqualsAndHashCode.Exclude
    private final UUID id;

    private final Recipe recipe;

    @Nullable
    private final String description;

    public RecipeSearchResult(UUID id, Recipe recipe) {
        this(id, recipe, null);
    }

    @Override
    public <P> String print(TreePrinter<P> printer, P p) {
        if (getDescription() == null) {
            return "~~>";
        } else {
            return String.format("~~(%s)~~>", getDescription());
        }
    }
}
