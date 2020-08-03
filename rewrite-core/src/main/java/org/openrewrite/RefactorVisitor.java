/*
 * Copyright 2020 the original author or authors.
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

import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

public interface RefactorVisitor<T extends Tree> extends SourceVisitor<T> {
    @SuppressWarnings("unchecked")
    @Override
    default T defaultTo(Tree t) {
        return (T) t;
    }

    /**
     * @return Other visitors that are run after this one.
     */
    default List<RefactorVisitor<T>> andThen() {
        return emptyList();
    }

    /**
     * Determines whether this visitor can be run multiple times as a top-level rule.
     * In the case of a visitor which mutates the underlying tree, indicates that running once or
     * N times will yield the same mutation.
     *
     * @return If true, this visitor can be run multiple times.
     */
    default boolean isIdempotent() {
        return true;
    }

    default Collection<T> generate() {
        return emptyList();
    }

    /**
     * Prepare to visit the next top-level tree.
     */
    default void next() {
    }
}
