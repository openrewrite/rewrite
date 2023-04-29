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
 * A large iterable is an iterable that may be too large
 * to be materialized in memory. It contains operations for
 * filtering and mapping that are optimized for large data sets.
 * <br/>
 * Ordering is not guaranteed.
 *
 * @param <T> The type of the elements in the iterable.
 */
public interface LargeIterable<T> extends Iterable<T> {

    /**
     * Execute a transformation on all items. This causes the iterable to be iterated and a new
     * iterable returned if any changes are made.
     *
     * @param map A transformation on T
     * @return A new iterable if the map function results in any changes, otherwise this iterable is returned.
     */
    LargeIterable<T> map(BiFunction<Integer, T, T> map);

    /**
     * Execute a transformation on all items. This causes the iterable to be iterated and a new
     * iterable returned if any changes are made.
     *
     * @param map A transformation on T
     * @return A new iterable if the map function results in any changes, otherwise this iterable is returned.
     */
    default LargeIterable<T> map(UnaryOperator<T> map) {
        return map((i, t) -> map.apply(t));
    }

    /**
     * Execute a transformation on all items. This causes the iterable to be iterated and a new
     * iterable returned if any changes are made.
     *
     * @param flatMap A transformation on T that may return [0..N] items for each original item in the iterable.
     * @return A new iterable if the map function results in any changes, otherwise this iterable is returned.
     */
    LargeIterable<T> flatMap(BiFunction<Integer, T, Object> flatMap);

    /**
     * Concatenate a new item. Where possible, implementations should not iterate the entire iterable in order
     * to accomplish this, since the ordering of a {@link LargeIterable} is not significant.
     *
     * @param t The new item to insert
     * @return A new iterable with the new item inserted.
     */
    LargeIterable<T> concat(@Nullable T t);

    /**
     * Concatenate new items. Where possible, implementations should not iterate the entire iterable in order
     * to accomplish this, since the ordering of a {@link LargeIterable} is not significant.
     *
     * @param ls The new item to insert
     * @return A new iterable with the new item inserted.
     */
    LargeIterable<T> concatAll(@Nullable Collection<? extends T> ls);
}
