/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.internal;

import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.Markup;

import java.util.Iterator;

import static java.util.Objects.requireNonNull;

public class FindRecipeRunException extends TreeVisitor<Tree, Integer> {

    private final RecipeRunException vt;
    private final Tree nearestTree;

    public FindRecipeRunException(RecipeRunException rre) {
        this.vt = rre;
        if (rre.getCursor() == null) {
            this.nearestTree = null;
        } else {
            Iterator<Object> path = requireNonNull(rre.getCursor()).getPath(Tree.class::isInstance);
            this.nearestTree = path.hasNext() ? (Tree) path.next() : null;
        }
    }

    @Override
    public Tree preVisit(Tree tree, Integer integer) {
        if (nearestTree == null) {
            return null;
        } else if (tree == nearestTree) {
            stopAfterPreVisit();
            return Markup.error(tree, vt);
        }
        return tree;
    }
}
