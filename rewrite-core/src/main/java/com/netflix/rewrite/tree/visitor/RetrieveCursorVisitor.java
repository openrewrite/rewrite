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
import lombok.AllArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
public class RetrieveCursorVisitor extends CursorAstVisitor<Cursor> {
    UUID treeId;

    @Override
    public Cursor defaultTo(Tree t) {
        return null;
    }

    @Override
    public Cursor visitTree(Tree tree) {
        return tree.getId().equals(treeId) ? getCursor() : super.visitTree(tree);
    }
}
