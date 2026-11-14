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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.Markup;

import java.util.*;

import static java.util.Objects.requireNonNull;

public class FindRecipeRunException extends TreeVisitor<Tree, Integer> {

    private final RecipeRunException vt;
    private final @Nullable Tree nearestTree;

    public FindRecipeRunException(RecipeRunException rre) {
        this(rre, rre.getCursor() == null ? null : innermostTree(rre));
    }

    private FindRecipeRunException(RecipeRunException rre, @Nullable Tree nearestTree) {
        this.vt = rre;
        this.nearestTree = nearestTree;
    }

    /**
     * Marks the nearest cursor tree still in {@code tree} (the failing one may be an edited copy), else {@code tree}.
     */
    public static <T extends Tree> T markNearest(T tree, RecipeRunException rre) {
        if (rre.getCursor() == null) {
            return Markup.error(tree, rre);
        }

        List<Tree> path = new ArrayList<>();
        requireNonNull(rre.getCursor()).getPath(Tree.class::isInstance)
                .forEachRemaining(t -> path.add((Tree) t));
        Set<Tree> candidates = Collections.newSetFromMap(new IdentityHashMap<>());
        candidates.addAll(path);
        Set<Tree> present = Collections.newSetFromMap(new IdentityHashMap<>());
        new TreeVisitor<Tree, Integer>() {
            @Override
            public Tree preVisit(Tree t, Integer p) {
                if (candidates.contains(t)) {
                    present.add(t);
                }
                return t;
            }
        }.visit(tree, 0);

        for (Tree candidate : path) {
            if (present.contains(candidate)) {
                //noinspection unchecked
                return (T) new FindRecipeRunException(rre, candidate).visitNonNull(tree, 0);
            }
        }
        return Markup.error(tree, rre);
    }

    private static @Nullable Tree innermostTree(RecipeRunException rre) {
        Iterator<Object> path = requireNonNull(rre.getCursor()).getPath(Tree.class::isInstance);
        return path.hasNext() ? (Tree) path.next() : null;
    }

    @Override
    public @Nullable Tree preVisit(Tree tree, Integer integer) {
        if (nearestTree == null) {
            return null;
        } else if (tree == nearestTree) {
            stopAfterPreVisit();
            return Markup.error(tree, vt);
        }
        return tree;
    }
}
