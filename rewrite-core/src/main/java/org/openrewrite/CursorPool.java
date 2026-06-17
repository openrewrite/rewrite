/*
 * Copyright 2024 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A per-thread free list of {@link Cursor} objects, used to recycle the cursor that the visitor
 * creates for every node it descends into instead of allocating a fresh one each time. Allocation
 * profiling of large recipe runs showed {@code new Cursor(...)} accounting for roughly 40% of all
 * allocation — pure churn (one cursor per node per pass), since a cursor is logically dead the
 * instant the visitor ascends back above its node.
 * <p>
 * Recycling is sound only because a cursor that must outlive its visit is copied via
 * {@link Cursor#detach()} into a fresh, never-pooled object, leaving the original free to be reset
 * and reused. {@link org.openrewrite.internal.CursorEscapeDetector} is the gate that proves no
 * un-detached cursor is actually retained past the visit that created it.
 * <p>
 * The pool is per-thread because recipe runs are parallel across source files but a single node's
 * visit (acquire through release) always happens on one thread, so a thread only ever touches its
 * own free list and no synchronization is needed.
 * <p>
 * Entirely disabled (every cursor is freshly allocated) unless {@code -Dorg.openrewrite.cursorPool=true}.
 */
public final class CursorPool {

    public static final boolean ENABLED = Boolean.getBoolean("org.openrewrite.cursorPool");

    private static final ThreadLocal<Deque<Cursor>> FREE = ThreadLocal.withInitial(ArrayDeque::new);

    private CursorPool() {
    }

    /**
     * Reuse a recycled cursor for the given position if one is available on this thread, otherwise
     * allocate a new one.
     */
    public static Cursor acquire(@Nullable Cursor parent, Object value) {
        Cursor cursor = FREE.get().poll();
        if (cursor == null) {
            return new Cursor(parent, value);
        }
        cursor.reset(parent, value);
        return cursor;
    }

    /**
     * Return a cursor to this thread's free list once the visitor has ascended above its node.
     * Root cursors are never recycled: they are held for the lifetime of a recipe run (by the
     * {@code RecipeScheduler} / {@code RecipeRunCycle} / prepared-recipe cache) and resetting one
     * would corrupt that long-lived state.
     */
    public static void release(Cursor cursor) {
        if (cursor.getParent() == null || cursor.isRoot()) {
            return;
        }
        FREE.get().push(cursor);
    }

    /**
     * Discard this thread's recycled cursors. Intended for tests that want an isolated pool; the
     * pool is otherwise meant to persist on long-lived worker threads across many source files.
     */
    public static void clear() {
        FREE.remove();
    }
}
