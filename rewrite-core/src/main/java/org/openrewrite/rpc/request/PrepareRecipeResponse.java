package org.openrewrite.rpc.request;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.config.RecipeDescriptor;

@Value
public class PrepareRecipeResponse {
    /**
     * The ID that the remote is using to refer to a
     * specific instance of the recipe.
     */
    String id;

    RecipeDescriptor descriptor;
    String editVisitor;

    @Nullable
    String scanVisitor;
}
