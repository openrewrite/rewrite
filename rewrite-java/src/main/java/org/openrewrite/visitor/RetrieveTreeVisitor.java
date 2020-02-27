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
package org.openrewrite.visitor;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.tree.Tree;
import org.openrewrite.internal.lang.Nullable;

import java.util.UUID;

public class RetrieveTreeVisitor extends AstVisitor<Tree> {
    @Nullable
    private final UUID treeId;

    public RetrieveTreeVisitor(@Nullable UUID treeId) {
        this.treeId = treeId;
    }

    @Override
    public Tree defaultTo(Tree t) {
        return null;
    }

    @Override
    public Tree visitTree(Tree tree) {
        return tree.getId().equals(treeId) ? tree : super.visitTree(tree);
    }
}
