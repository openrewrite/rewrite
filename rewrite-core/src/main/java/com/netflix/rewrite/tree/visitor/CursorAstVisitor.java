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
package com.netflix.rewrite.tree.visitor;

import com.netflix.rewrite.tree.Cursor;
import com.netflix.rewrite.tree.Tree;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.NonFinal;

public abstract class CursorAstVisitor<R> extends AstVisitor<R> {
    @NonFinal
    @Getter
    Cursor cursor;

    public R visit(Tree tree) {
        if(tree == null) {
            return defaultTo(null);
        }

        cursor = new Cursor(cursor, tree);
        R t = reduce(tree.accept(this), visitTree(tree));
        cursor = cursor.getParent();
        return t;
    }
}
