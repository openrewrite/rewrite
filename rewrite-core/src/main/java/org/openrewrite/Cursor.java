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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;

@EqualsAndHashCode(exclude = "messages")
public class Cursor {
    public static final String ROOT_VALUE = "root";

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

    public Iterator<Cursor> getPathAsCursors() {
        return new CursorPathIterator(this);
    }

    public Iterator<Cursor> getPathAsCursors(Predicate<Cursor> filter) {
        return new CursorPathIterator(this, filter);
    }

    public Iterator<Object> getPath() {
        return new CursorIterator(this);
    }

    public Iterator<Object> getPath(Predicate<Object> filter) {
        return new CursorIterator(this, filter);
    }

    public Stream<Object> getPathAsStream() {
        return stream(Spliterators.spliteratorUnknownSize(getPath(), 0), false);
    }

    public Stream<Object> getPathAsStream(Predicate<Object> filter) {
        return stream(Spliterators.spliteratorUnknownSize(getPath(filter),
                Spliterator.IMMUTABLE), false);
    }

    private static class CursorPathIterator implements Iterator<Cursor> {

        private Predicate<Cursor> filter = c -> true;
        private Cursor cursor;

        private CursorPathIterator(Cursor cursor) {
            this.cursor = cursor;
        }

        private CursorPathIterator(Cursor cursor, Predicate<Cursor> filter) {
            this.cursor = cursor;
            this.filter = filter;
        }

        @Override
        public boolean hasNext() {
            for (Cursor c = cursor; c != null; c = c.parent) {
                if (filter.test(c)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Cursor next() {
            for (; cursor != null; cursor = cursor.parent) {
                Cursor c = cursor;
                if (filter.test(c)) {
                    cursor = c.parent;
                    return c;
                }
            }
            throw new NoSuchElementException();
        }
    }

    private static class CursorIterator implements Iterator<Object> {
        private Cursor cursor;

        private Predicate<Object> filter = v -> true;

        private CursorIterator(Cursor cursor) {
            this.cursor = cursor;
        }

        private CursorIterator(Cursor cursor, Predicate<Object> filter) {
            this.cursor = cursor;
            this.filter = filter;
        }

        @Override
        public boolean hasNext() {
            for (Cursor c = cursor; c != null; c = c.parent) {
                if (filter.test(c.value)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Object next() {
            for (; cursor != null; cursor = cursor.parent) {
                Object v = cursor.value;
                if (filter.test(v)) {
                    cursor = cursor.parent;
                    return v;
                }
            }
            throw new NoSuchElementException();
        }
    }

    @Nullable
    public <T> T firstEnclosing(Class<T> tClass) {
        CursorIterator iter = new CursorIterator(this);
        while (iter.hasNext()) {
            Object value = iter.next();
            if (tClass.isAssignableFrom(value.getClass())) {
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
                        .map(t -> t instanceof Tree ?
                                t.getClass().getSimpleName() :
                                t.toString())
                        .collect(Collectors.joining("->"))
                + "}";
    }

    public Cursor dropParentUntil(Predicate<Object> valuePredicate) {
        Cursor cursor = parent;
        while (cursor != null && !valuePredicate.test(cursor.value)) {
            cursor = cursor.parent;
        }
        if (cursor == null) {
            throw new IllegalStateException("Expected to find a matching parent for " + this);
        }
        return cursor;
    }

    public Cursor dropParentWhile(Predicate<Object> valuePredicate) {
        Cursor cursor = parent;
        while (cursor != null && valuePredicate.test(cursor.value)) {
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

    /**
     * Return the first parent of the current cursor which points to an AST element, or the root cursor if the current
     * cursor already points to the root AST element. This skips over non-tree Padding elements.
     * </p>
     * If you do want to access Padding elements, use getParent() or getParentOrThrow(), which do not skip over these elements.
     *
     * @return a cursor which either points at the first non-padding parent of the current element
     */
    public Cursor getParentTreeCursor() {
        return dropParentUntil(it -> it instanceof Tree || it == Cursor.ROOT_VALUE);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) value;
    }

    public boolean isScopeInPath(Tree scope) {
        return value instanceof Tree && ((Tree) value).getId().equals(scope.getId()) ||
                getPathAsStream().anyMatch(p -> p instanceof Tree && ((Tree) p).getId().equals(scope.getId()));
    }

    public void putMessageOnFirstEnclosing(Class<?> enclosing, String key, Object value) {
        if (enclosing.isInstance(this.getValue())) {
            putMessage(key, value);
        } else if (parent != null) {
            parent.putMessageOnFirstEnclosing(enclosing, key, value);
        }
    }

    public void putMessage(String key, Object value) {
        if (messages == null) {
            messages = new HashMap<>();
        }
        messages.put(key, value);
    }

    public <T> T computeMessageIfAbsent(String key, Function<String, ? extends T> mappingFunction) {
        if (messages == null) {
            messages = new HashMap<>();
        }
        @SuppressWarnings("unchecked") T t = (T) messages.computeIfAbsent(key, mappingFunction);
        return t;
    }

    /**
     * Finds the closest message matching the provided key, leaving it in the message map for further access.
     *
     * @param key The message key to find.
     * @param <T> The expected value of the message.
     * @return The closest message matching the provided key in the cursor stack, or <code>null</code> if none.
     */
    @Nullable
    public <T> T getNearestMessage(String key) {
        @SuppressWarnings("unchecked") T t = messages == null ? null : (T) messages.get(key);
        return t == null && parent != null ? parent.getNearestMessage(key) : t;
    }

    public <T> T getNearestMessage(String key, T defaultValue) {
        @SuppressWarnings("unchecked") T t = messages == null ? null : (T) messages.get(key);
        if(t == null) {
            if(parent != null) {
                return parent.getNearestMessage(key, defaultValue);
            }
            return defaultValue;
        }
        return t;
    }

    /**
     * Finds the closest message matching the provided key, removing it from the message map.
     *
     * @param key The message key to find.
     * @param <T> The expected value of the message.
     * @return The closest message matching the provided key in the cursor stack, or <code>null</code> if none.
     */
    @Nullable
    public <T> T pollNearestMessage(String key) {
        @SuppressWarnings("unchecked") T t = messages == null ? null : (T) messages.remove(key);
        return t == null && parent != null ? parent.pollNearestMessage(key) : t;
    }

    /**
     * Finds the closest message matching the provided key, leaving it in the message map for further access.
     *
     * @param key The message key to find.
     * @param <T> The expected value of the message.
     * @return The message matching the provided key, or <code>null</code> if none.
     */
    @Nullable
    public <T> T getMessage(String key) {
        //noinspection unchecked
        return messages == null ? null : (T) messages.get(key);
    }

    public <T> T getMessage(String key, T defaultValue) {
        //noinspection unchecked
        return messages == null ? defaultValue : (T) messages.getOrDefault(key, defaultValue);
    }

    /**
     * Finds the message matching the provided key, removing it from the message map.
     *
     * @param key The message key to find.
     * @param <T> The expected value of the message.
     * @return The message matching the provided key, or <code>null</code> if none.
     */
    @Nullable
    public <T> T pollMessage(String key) {
        //noinspection unchecked
        return messages == null ? null : (T) messages.remove(key);
    }

    /**
     * Creates a cursor at the same position, but with its own messages that can't influence
     * the messages of the cursor that was forked.
     *
     * @return A new cursor with the same position but an initially clear set of messages.
     */
    public Cursor fork() {
        return new Cursor(parent == null ? null : parent.fork(), value);
    }
}
