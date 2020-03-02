/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

public abstract class SourceVisitor<R> {
    private static final boolean IS_DEBUGGING = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

    private final ThreadLocal<List<SourceVisitor<R>>> andThen = new ThreadLocal<>();
    private final ThreadLocal<Cursor> cursor = new ThreadLocal<>();

    protected volatile int cycle = 0;

    public SourceVisitor() {
        andThen.set(new ArrayList<>());
    }

    /**
     * Used to build up pipelines of visitors.
     *
     * @return Other visitors that are run after this one.
     */
    public List<SourceVisitor<R>> andThen() {
        return andThen.get();
    }

    public void andThen(SourceVisitor<R> visitor) {
        andThen.get().add(visitor);
    }

    /**
     * Determines whether this visitor can be run multiple times as a top-level rule.
     * In the case of a visitor which mutates the underlying tree, indicates that running once or
     * N times will yield the same mutation.
     */
    public boolean isIdempotent() {
        return true;
    }

    public boolean isCursored() {
        return IS_DEBUGGING;
    }

    public Cursor getCursor() {
        if (cursor.get() == null) {
            throw new IllegalStateException("Cursoring is not enabled for this visitor. " +
                    "Override isCursored() and return true to enable.");
        }
        return cursor.get();
    }

    public void nextCycle() {
        synchronized (this) {
            cycle++;
        }
        andThen.get().clear();
    }

    @Nullable
    public String getName() {
        return null;
    }

    public abstract R defaultTo(@Nullable Tree t);

    /**
     * Some sensible defaults for reduce (boolean OR, list concatenation, or else just the value of r1).
     * Override if your particular visitor needs to reduce values in a different way.
     */
    @SuppressWarnings({"unchecked", "RedundantCast"})
    public R reduce(R r1, R r2) {
        if (r1 instanceof Boolean) {
            return (R) (Boolean) ((Boolean) r1 || (Boolean) r2);
        }
        if (r1 instanceof Set) {
            return (R) Stream.concat(
                    stream(((Iterable<?>) r1).spliterator(), false),
                    stream(((Iterable<?>) r2).spliterator(), false)
            ).collect(Collectors.toSet());
        }
        if (r1 instanceof Collection) {
            return (R) Stream.concat(
                    stream(((Iterable<?>) r1).spliterator(), false),
                    stream(((Iterable<?>) r2).spliterator(), false)
            ).collect(toList());
        }
        return r1 == null ? r2 : r1;
    }

    public final R visit(@Nullable Tree tree) {
        if (tree == null) {
            return defaultTo(null);
        }

        if (isCursored()) {
            cursor.set(new Cursor(cursor.get(), tree));
        }

        R t = reduce(tree.accept(this), visitTree(tree));

        if (isCursored()) {
            cursor.set(cursor.get().getParent());
        }

        return t;
    }

    public R visit(@Nullable List<? extends Tree> trees) {
        R r = defaultTo(null);
        if (trees != null) {
            for (Tree tree : trees) {
                if (tree != null) {
                    r = reduce(visit(tree), r);
                }
            }
        }
        return r;
    }

    public R visitTree(Tree tree) {
        return defaultTo(tree);
    }

    public final R visitAfter(R r, @Nullable Tree tree) {
        return tree == null ? r : reduce(r, visit(tree));
    }

    public final R visitAfter(R r, @Nullable List<? extends Tree> trees) {
        return reduce(r, visit(trees));
    }
}
