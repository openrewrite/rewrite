/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scheduling;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Recipe;

import java.util.AbstractList;

/**
 * A persistent, immutable path from the root recipe down to a descendant recipe.
 * <p>
 * Each node references its {@code parent}, so deriving a child path via {@link #child(Recipe)}
 * is O(1) and shares the entire ancestor chain instead of copying it. This replaces the
 * previous approach of copying a {@link java.util.Stack} for every node visited during a
 * recipe-tree traversal, which is rebuilt identically for every source file and every cycle.
 * <p>
 * Because it implements {@link java.util.List List&lt;Recipe&gt;} (ordered root-first, leaf-last),
 * it drops directly into consumers expecting the recipe path as a list, with no copying.
 */
class RecipePath extends AbstractList<Recipe> {
    private final Recipe recipe;
    private final @Nullable RecipePath parent;
    private final int size;

    RecipePath(Recipe recipe) {
        this(recipe, null);
    }

    private RecipePath(Recipe recipe, @Nullable RecipePath parent) {
        this.recipe = recipe;
        this.parent = parent;
        this.size = parent == null ? 1 : parent.size + 1;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Recipe get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        // Walk back from the leaf to the requested index.
        RecipePath current = this;
        for (int i = size - 1; i > index; i--) {
            assert current.parent != null;
            current = current.parent;
        }
        return current.recipe;
    }

    /**
     * @return The recipe at the tip of this path (the deepest descendant).
     */
    Recipe leaf() {
        return recipe;
    }

    /**
     * @return A new path with {@code childRecipe} appended as the leaf, sharing this path as its
     * ancestor chain. This is O(1) and performs no copying.
     */
    RecipePath child(Recipe childRecipe) {
        return new RecipePath(childRecipe, this);
    }
}
