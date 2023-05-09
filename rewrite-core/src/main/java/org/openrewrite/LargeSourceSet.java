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

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * A source set that may be too large to be materialized in memory.
 * It contains operations for filtering and mapping that are optimized
 * for large repositories, though the same operations work on small repositories.
 * <br/>
 * Ordering is not guaranteed.
 * <br/>
 * A large source set must always track of its initial state to be
 * able to produce {@link #getChangeset()} from that initial state
 * through any number of transformations to some end state.
 */
public interface LargeSourceSet extends Iterable<SourceFile> {

    /**
     * Execute a transformation on all items. This causes the iterable to be iterated and a new
     * iterable returned if any changes are made.
     *
     * @param map A transformation on T
     * @return A new source set if the map function results in any changes, otherwise this source set is returned.
     */
    LargeSourceSet map(UnaryOperator<SourceFile> map);

    /**
     * Execute a transformation on all items. This causes the iterable to be iterated and a new
     * iterable returned if any changes are made.
     *
     * @param flatMap A transformation on T that may return [0..N] items for each original item in the iterable.
     * @return A new source set if the map function results in any changes, otherwise this source set is returned.
     */
    LargeSourceSet flatMap(BiFunction<Integer, SourceFile, Object> flatMap);

    /**
     * Concatenate a new item. Where possible, implementations should not iterate the entire iterable in order
     * to accomplish this, since the ordering of a {@link LargeSourceSet} is not significant.
     *
     * @return A new source set with the new item inserted.
     */
    LargeSourceSet concat(@Nullable SourceFile sourceFile);

    /**
     * Concatenate new items. Where possible, implementations should not iterate the entire iterable in order
     * to accomplish this, since the ordering of a {@link SourceFile} is not significant.
     *
     * @param ls The new item to insert
     * @return A new source set with the new item inserted.
     */
    LargeSourceSet concatAll(@Nullable Collection<? extends SourceFile> ls);

    /**
     * @return The initial state of the first incarnation of this large iterable.
     * It may have passed through one or several transformations in the meantime.
     */
    LargeSourceSet getInitialState();

    /**
     * @return The set of changes (encompassing adds, edits, and deletions)
     * to the initial state.
     */
    Changeset getChangeset();

    /**
     * Called when a source file is deleted from the source set.
     *
     * @param sourceFile The source file that is deleted.
     */
    void onDelete(SourceFile sourceFile, List<Recipe> recipeStack);
}
