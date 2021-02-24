package org.openrewrite;

/**
 * Non-discoverable Recipe, where you don't care about displayName and description
 */
public class HiddenRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }
}
