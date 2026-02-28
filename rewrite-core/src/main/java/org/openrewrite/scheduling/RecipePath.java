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
 * A persistent linked-list representing a path from the root recipe to a descendant.
 * Each child holds a reference to its parent, so creating a child path is O(1)
 * instead of copying the entire path.
 */
class RecipePath extends AbstractList<Recipe> {
    private final Recipe recipe;
    private final @Nullable RecipePath parent;
    private final int size;

    public RecipePath(Recipe recipe) {
        this(recipe, null);
    }

    private RecipePath(Recipe recipe, @Nullable RecipePath parent) {
        this.recipe = recipe;
        this.parent = parent;
        this.size = (parent == null) ? 1 : parent.size + 1;
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
        // Walk back from this node to find the element at the given index
        RecipePath current = this;
        for (int i = size - 1; i > index; i--) {
            assert current.parent != null;
            current = current.parent;
        }
        return current.recipe;
    }

    Recipe leaf() {
        return recipe;
    }

    RecipePath child(Recipe childRecipe) {
        return new RecipePath(childRecipe, this);
    }
}
