package org.openrewrite.marker;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.Incubating;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;

import java.util.UUID;

/**
 * Used by search visitors to mark AST elements that match the search criteria. By marking AST elements in a tree,
 * search results can be contextualized in the tree that they are found in.
 */
@Incubating(since = "7.0.0")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecipeSearchResult implements SearchResult {
    @EqualsAndHashCode.Include
    private final UUID id;

    private final Recipe recipe;

    @Nullable
    private final String description;
}
