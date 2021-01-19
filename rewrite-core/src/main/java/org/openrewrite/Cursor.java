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

import lombok.EqualsAndHashCode;
import org.openrewrite.internal.lang.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;

@EqualsAndHashCode
public class Cursor {
    @Nullable
    private final Cursor parent;

    private final Tree tree;

    @Nullable
    private Map<String, Object> messages;

    public Cursor(@Nullable Cursor parent, Tree tree) {
        this.parent = parent;
        this.tree = tree;
    }

    public Iterator<Tree> getPath() {
        return new CursorIterator(this);
    }

    public Stream<Tree> getPathAsStream() {
        return stream(Spliterators.spliteratorUnknownSize(getPath(), 0), false);
    }

    private static class CursorIterator implements Iterator<Tree> {
        private Cursor cursor;

        private CursorIterator(Cursor cursor) {
            this.cursor = cursor;
        }

        @Override
        public boolean hasNext() {
            return cursor != null;
        }

        @Override
        public Tree next() {
            Tree t = cursor.tree;
            cursor = cursor.parent;
            return t;
        }
    }

    @Nullable
    public <T extends Tree> T firstEnclosing(Class<T> tClass) {
        CursorIterator iter = new CursorIterator(this);
        while (iter.hasNext()) {
            Tree t = iter.next();
            if (tClass.isInstance(t)) {
                //noinspection unchecked
                return (T) t;
            }
        }
        return null;
    }

    public <T extends Tree> T firstEnclosingRequired(Class<T> tClass) {
        T firstEnclosing = firstEnclosing(tClass);
        if (firstEnclosing == null) {
            throw new IllegalStateException("Expected to find enclosing " + tClass.getSimpleName());
        }
        return firstEnclosing;
    }

    @Override
    public String toString() {
        return "Cursor{" +
                stream(Spliterators.spliteratorUnknownSize(getPath(), 0), false)
                        .map(t -> t.getClass().getSimpleName())
                        .collect(Collectors.joining("->"))
                + "}";
    }

    @Nullable
    public Cursor getParent() {
        return parent;
    }

    public Cursor getParentOrThrow() {
        if (parent == null) {
            throw new IllegalStateException("Expected to find a parent for " + this);
        }
        return parent;
    }

    @SuppressWarnings("unchecked")
    public <T extends Tree> T getTree() {
        return (T) tree;
    }

    public boolean isScopeInPath(Tree scope) {
        return (tree != null && tree.getId().equals(scope.getId())) || getPathAsStream().anyMatch(p -> p.getId().equals(scope.getId()));
    }

    @Incubating(since = "7.0.0")
    public void putMessage(String key, Object value) {
        if (messages == null) {
            messages = new HashMap<>();
        }
        messages.put(key, value);
    }

    /**
     * Finds the closest message matching the provided key, leaving it in the message map for further access.
     *
     * @param key The message key to find.
     * @param <T> The expected value of the message.
     * @return The closest message matching the provided key in the cursor stack, or <code>null</code> if none.
     */
    @Incubating(since = "7.0.0")
    @Nullable
    public <T> T peekMessage(String key) {
        @SuppressWarnings("unchecked") T t = messages == null ? null : (T) messages.get(key);
        return t == null && parent != null ? parent.peekMessage(key) : t;
    }

    /**
     * Finds the closest message matching the provided key, removing it from the message map.
     *
     * @param key The message key to find.
     * @param <T> The expected value of the message.
     * @return The closest message matching the provided key in the cursor stack, or <code>null</code> if none.
     */
    @Incubating(since = "7.0.0")
    @Nullable
    public <T> T pollMessage(String key) {
        @SuppressWarnings("unchecked") T t = messages == null ? null : (T) messages.remove(key);
        return t == null && parent != null ? parent.pollMessage(key) : t;
    }
}
