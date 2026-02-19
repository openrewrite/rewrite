package org.openrewrite.config;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Indicates that a recipe exposes a list of recipes used as preconditions.
 * This is purely informational and not taken into consideration by recipe execution.
 * Imperative recipes interact with preconditions as implementation details of their visitor(s).
 * There is no reason for an imperative recipe to implement this, it will not affect the behavior of the recipe in any way.
 *
 */
public interface RecipePreconditions {
    List<Recipe> getPreconditions();
}
