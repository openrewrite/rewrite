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
package org.openrewrite.java.visitor;

import org.openrewrite.java.tree.Cursor;
import org.openrewrite.java.tree.Tree;

public abstract class CursorAstVisitor<R> extends AstVisitor<R> {
    private final ThreadLocal<Cursor> cursor = new ThreadLocal<>();

    public R visit(Tree tree) {
        if(tree == null) {
            return defaultTo(null);
        }

        cursor.set(new Cursor(cursor.get(), tree));
        R t = reduce(tree.accept(this), visitTree(tree));
        cursor.set(cursor.get().getParent());
        return t;
    }

    public Cursor getCursor() {
        return cursor.get();
    }
}
