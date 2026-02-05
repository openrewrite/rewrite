package org.openrewrite;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.net.URI;

@Value
@EqualsAndHashCode(callSuper = false)
public class RecipeNotFoundException extends RuntimeException {
    String recipeName;

    @Nullable
    URI source;

    public RecipeNotFoundException(String recipeName) {
        this(recipeName, null);
    }

    public RecipeNotFoundException(String recipeName, @Nullable URI source) {
        super("Unable to find recipe " + recipeName + (source == null ? "" : " listed in " + source));
        this.recipeName = recipeName;
        this.source = source;
    }
}
