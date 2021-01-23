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

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;

@EqualsAndHashCode(exclude = "messages")
public class Cursor {
    @Nullable
    private final Cursor parent;

    private final Object value;

    @Nullable
    private Map<String, Object> messages;

    public Cursor(@Nullable Cursor parent, Object value) {
        this.parent = parent;
        this.value = value;
    }

    public Cursor getRoot() {
        Cursor c = this;
        while (c.parent != null) {
            c = c.parent;
        }
        return c;
    }

    public Iterator<Object> getPath() {
        return new CursorIterator(this);
    }

    public Stream<Object> getPathAsStream() {
        return stream(Spliterators.spliteratorUnknownSize(getPath(), 0), false);
    }

    private static class CursorIterator implements Iterator<Object> {
        private Cursor cursor;

        private CursorIterator(Cursor cursor) {
            this.cursor = cursor;
        }

        @Override
        public boolean hasNext() {
            return cursor != null;
        }

        @Override
        public Object next() {
            Object v = cursor.value;
            cursor = cursor.parent;
            return v;
        }
    }

    @Nullable
    public <T> T firstEnclosing(Class<T> tClass) {
        CursorIterator iter = new CursorIterator(this);
        while (iter.hasNext()) {
            Object value = iter.next();
            if (tClass.isInstance(value)) {
                //noinspection unchecked
                return (T) value;
            }
        }
        return null;
    }

    public <T> T firstEnclosingOrThrow(Class<T> tClass) {
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

    public Cursor dropParentUntil(Predicate<Object> valuePredicate) {
        Cursor cursor = parent;
        while(cursor != null && !valuePredicate.test(cursor.value)) {
            cursor = cursor.parent;
        }
        if (cursor == null) {
            throw new IllegalStateException("Expected to find a matching parent for " + this);
        }
        return cursor;
    }

    public Cursor dropParentWhile(Predicate<Object> valuePredicate) {
        Cursor cursor = parent;
        while(cursor != null && valuePredicate.test(cursor.value)) {
            cursor = cursor.parent;
        }
        if (cursor == null) {
            throw new IllegalStateException("Expected to find a matching parent for " + this);
        }
        return cursor;
    }

    @Incubating(since = "7.0.0")
    @Nullable
    public Cursor getParent(int levels) {
        Cursor cursor = this;
        for (int i = 0; i < levels && cursor != null; i++) {
            cursor = cursor.parent;
        }
        return cursor;
    }

    @Nullable
    public Cursor getParent() {
        return getParent(1);
    }

    @Incubating(since = "7.0.0")
    public Cursor getParentOrThrow(int levels) {
        Cursor parent = getParent(levels);
        if (parent == null) {
            throw new IllegalStateException("Expected to find a parent for " + this);
        }
        return parent;
    }

    public Cursor getParentOrThrow() {
        return getParentOrThrow(1);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) value;
    }

    public boolean isScopeInPath(Tree scope) {
        return value instanceof Tree && ((Tree) value).getId().equals(scope.getId()) ||
                getPathAsStream().anyMatch(p -> p instanceof Tree && ((Tree) p).getId().equals(scope.getId()));
    }

    @Incubating(since = "7.0.0")
    public void putMessageOnFirstEnclosing(Class<?> enclosing, String key, Object value) {
        if (enclosing.isInstance(this)) {
            putMessage(key, value);
        } else if (parent != null) {
            parent.putMessageOnFirstEnclosing(enclosing, key, value);
        }
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
