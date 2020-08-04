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

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.internal.lang.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

public interface SourceVisitor<R> {
    default Cursor getCursor() {
        throw new IllegalStateException("Cursoring is not enabled for this visitor");
    }

    default Iterable<Tag> getTags() {
        return Tags.empty();
    }

    default Validated validate() {
        return Validated.none();
    }

    default String getName() {
        return getClass().getName();
    }

    R defaultTo(@Nullable Tree t);

    /**
     * Some sensible defaults for reduce (boolean OR, list concatenation, or else just the value of r1).
     * Override if your particular visitor needs to reduce values in a different way.
     *
     * @param r1 The left side to reduce.
     * @param r2 The right side to reduce.
     * @return The reduced value.
     */
    @SuppressWarnings("unchecked")
    default R reduce(R r1, R r2) {
        if (r1 instanceof Boolean) {
            return (R) (Boolean) ((Boolean) r1 || (Boolean) r2);
        }
        else if (r1 instanceof String) {
            return (R) (r1.toString() + (r2 == null ? "" : r2.toString()));
        }
        else if (r1 instanceof Set) {
            return (R) Stream.concat(
                    stream(((Iterable<?>) r1).spliterator(), false),
                    stream(((Iterable<?>) r2).spliterator(), false)
            ).collect(Collectors.toSet());
        }
        else if (r1 instanceof Collection) {
            return (R) Stream.concat(
                    stream(((Iterable<?>) r1).spliterator(), false),
                    stream(((Iterable<?>) r2).spliterator(), false)
            ).collect(toList());
        }
        else {
            return r1 == null ? r2 : r1;
        }
    }

    R visit(@Nullable Tree tree);

    default R visit(@Nullable List<? extends Tree> trees) {
        R r = defaultTo(null);
        if (trees != null) {
            for (Tree tree : trees) {
                if (tree != null) {
                    r = reduce(r, visit(tree));
                }
            }
        }
        return r;
    }

    default R visitTree(Tree tree) {
        return defaultTo(tree);
    }
}
