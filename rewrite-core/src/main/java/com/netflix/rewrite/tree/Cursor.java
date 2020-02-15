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
package com.netflix.rewrite.tree;

import com.netflix.rewrite.internal.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

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

    public Tr.CompilationUnit enclosingCompilationUnit() {
        Tr.CompilationUnit cu = firstEnclosing(Tr.CompilationUnit.class);
        if (cu == null) {
            throw new IllegalStateException("Expected to find a Tr.CompilationUnit in " + this);
        }
        return cu;
    }

    @Nullable
    public Tr.Block<?> enclosingBlock() {
        return firstEnclosing(Tr.Block.class);
    }

    @Nullable
    public Tr.VariableDecls enclosingVariableDecl() {
        return firstEnclosing(Tr.VariableDecls.class);
    }

    @Nullable
    public Tr.MethodDecl enclosingMethod() {
        return firstEnclosing(Tr.MethodDecl.class);
    }

    public Tr.ClassDecl enclosingClass() {
        return firstEnclosing(Tr.ClassDecl.class);
    }

    @Nullable
    private <T extends Tree> T firstEnclosing(Class<T> tClass) {
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

    /**
     * @param scope The cursor of the lower scoped tree element to check.
     * @return Whether this cursor shares the same name scope as {@code scope}.
     */
    public boolean isInSameNameScope(Cursor scope) {
        return getPathAsStream()
                .filter(t -> t instanceof Tr.Block ||
                        t instanceof Tr.MethodDecl ||
                        t instanceof Tr.Try ||
                        t instanceof Tr.ForLoop ||
                        t instanceof Tr.ForEachLoop).findAny()
                .map(higherNameScope -> scope.getPathAsStream()
                        .takeWhile(t -> !(t instanceof Tr.ClassDecl) ||
                                (((Tr.ClassDecl) t).getKind() instanceof Tr.ClassDecl.Kind.Class &&
                                        !((Tr.ClassDecl) t).hasModifier("static"))
                        )
                        .anyMatch(higherNameScope::equals))
                .orElse(false);
    }
}
