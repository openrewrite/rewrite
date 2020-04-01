package org.openrewrite;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.openrewrite.internal.lang.Nullable;

import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;

@EqualsAndHashCode
@AllArgsConstructor
public class Cursor {
    @Nullable
    private final Cursor parent;

    private final Tree tree;

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
}
