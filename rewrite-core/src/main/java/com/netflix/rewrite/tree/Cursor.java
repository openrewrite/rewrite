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
import lombok.Data;
import lombok.experimental.NonFinal;

import java.util.Iterator;

@Data
public class Cursor {
    @Nullable
    Cursor parent;

    Tree tree;

    public Iterator<Tree> getPath() {
        return new CursorIterator(this);
    }

    private static class CursorIterator implements Iterator<Tree> {
        @NonFinal
        Cursor cursor;

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
    public Tr.VariableDecls enclosingVariableDecl() {
        return firstEnclosing(Tr.VariableDecls.class);
    }

    @Nullable
    public Tr.MethodDecl enclosingMethod() {
        return firstEnclosing(Tr.MethodDecl.class);
    }

    @Nullable
    public Tr.ClassDecl enclosingClass() {
        return firstEnclosing(Tr.ClassDecl.class);
    }

    @Nullable
    private <T extends Tree> T firstEnclosing(Class<T> tClass) {
        CursorIterator iter = new CursorIterator(this);
        while(iter.hasNext()) {
            Tree t = iter.next();
            if(tClass.isInstance(t)) {
                //noinspection unchecked
                return (T) t;
            }
        }
        return null;
    }
}
