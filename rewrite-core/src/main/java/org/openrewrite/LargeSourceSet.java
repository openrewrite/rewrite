/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A source set that may be too large to be materialized in memory.
 * It contains operations for filtering and mapping that are optimized
 * for large repositories, though the same operations work on small repositories.
 * <p>
 * A large source set must always track of its initial state to be
 * able to produce {@link #getChangeset()} from that initial state
 * through any number of transformations to some end state.
 */
public interface LargeSourceSet {

    /**
     * Called by {@link RecipeScheduler} at the beginning of a scan/generate/edit cycle.
     *
     * @deprecated Override {@link #beforeCycle(boolean)} instead
     */
    @Deprecated
    default void beforeCycle() {
    }

    /**
     * Called by {@link RecipeScheduler} at the beginning of a scan/generate/edit cycle.
     *
     * @param definitelyLastCycle {@code true} if this is definitely the last cycle of a recipe; when called with a
     *                            value of {@code false} the next call to {@link #afterCycle(boolean)} could still be
     *                            called with {@code true}. This depends on whether any of the recipes requested an
     *                            additional cycle.
     */
    default void beforeCycle(boolean definitelyLastCycle) {
        beforeCycle();
    }

    /**
     * Maintain context about what recipe is performing an edit or generating code.
     *
     * @param recipeStack A stack rooted at the currently operating recipe and extending up its containing recipes
     *                    to top-level recipe that a developer is running directly.
     */
    void setRecipe(List<Recipe> recipeStack);

    /**
     * Execute a transformation on all items.
     *
     * @param map A transformation on T
     * @return A new source set if the map function results in any changes, otherwise this source set is returned.
     */
    LargeSourceSet edit(UnaryOperator<SourceFile> map);

    /**
     * Concatenate new items. Where possible, implementations should not iterate the entire source set in order
     * to accomplish this, since the ordering of {@link SourceFile} is not significant.
     *
     * @param ls The new item to insert
     * @return A new source set with the new item inserted.
     */
    LargeSourceSet generate(@Nullable Collection<? extends SourceFile> ls);

    /**
     * Called by {@link RecipeScheduler} at the conclusion of a scan/generate/edit cycle.
     */
    default void afterCycle(boolean lastCycle) {
    }

    /**
     * @return The set of changes (encompassing adds, edits, and deletions)
     * to the initial state.
     */
    Changeset getChangeset();

    /**
     * Get the original source file, before any edits. Returns null if it is not present.
     *
     * @param sourcePath The path of the source file to retrieve.
     * @return The original source file. Null if not present.
     */
    @Nullable
    SourceFile getBefore(Path sourcePath);
}
